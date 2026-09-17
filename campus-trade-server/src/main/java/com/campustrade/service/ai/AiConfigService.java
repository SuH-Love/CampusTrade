package com.campustrade.service.ai;

import com.campustrade.entity.*;
import com.campustrade.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Slf4j
@Service
public class AiConfigService {

    @Autowired private AiPromptTemplateMapper promptMapper;
    @Autowired private AiToolDefMapper toolMapper;
    @Autowired private AiSafetyRuleMapper ruleMapper;
    @Autowired private AiConfigMapper configMapper;
    @Autowired private AiConfigVersionMapper versionMapper;
    @Autowired private StringRedisTemplate redis;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private ObjectMapper objectMapper;

    private static final String CHANNEL = "ai:config:changed";

    private final Cache<String, String> promptCache = Caffeine.newBuilder()
            .maximumSize(200).expireAfterWrite(Duration.ofHours(2)).build();
    private final Cache<String, AiToolDef> toolCache = Caffeine.newBuilder()
            .maximumSize(100).expireAfterWrite(Duration.ofHours(2)).build();
    private final Cache<String, List<AiSafetyRule>> ruleCache = Caffeine.newBuilder()
            .maximumSize(50).expireAfterWrite(Duration.ofHours(2)).build();
    private final Cache<String, String> configCache = Caffeine.newBuilder()
            .maximumSize(200).expireAfterWrite(Duration.ofHours(2)).build();

    @PostConstruct
    public void init() {
        loadAllToCache();

    }

    @Scheduled(fixedRate = 600000)
    public void periodicSync() {
        loadAllToCache();
    }

    public void refreshCache() {
        loadAllToCache();
    }

    public String getAssembledSystemPrompt() {
        try {
            List<AiPromptTemplate> templates = promptMapper.selectByCategory("system");
            if (templates == null || templates.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            for (AiPromptTemplate t : templates) {
                if (t.getIsActive() != null && t.getIsActive() == 1 && t.getContent() != null) {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(t.getContent());
                }
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to assemble system prompt from DB: {}", e.getMessage());
            return null;
        }
    }

    private void loadAllToCache() {
        try {
            List<AiPromptTemplate> prompts = promptMapper.selectAllActive();
            if (prompts != null) prompts.forEach(t -> { if (t.getContent() != null) promptCache.put(t.getTemplateKey(), t.getContent()); });

            List<AiToolDef> tools = toolMapper.selectAllActive();
            if (tools != null) tools.forEach(t -> toolCache.put(t.getToolName(), t));

            List<AiSafetyRule> allRules = ruleMapper.selectAllActive();
            if (allRules != null) {
                Map<String, List<AiSafetyRule>> grouped = new HashMap<>();
                for (AiSafetyRule r : allRules) {
                    grouped.computeIfAbsent(r.getRuleType(), k -> new ArrayList<>()).add(r);
                }
                grouped.forEach(ruleCache::put);
            }

            List<AiConfig> configs = configMapper.selectAllActive();
            if (configs != null) configs.forEach(c -> configCache.put(c.getConfigGroup() + ":" + c.getConfigKey(), c.getConfigValue()));

            log.info("配置全量加载完成: prompt={}, tool={}, safety={}, config={}",
                prompts != null ? prompts.size() : 0,
                tools != null ? tools.size() : 0,
                allRules != null ? allRules.size() : 0,
                configs != null ? configs.size() : 0);
        } catch (Exception e) {
            log.warn("配置全量加载失败，使用代码默认值: {}", e.getMessage());
        }
    }

    // ========== Prompt模板 ==========
    public String getPromptTemplate(String key) {
        return promptCache.get(key, k -> {
            try {
                String redisVal = redis.opsForValue().get("ai:cfg:prompt:" + k);
                if (redisVal != null) return redisVal;
                AiPromptTemplate tpl = promptMapper.selectByKey(k);
                if (tpl != null && tpl.getIsActive() == 1) {
                    redis.opsForValue().set("ai:cfg:prompt:" + k, tpl.getContent());
                    return tpl.getContent();
                }
            } catch (Exception e) {
                log.warn("读取Prompt模板失败: key={}, {}", k, e.getMessage());
            }
            return PromptDefaults.getDefault(k);
        });
    }

    public void updatePromptTemplate(String key, String content, Long userId) {
        AiPromptTemplate old = promptMapper.selectByKeyForUpdate(key);
        if (old != null) {
            AiConfigVersion version = new AiConfigVersion();
            version.setConfigType("prompt");
            version.setConfigId(old.getId());
            version.setConfigKey(key);
            version.setConfigVersion(old.getConfigVersion() != null ? old.getConfigVersion() : 0);
            version.setSnapshot(old.getContent());
            version.setCreatedBy(userId);
            versionMapper.insert(version);
            int currentVer = old.getConfigVersion() != null ? old.getConfigVersion() : 0;
            int rows = promptMapper.updateContent(key, content, currentVer + 1, userId, currentVer);
            if (rows == 0) throw new RuntimeException("配置已被其他人修改，请刷新重试");
        }
        promptCache.invalidate(key);
        try { redis.opsForValue().set("ai:cfg:prompt:" + key, content); } catch (Exception ignored) {}
        notifyChange("prompt", key);
    }

    public List<AiPromptTemplate> getAllPrompts() {
        return promptMapper.selectAllActive();
    }

    public List<AiPromptTemplate> getPromptsByCategory(String category) {
        return promptMapper.selectByCategory(category);
    }

    // ========== 安全规则 ==========
    public List<AiSafetyRule> getSafetyRules(String ruleType) {
        return ruleCache.get(ruleType, k -> {
            try {
                List<AiSafetyRule> rules = ruleMapper.selectByType(k);
                return rules != null ? rules : Collections.emptyList();
            } catch (Exception e) {
                log.warn("读取安全规则失败: type={}, {}", k, e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    public List<AiSafetyRule> getAllSafetyRules() {
        return ruleMapper.selectAllActive();
    }

    public void insertSafetyRule(AiSafetyRule rule) {
        ruleMapper.insert(rule);
        ruleCache.invalidateAll();
        notifyChange("safety", null);
    }

    public void updateSafetyRule(AiSafetyRule rule) {
        ruleMapper.update(rule);
        ruleCache.invalidateAll();
        notifyChange("safety", null);
    }

    public void deleteSafetyRule(Long id) {
        ruleMapper.deleteById(id);
        ruleCache.invalidateAll();
        notifyChange("safety", null);
    }

    public void toggleSafetyRule(Long id, Integer isActive) {
        ruleMapper.toggleActive(id, isActive);
        ruleCache.invalidateAll();
        notifyChange("safety", null);
    }

    // ========== 通用参数 ==========
    public String getConfig(String group, String key) {
        return configCache.get(group + ":" + key, k -> {
            try {
                AiConfig config = configMapper.selectByGroupKey(group, key);
                if (config != null && config.getIsActive() == 1) return config.getConfigValue();
            } catch (Exception e) {
                log.warn("读取配置失败: group={}, key={}, {}", group, key, e.getMessage());
            }
            return null;
        });
    }

    public int getInt(String group, String key, int defaultValue) {
        String val = getConfig(group, key);
        if (val != null) {
            try { return Integer.parseInt(val); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public double getDouble(String group, String key, double defaultValue) {
        String val = getConfig(group, key);
        if (val != null) {
            try { return Double.parseDouble(val); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public boolean getBoolean(String group, String key, boolean defaultValue) {
        String val = getConfig(group, key);
        if (val != null) {
            try { return Boolean.parseBoolean(val); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public void updateConfig(String group, String key, String value, Long userId) {
        configMapper.update(group, key, value, userId);
        configCache.invalidate(group + ":" + key);
        notifyChange("config", group + ":" + key);
    }

    public List<AiConfig> getAllConfigs() {
        return configMapper.selectAllActive();
    }

    public List<AiConfig> getConfigsByGroup(String group) {
        return configMapper.selectByGroup(group);
    }

    // ========== 快捷问题 ==========
    @Autowired private AiQuickQuestionMapper questionMapper;

    public List<AiQuickQuestion> getActiveQuickQuestions() {
        return questionMapper.selectAllActive();
    }

    public void insertQuickQuestion(AiQuickQuestion q) {
        questionMapper.insert(q);
    }

    public void updateQuickQuestion(AiQuickQuestion q) {
        questionMapper.update(q);
    }

    public void deleteQuickQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    public void toggleQuickQuestion(Long id, Integer isActive) {
        questionMapper.toggleActive(id, isActive);
    }

    // ========== 工具定义 ==========
    public List<AiToolDef> getActiveTools() {
        return toolMapper.selectAllActive();
    }

    public AiToolDef getToolByName(String name) {
        return toolCache.get(name, k -> {
            try {
                AiToolDef d = toolMapper.selectByName(k);
                return d != null ? d : new AiToolDef();
            } catch (Exception e) {
                log.warn("读取工具定义失败: name={}, {}", k, e.getMessage());
                return new AiToolDef();
            }
        });
    }

    public void toggleTool(String name, Integer isActive) {
        toolMapper.toggleActive(name, isActive);
        toolCache.invalidate(name);
        notifyChange("tool", name);
    }

    // ========== 版本历史 ==========
    public List<AiConfigVersion> getVersionsByTypeKey(String type, String key) {
        return versionMapper.selectByTypeKey(type, key);
    }

    // ========== 通知 ==========
    private void notifyChange(String type, String key) {
        try {
            Map<String, String> msg = new HashMap<>();
            msg.put("type", type);
            msg.put("key", key != null ? key : "");
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(msg));
        } catch (Exception e) {
            log.warn("发送配置变更通知失败: {}", e.getMessage());
        }
    }
}
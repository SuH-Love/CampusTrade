package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.*;
import com.campustrade.mapper.*;
import com.campustrade.service.ai.AiConfigService;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Api(tags = "AI配置管理接口")
@RestController
@RequestMapping("/api/ai/config")
public class AiConfigController {

    @Autowired
    private AiConfigService configService;
    @Autowired
    private AiPromptTemplateMapper promptMapper;
    @Autowired
    private AiSafetyRuleMapper safetyRuleMapper;
    @Autowired
    private AiConfigMapper configMapper;
    @Autowired
    private AiQuickQuestionMapper quickQuestionMapper;
    @Autowired
    private AiConfigVersionMapper versionMapper;
    @Autowired
    private com.campustrade.mapper.AiToolDefMapper toolDefMapper;
    @Autowired
    private com.campustrade.service.ai.DeepSeekClient deepSeekClient;

    private Long currentAdminId() {
        Long uid = SecurityUtil.getCurrentUserId();
        return uid != null ? uid : 0L;
    }

    private void recordVersion(String type, Long configId, String key, Integer version, String snapshot, String note) {
        try {
            AiConfigVersion v = new AiConfigVersion();
            v.setConfigType(type);
            v.setConfigId(configId);
            v.setConfigKey(key);
            v.setConfigVersion(version);
            v.setSnapshot(snapshot);
            v.setChangeNote(note);
            v.setCreatedBy(currentAdminId());
            versionMapper.insert(v);
        } catch (Exception e) {
            log.warn("Failed to record version: {}", e.getMessage());
        }
    }

    // ==================== 提示词模板 ====================

    @ApiOperation("提示词模板列表")
    @GetMapping("/prompts")
    public Result<List<AiPromptTemplate>> listPrompts(@RequestParam(required = false) String category) {
        List<AiPromptTemplate> list = (category != null && !category.isEmpty())
                ? promptMapper.selectByCategory(category)
                : promptMapper.selectAllActive();
        return Result.success(list);
    }

    @ApiOperation("提示词模板详情")
    @GetMapping("/prompts/{key}")
    public Result<AiPromptTemplate> getPrompt(@PathVariable String key) {
        AiPromptTemplate prompt = promptMapper.selectByKey(key);
        if (prompt == null) return Result.error(404, "模板不存在");
        return Result.success(prompt);
    }

    @ApiOperation("更新提示词模板")
    @PutMapping("/prompts/{key}")
    public Result<Void> updatePrompt(@PathVariable String key, @RequestBody Map<String, String> body) {
        AiPromptTemplate existing = promptMapper.selectByKeyForUpdate(key);
        if (existing == null) return Result.error(404, "模板不存在");
        String content = body.get("content");
        if (content == null || content.isEmpty()) return Result.error(400, "内容不能为空");
        int newVersion = (existing.getConfigVersion() != null ? existing.getConfigVersion() : 1) + 1;
        int rows = promptMapper.updateContent(key, content, newVersion, currentAdminId(), existing.getConfigVersion());
        if (rows == 0) return Result.error(409, "版本冲突，请刷新后重试");
        recordVersion("prompt", existing.getId(), key, newVersion, content, body.get("note"));
        configService.refreshCache();
        return Result.success(null);
    }

    @ApiOperation("提示词版本历史")
    @GetMapping("/prompts/{key}/versions")
    public Result<List<AiConfigVersion>> promptVersions(@PathVariable String key) {
        return Result.success(versionMapper.selectByTypeKey("prompt", key));
    }

    @ApiOperation("回滚提示词版本")
    @PostMapping("/prompts/{key}/rollback/{ver}")
    public Result<Void> rollbackPrompt(@PathVariable String key, @PathVariable Integer ver) {
        AiConfigVersion v = versionMapper.selectByVersion("prompt", key, ver);
        if (v == null) return Result.error(404, "版本不存在");
        AiPromptTemplate existing = promptMapper.selectByKeyForUpdate(key);
        if (existing == null) return Result.error(404, "模板不存在");
        int newVersion = (existing.getConfigVersion() != null ? existing.getConfigVersion() : 1) + 1;
        promptMapper.updateContent(key, v.getSnapshot(), newVersion, currentAdminId(), existing.getConfigVersion());
        recordVersion("prompt", existing.getId(), key, newVersion, v.getSnapshot(), "回滚到版本" + ver);
        configService.refreshCache();
        return Result.success(null);
    }

    // ==================== 安全规则 ====================

    @ApiOperation("安全规则列表")
    @GetMapping("/safety-rules")
    public Result<List<AiSafetyRule>> listSafetyRules(@RequestParam(required = false) String ruleType) {
        List<AiSafetyRule> list = (ruleType != null && !ruleType.isEmpty())
                ? safetyRuleMapper.selectByType(ruleType)
                : safetyRuleMapper.selectAllActive();
        return Result.success(list);
    }

    @ApiOperation("新增安全规则")
    @PostMapping("/safety-rules")
    public Result<Void> addSafetyRule(@RequestBody AiSafetyRule rule) {
        if (rule.getIsActive() == null) rule.setIsActive(1);
        if (rule.getSortOrder() == null) rule.setSortOrder(0);
        rule.setUpdatedBy(currentAdminId());
        safetyRuleMapper.insert(rule);
        configService.refreshCache();
        return Result.success(null);
    }

    @ApiOperation("更新安全规则")
    @PutMapping("/safety-rules/{id}")
    public Result<Void> updateSafetyRule(@PathVariable Long id, @RequestBody AiSafetyRule rule) {
        rule.setId(id);
        rule.setUpdatedBy(currentAdminId());
        safetyRuleMapper.update(rule);
        configService.refreshCache();
        return Result.success(null);
    }

    @ApiOperation("删除安全规则")
    @DeleteMapping("/safety-rules/{id}")
    public Result<Void> deleteSafetyRule(@PathVariable Long id) {
        safetyRuleMapper.deleteById(id);
        configService.refreshCache();
        return Result.success(null);
    }

    @ApiOperation("启用/禁用安全规则")
    @PatchMapping("/safety-rules/{id}/toggle")
    public Result<Void> toggleSafetyRule(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        safetyRuleMapper.toggleActive(id, body.get("isActive"));
        configService.refreshCache();
        return Result.success(null);
    }

    // ==================== 参数配置 ====================

    @ApiOperation("全部参数列表")
    @GetMapping("/params")
    public Result<Map<String, List<AiConfig>>> listParams() {
        List<AiConfig> all = configMapper.selectAllActive();
        Map<String, List<AiConfig>> grouped = new LinkedHashMap<>();
        for (AiConfig c : all) {
            grouped.computeIfAbsent(c.getConfigGroup(), k -> new ArrayList<>()).add(c);
        }
        return Result.success(grouped);
    }

    @ApiOperation("更新参数")
    @PutMapping("/params/{group}/{key}")
    public Result<Void> updateParam(@PathVariable String group, @PathVariable String key, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null) return Result.error(400, "值不能为空");
        configMapper.update(group, key, value, currentAdminId());
        configService.refreshCache();
        return Result.success(null);
    }

    // ==================== 快捷问题 ====================

    @ApiOperation("快捷问题列表(管理)")
    @GetMapping("/quick-questions")
    public Result<List<AiQuickQuestion>> listQuickQuestions() {
        return Result.success(quickQuestionMapper.selectAllActive());
    }

    @ApiOperation("新增快捷问题")
    @PostMapping("/quick-questions")
    public Result<Void> addQuickQuestion(@RequestBody AiQuickQuestion q) {
        if (q.getIsActive() == null) q.setIsActive(1);
        if (q.getSortOrder() == null) q.setSortOrder(0);
        quickQuestionMapper.insert(q);
        return Result.success(null);
    }

    @ApiOperation("更新快捷问题")
    @PutMapping("/quick-questions/{id}")
    public Result<Void> updateQuickQuestion(@PathVariable Long id, @RequestBody AiQuickQuestion q) {
        q.setId(id);
        quickQuestionMapper.update(q);
        return Result.success(null);
    }

    @ApiOperation("删除快捷问题")
    @DeleteMapping("/quick-questions/{id}")
    public Result<Void> deleteQuickQuestion(@PathVariable Long id) {
        quickQuestionMapper.deleteById(id);
        return Result.success(null);
    }

    @ApiOperation("启用/禁用快捷问题")
    @PatchMapping("/quick-questions/{id}/toggle")
    public Result<Void> toggleQuickQuestion(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        quickQuestionMapper.toggleActive(id, body.get("isActive"));
        return Result.success(null);
    }

    // ==================== 版本管理 ====================

    @ApiOperation("全局版本历史")
    @GetMapping("/versions")
    public Result<List<AiConfigVersion>> listVersions(@RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String key) {
        if (type != null && !type.isEmpty() && key != null && !key.isEmpty()) {
            return Result.success(versionMapper.selectByTypeKey(type, key));
        }
        if (type != null && !type.isEmpty()) {
            return Result.success(versionMapper.selectByType(type));
        }
        return Result.success(versionMapper.selectAll());
    }

    @ApiOperation("指定配置版本历史")
    @GetMapping("/versions/{type}/{key}")
    public Result<List<AiConfigVersion>> versions(@PathVariable String type, @PathVariable String key) {
        return Result.success(versionMapper.selectByTypeKey(type, key));
    }

    // ==================== 工具定义管理 ====================

    @ApiOperation("工具定义列表")
    @GetMapping("/tools")
    public Result<List<com.campustrade.entity.AiToolDef>> listTools(@RequestParam(required = false) String group) {
        List<com.campustrade.entity.AiToolDef> tools = toolDefMapper.selectAllActive();
        if (group != null && !group.isEmpty()) {
            tools = tools.stream().filter(t -> group.equals(t.getToolGroup())).collect(java.util.stream.Collectors.toList());
        }
        return Result.success(tools);
    }

    @ApiOperation("更新工具定义")
    @PutMapping("/tools/{name}")
    public Result<Void> updateTool(@PathVariable String name, @RequestBody Map<String, Object> body) {
        com.campustrade.entity.AiToolDef tool = toolDefMapper.selectByName(name);
        if (tool == null) return Result.error(404, "工具不存在");
        if (body.containsKey("description")) tool.setDescription((String) body.get("description"));
        if (body.containsKey("displayName")) tool.setDisplayName((String) body.get("displayName"));
        toolDefMapper.update(tool);
        configService.refreshCache();
        return Result.success(null);
    }

    @ApiOperation("启用/禁用工具")
    @PatchMapping("/tools/{name}/toggle")
    public Result<Void> toggleTool(@PathVariable String name, @RequestBody Map<String, Integer> body) {
        toolDefMapper.toggleActive(name, body.get("isActive"));
        configService.refreshCache();
        return Result.success(null);
    }

    // ==================== Prompt预览/测试 ====================

    @ApiOperation("预览系统提示词组装结果")
    @PostMapping("/prompts/preview")
    public Result<Map<String, Object>> previewPrompt() {
        Map<String, Object> result = new LinkedHashMap<>();
        String assembled = configService.getAssembledSystemPrompt();
        result.put("assembled", assembled);
        result.put("length", assembled != null ? assembled.length() : 0);
        result.put("estimatedTokens", assembled != null ? assembled.length() / 3 : 0);
        List<AiPromptTemplate> templates = promptMapper.selectByCategory("system");
        result.put("templateCount", templates != null ? templates.size() : 0);
        return Result.success(result);
    }

    @ApiOperation("在线测试提示词")
    @PostMapping("/prompts/test")
    public Result<Map<String, Object>> testPrompt(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        String testMessage = body.get("message");
        if (testMessage == null || testMessage.isEmpty()) {
            return Result.error(400, "测试消息不能为空");
        }
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", configService.getAssembledSystemPrompt());
            messages.add(sysMsg);
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", testMessage);
            messages.add(userMsg);
            String answer = deepSeekClient.chat(messages);
            result.put("answer", answer);
            result.put("success", true);
        } catch (Exception e) {
            result.put("answer", "测试失败: " + e.getMessage());
            result.put("success", false);
        }
        return Result.success(result);
    }

    // ==================== 版本对比 ====================

    @ApiOperation("对比两个版本")
    @GetMapping("/versions/compare")
    public Result<Map<String, Object>> compareVersions(@RequestParam String type,
                                                        @RequestParam String key,
                                                        @RequestParam Integer v1,
                                                        @RequestParam Integer v2) {
        Map<String, Object> result = new LinkedHashMap<>();
        AiConfigVersion ver1 = versionMapper.selectByVersion(type, key, v1);
        AiConfigVersion ver2 = versionMapper.selectByVersion(type, key, v2);
        result.put("v1", ver1);
        result.put("v2", ver2);
        if (ver1 != null && ver2 != null && ver1.getSnapshot() != null && ver2.getSnapshot() != null) {
            result.put("v1Length", ver1.getSnapshot().length());
            result.put("v2Length", ver2.getSnapshot().length());
            result.put("identical", ver1.getSnapshot().equals(ver2.getSnapshot()));
        }
        return Result.success(result);
    }
}
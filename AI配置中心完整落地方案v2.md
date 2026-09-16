# AI配置中心完整落地方案 v2

> 目标：知识库/提示词/工具定义/安全规则/Agent参数全部数据库化，统一管理后台，热更新，版本控制
> 设计原则：可维护性（可视化编辑+版本回滚） + 高可用性（三级降级+多节点一致） + 可拓展性（接口抽象+插件化）

---

## 一、现状问题再分析

### 1.1 当前配置散落位置全景

```
代码硬编码（改需编译部署）：
├── AiController.java
│   ├── buildDefaultSystemPrompt()        → 3000字System Prompt（7章节）
│   ├── PLATFORM_KNOWLEDGE_BLOCKS[12]     → 平台知识fallback
│   ├── maxIterations = 10                → Agent最大循环
│   ├── maxTokenBudget = 8000             → Token预算
│   ├── systemPart=1500/knowledge=600/faq=400/summary=500 → Token分配
│   └── 工具结果截断2000字符
├── AiToolService.java
│   ├── buildToolCache()                  → 35个工具定义（name/desc/params）
│   ├── WRITE_TOOLS[15]                   → 写操作工具集
│   └── TOOL_CACHE_TTL = 120s
├── AiSafetyService.java
│   ├── INJECTION_PATTERNS[34]            → Prompt Injection正则
│   ├── SENSITIVE_PATTERNS[10]            → 脱敏正则
│   ├── MAX_INPUT_LENGTH = 500
│   └── sanitizeOutput() 6个DSML过滤正则
├── AiReviewService.java
│   ├── MODERATION_SYSTEM_PROMPT          → 商品审核提示词
│   ├── TITLE_OPTIMIZATION_PROMPT         → 标题优化提示词
│   └── BLOCKED_KEYWORDS[24]              → 违禁关键词
├── DeepSeekClient.java
│   ├── FALLBACK_ANSWERS                  → 降级文案
│   ├── CB_FAILURE_THRESHOLD = 5          → 熔断阈值
│   ├── CB_RECOVERY_TIMEOUT = 30000       → 熔断恢复
│   ├── temperature = 0.3
│   ├── INTENT_TEMPLATES[16]             → 意图分类模板
│   └── reasonerKeywords[16]             → 推理场景关键词
├── SessionService.java
│   ├── MAX_CONTEXT_TOKENS = 4000
│   ├── SHORT_TERM_KEEP = 10
│   └── MAX_SUMMARY_LENGTH = 2000
└── AiConsultant.vue（前端）
    └── allSuggestions[40]               → 快捷问题池

application.yml（改需重启）：
├── ai.system-prompt                      → 另一套System Prompt（与代码版不一致！）
├── ai.document.chunk-size/overlap/max    → 分块参数
├── ai.faq.top-k/threshold/rerank         → FAQ检索参数
├── ai.session.ttl/max-history            → 会话参数
└── ai.model.max-concurrent/max-tokens    → 模型参数

Redis（可动态改但无版本控制）：
├── ai:system-prompt:custom               → 自定义Prompt（遮蔽代码/yml默认值）
├── ai:config:apikey/model/baseUrl        → 模型配置
└── ai:config:emb:*                       → embedding配置
```

### 1.2 核心问题

| 问题 | 影响 | 举例 |
|------|------|------|
| **三处Prompt不一致** | 不确定实际用哪套 | yml一套、代码一套、Redis一套，优先级Redis>yml>代码 |
| **改配置需部署** | 迭代慢 | 改个正则/调个参数要编译→上传→构建→重启 |
| **无版本控制** | 无法回滚 | 改错Prompt无法快速恢复 |
| **无变更审计** | 无法追责 | 谁改的、何时改的、改了什么无记录 |
| **无格式校验** | 容易写错 | 正则语法错误/JSON格式错误运行时才发现 |
| **无预览测试** | 靠猜效果 | 改完Prompt不知道组装后长什么样 |
| **多节点不一致** | 配置漂移 | 多实例时Redis Prompt只改了一个节点的缓存 |

---

## 二、方案总览

### 2.1 架构

```
┌─────────────────────────────────────────────────────────────┐
│                  管理后台 AI配置中心                          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌───────┐ │
│  │提示词模板│ │工具管理  │ │安全规则  │ │参数配置  │ │版本历史│ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └───────┘ │
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Prompt预览 & 在线测试                        ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────┬──────────────────────────────────┘
                           │ REST API (管理员权限)
┌──────────────────────────▼──────────────────────────────────┐
│                    AiConfigService                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────┐  │
│  │Prompt引擎 │  │工具注册表 │  │安全规则引擎│  │参数管理器   │  │
│  └──────────┘  └──────────┘  └──────────┘  └────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐│
│  │          三级降级：Caffeine → Redis → 代码默认值          ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │    多节点同步：Redis Pub/Sub通知 + 定期全量同步兜底       ││
│  └─────────────────────────────────────────────────────────┘│
└──────────┬───────────────────────────────────┬─────────────┘
           │                                    │
    ┌──────▼──────┐                     ┌───────▼───────┐
    │   MySQL     │                     │    Redis      │
    │ (唯一真相源) │                     │ (缓存+通知通道)│
    │ 6张配置表    │                     │ Pub/Sub + L2  │
    └─────────────┘                     └───────────────┘
```

### 2.2 数据流

```
读取配置（高频，每次AI对话）：
  AiConfigService.getPromptTemplate("system.role")
    → Caffeine L1命中？ → 返回（纳秒级）
    → L1未命中 → Redis L2命中？ → 填充L1 → 返回（毫秒级）
    → L2未命中 → MySQL查询 → 填充L1+L2 → 返回
    → MySQL失败 → 代码默认值（兜底，日志告警）

修改配置（低频，管理员操作）：
  API请求 → 格式校验 → MySQL事务更新 + 版本快照
    → 刷新本地Caffeine → Redis SET L2
    → Redis PUBLISH "ai:config:changed" {type, key, version}
    → 其他节点收到通知 → 刷新本地Caffeine
```

---

## 三、数据库设计（完整DDL）

### 3.1 t_ai_prompt_template（提示词模板）

```sql
CREATE TABLE t_ai_prompt_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_key VARCHAR(100) NOT NULL COMMENT '模板标识',
  template_name VARCHAR(200) NOT NULL COMMENT '显示名称',
  category VARCHAR(50) NOT NULL COMMENT '分类: system/review/safety/fallback/navigation',
  content TEXT NOT NULL COMMENT '模板内容，支持{{variable}}占位符',
  variables JSON COMMENT '变量定义[{"name":"tools","desc":"工具列表","source":"dynamic"}]',
  description VARCHAR(500) COMMENT '说明',
  is_active TINYINT NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by BIGINT COMMENT '修改人ID',
  UNIQUE KEY uk_template_key (template_key),
  INDEX idx_category_active (category, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词模板';
```

**初始模板数据**（从代码迁移）：

| template_key | category | 说明 | 变量 |
|-------------|----------|------|------|
| system.role | system | 角色定义"你是校园交易助手小苏" | - |
| system.responsibility | system | 核心职责 | - |
| system.tool_rules | system | 工具调用规则 | `{{tools}}`动态注入 |
| system.vision | system | 图片分析规则 | `{{vision_enabled}}` |
| system.safety | system | 安全约束 | - |
| system.navigation | system | 平台导航回答规则 | - |
| review.moderation | review | 商品审核提示词 | - |
| review.title_optimize | review | 标题优化提示词 | - |
| safety.reminder | safety | 安全提醒文案 | - |
| fallback.answer | fallback | AI不可用降级文案 | - |

### 3.2 t_ai_tool_def（工具定义）

```sql
CREATE TABLE t_ai_tool_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tool_name VARCHAR(100) NOT NULL COMMENT '工具名 如searchGoods',
  display_name VARCHAR(200) COMMENT '显示名',
  tool_group VARCHAR(50) NOT NULL COMMENT '分组: goods/order/user/chat/admin/notification',
  description TEXT NOT NULL COMMENT '工具描述(给模型看)',
  parameters JSON NOT NULL COMMENT '参数JSON Schema',
  handler_class VARCHAR(200) NOT NULL COMMENT '处理器全限定类名',
  handler_method VARCHAR(100) NOT NULL COMMENT '处理器方法名',
  required_role VARCHAR(50) NOT NULL DEFAULT 'USER' COMMENT 'USER/SELLER/ADMIN',
  is_write_operation TINYINT NOT NULL DEFAULT 0 COMMENT '是否写操作',
  need_confirm TINYINT NOT NULL DEFAULT 0 COMMENT '是否需用户确认',
  is_active TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  version INT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by BIGINT,
  UNIQUE KEY uk_tool_name (tool_name),
  INDEX idx_group_active (tool_group, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI工具定义';
```

### 3.3 t_ai_safety_rule（安全规则）

```sql
CREATE TABLE t_ai_safety_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  rule_type VARCHAR(50) NOT NULL COMMENT 'injection/sensitive_mask/dsml_filter/blocked_keyword/output_check',
  rule_pattern VARCHAR(500) NOT NULL COMMENT '正则表达式或关键词',
  rule_action VARCHAR(20) NOT NULL COMMENT 'block/mask/filter/warn',
  replacement VARCHAR(100) COMMENT '替换值(mask类型用)',
  description VARCHAR(500),
  is_active TINYINT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by BIGINT,
  INDEX idx_type_active (rule_type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI安全规则';
```

### 3.4 t_ai_config（通用参数配置）

```sql
CREATE TABLE t_ai_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_group VARCHAR(50) NOT NULL COMMENT 'agent/rag/session/model/review/safety',
  config_key VARCHAR(100) NOT NULL COMMENT '配置键',
  config_value VARCHAR(2000) NOT NULL COMMENT '配置值',
  config_type VARCHAR(20) NOT NULL COMMENT 'int/float/boolean/string/json',
  description VARCHAR(500),
  is_active TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by BIGINT,
  UNIQUE KEY uk_group_key (config_group, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI通用配置';
```

### 3.5 t_ai_quick_question（快捷问题）

```sql
CREATE TABLE t_ai_quick_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question TEXT NOT NULL COMMENT '问题内容',
  category VARCHAR(50) COMMENT 'goods/order/payment/security/chat/other',
  sort_order INT NOT NULL DEFAULT 0,
  is_active TINYINT NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_category_active (category, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI快捷问题';
```

### 3.6 t_ai_config_version（版本快照）

```sql
CREATE TABLE t_ai_config_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_type VARCHAR(50) NOT NULL COMMENT 'prompt/tool/safety/config/question',
  config_id BIGINT NOT NULL COMMENT '对应配置表ID',
  config_key VARCHAR(100) COMMENT '对应配置键(便于查询)',
  version INT NOT NULL,
  snapshot JSON NOT NULL COMMENT '完整快照',
  change_note VARCHAR(500) COMMENT '变更说明',
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_type_key (config_type, config_key),
  INDEX idx_type_id (config_type, config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置版本历史';
```

---

## 四、后端实现设计

### 4.1 AiConfigService（核心配置服务）

```java
@Service
public class AiConfigService {

    @Autowired private AiPromptTemplateMapper promptMapper;
    @Autowired private AiToolDefMapper toolMapper;
    @Autowired private AiSafetyRuleMapper ruleMapper;
    @Autowired private AiConfigMapper configMapper;
    @Autowired private AiConfigVersionMapper versionMapper;
    @Autowired private StringRedisTemplate redis;

    // L1: Caffeine本地缓存
    private final Cache<String, String> promptCache = Caffeine.newBuilder()
        .maximumSize(200).expireAfterWrite(Duration.ofHours(2)).build();
    private final Cache<String, AiToolDef> toolCache = Caffeine.newBuilder()
        .maximumSize(100).expireAfterWrite(Duration.ofHours(2)).build();
    private final Cache<String, List<AiSafetyRule>> ruleCache = Caffeine.newBuilder()
        .maximumSize(50).expireAfterWrite(Duration.ofHours(2)).build();
    private final Cache<String, String> configCache = Caffeine.newBuilder()
        .maximumSize(200).expireAfterWrite(Duration.ofHours(2)).build();

    // ========== 提示词模板 ==========
    public String getPromptTemplate(String key) {
        return promptCache.get(key, k -> {
            // L2: Redis
            String redisVal = redis.opsForValue().get("ai:cfg:prompt:" + k);
            if (redisVal != null) return redisVal;
            // DB
            AiPromptTemplate tpl = promptMapper.selectByKey(k);
            if (tpl != null && tpl.getIsActive() == 1) {
                redis.opsForValue().set("ai:cfg:prompt:" + k, tpl.getContent());
                return tpl.getContent();
            }
            // 兜底: 代码默认值
            return PromptDefaults.getDefault(key);
        });
    }

    public void updatePromptTemplate(String key, String content, Long userId) {
        // 1. 校验模板格式（变量引用合法性）
        validateTemplate(content);
        // 2. 记录旧版本快照
        AiPromptTemplate old = promptMapper.selectByKey(key);
        if (old != null) {
            saveVersionSnapshot("prompt", old.getId(), key, old.getVersion(), old.getContent());
        }
        // 3. DB更新（事务）
        promptMapper.updateContent(key, content, old.getVersion() + 1, userId);
        // 4. 刷新L1+L2
        promptCache.invalidate(key);
        redis.opsForValue().set("ai:cfg:prompt:" + key, content);
        // 5. 通知其他节点
        redis.convertAndSend("ai:config:changed",
            JSON.toJSONString(Map.of("type", "prompt", "key", key)));
    }

    // ========== 工具定义 ==========
    public List<AiToolDef> getActiveTools(String userRole) {
        return toolCache.asMap().values().stream()
            .filter(t -> t.getIsActive() == 1)
            .filter(t -> hasPermission(userRole, t.getRequiredRole()))
            .sorted(Comparator.comparing(AiToolDef::getSortOrder))
            .collect(Collectors.toList());
    }

    public JsonNode executeTool(String toolName, JsonNode args, Long userId) {
        AiToolDef def = toolCache.get(toolName, k -> {
            AiToolDef d = toolMapper.selectByName(k);
            return d != null ? d : AiToolDef.empty();
        });
        if (def.getId() == null) throw new BusinessException("工具不存在: " + toolName);
        if (def.getIsActive() == 0) throw new BusinessException("工具已禁用: " + toolName);
        // 反射调用
        try {
            Object handler = applicationContext.getBean(Class.forName(def.getHandlerClass()));
            Method method = handler.getClass().getMethod(def.getHandlerMethod(),
                JsonNode.class, Long.class);
            return (JsonNode) method.invoke(handler, args, userId);
        } catch (Exception e) {
            throw new BusinessException("工具执行失败: " + toolName, e);
        }
    }

    // ========== 安全规则 ==========
    public List<AiSafetyRule> getSafetyRules(String ruleType) {
        return ruleCache.get(ruleType, k -> {
            List<AiSafetyRule> rules = ruleMapper.selectByType(k);
            return rules != null ? rules : Collections.emptyList();
        });
    }

    // ========== 通用参数 ==========
    public int getInt(String group, String key, int defaultValue) {
        String val = getConfig(group, key);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }

    public String getConfig(String group, String key) {
        String cacheKey = group + ":" + key;
        return configCache.get(cacheKey, k -> {
            AiConfig config = configMapper.selectByGroupKey(group, key);
            if (config != null && config.getIsActive() == 1) return config.getConfigValue();
            return null; // null时Caffeine不缓存
        });
    }

    // ========== 多节点同步 ==========
    @PostConstruct
    public void init() {
        // 订阅Redis配置变更通知
        redis.getConnection().subscribe(message -> {
            String body = new String(message.getBody());
            JSONObject msg = JSON.parseObject(body);
            String type = msg.getString("type");
            String key = msg.getString("key");
            switch (type) {
                case "prompt": promptCache.invalidate(key); break;
                case "tool": toolCache.invalidate(key); break;
                case "safety": ruleCache.invalidateAll(); break;
                case "config": configCache.invalidate(key); break;
            }
            log.info("配置已刷新: type={}, key={}", type, key);
        }, "ai:config:changed".getBytes());

        // 启动时全量加载到缓存
        loadAllToCache();
    }

    // 定期全量同步兜底（每10分钟）
    @Scheduled(fixedRate = 600000)
    public void periodicSync() {
        loadAllToCache();
    }

    private void loadAllToCache() {
        try {
            promptMapper.selectAllActive().forEach(t ->
                promptCache.put(t.getTemplateKey(), t.getContent()));
            toolMapper.selectAllActive().forEach(t ->
                toolCache.put(t.getToolName(), t));
            // ... 其他配置同理
            log.info("配置全量加载完成");
        } catch (Exception e) {
            log.warn("配置全量加载失败，使用代码默认值: {}", e.getMessage());
        }
    }
}
```

### 4.2 Prompt模板引擎

```java
@Service
public class PromptEngine {

    @Autowired private AiConfigService configService;
    @Autowired private AiToolService toolService;

    /**
     * 组装完整System Prompt
     * 由7个模板拼接，变量动态注入
     */
    public String buildSystemPrompt(String userRole) {
        String role = configService.getPromptTemplate("system.role");
        String responsibility = configService.getPromptTemplate("system.responsibility");

        // 动态注入工具列表
        String toolRules = configService.getPromptTemplate("system.tool_rules");
        String toolDescriptions = toolService.buildToolDescriptions(userRole);
        toolRules = toolRules.replace("{{tools}}", toolDescriptions);

        // 动态注入视觉能力
        String vision = configService.getPromptTemplate("system.vision");
        String visionModel = configService.getConfig("model", "vision_model", null);
        vision = vision.replace("{{vision_enabled}}",
            visionModel != null ? "true" : "false");

        String safety = configService.getPromptTemplate("system.safety");
        String navigation = configService.getPromptTemplate("system.navigation");

        return String.join("\n\n", role, responsibility, toolRules, vision, safety, navigation);
    }

    /**
     * 预览组装结果（管理后台用）
     */
    public PromptPreview preview(String userRole) {
        PromptPreview preview = new PromptPreview();
        preview.setRole(configService.getPromptTemplate("system.role"));
        preview.setResponsibility(configService.getPromptTemplate("system.responsibility"));
        preview.setToolRules(configService.getPromptTemplate("system.tool_rules"));
        preview.setTools(toolService.buildToolDescriptions(userRole));
        preview.setVision(configService.getPromptTemplate("system.vision"));
        preview.setSafety(configService.getPromptTemplate("system.safety"));
        preview.setNavigation(configService.getPromptTemplate("system.navigation"));
        preview.setFullPrompt(buildSystemPrompt(userRole));
        preview.setTokenCount(estimateTokens(preview.getFullPrompt()));
        return preview;
    }
}
```

### 4.3 安全规则引擎

```java
@Service
public class SafetyRuleEngine {

    @Autowired private AiConfigService configService;

    public SafetyCheckResult checkInput(String input) {
        // 1. 长度检查
        int maxLen = configService.getInt("safety", "max_input_length", 500);
        if (input.length() > maxLen) {
            return SafetyCheckResult.block("输入过长");
        }

        // 2. Injection检测
        List<AiSafetyRule> injectionRules = configService.getSafetyRules("injection");
        for (AiSafetyRule rule : injectionRules) {
            if (Pattern.matches(rule.getRulePattern(), input)) {
                return SafetyCheckResult.block("检测到Prompt Injection: " + rule.getDescription());
            }
        }

        return SafetyCheckResult.pass();
    }

    public String maskSensitive(String output) {
        List<AiSafetyRule> maskRules = configService.getSafetyRules("sensitive_mask");
        for (AiSafetyRule rule : maskRules) {
            output = output.replaceAll(rule.getRulePattern(),
                rule.getReplacement() != null ? rule.getReplacement() : "***");
        }
        return output;
    }

    public String filterDsml(String output) {
        List<AiSafetyRule> filterRules = configService.getSafetyRules("dsml_filter");
        for (AiSafetyRule rule : filterRules) {
            output = output.replaceAll(rule.getRulePattern(), "");
        }
        return output;
    }
}
```

### 4.4 现有Service改造点

| 现有代码 | 改造方式 | 改造后 |
|---------|---------|--------|
| `AiController.buildDefaultSystemPrompt()` | 删除方法，调用PromptEngine | `promptEngine.buildSystemPrompt(role)` |
| `AiController` 硬编码参数 | 注入AiConfigService | `configService.getInt("agent","max_iterations",10)` |
| `AiToolService.buildToolCache()` | 从DB加载工具定义 | `configService.getActiveTools(role)` |
| `AiToolService.executeTool()` | 反射调用DB配置的handler | `configService.executeTool(name, args, userId)` |
| `AiSafetyService` 硬编码正则 | 调用SafetyRuleEngine | `safetyEngine.checkInput(input)` |
| `AiReviewService` 硬编码提示词 | 从DB读模板 | `configService.getPromptTemplate("review.moderation")` |
| `AiReviewService.BLOCKED_KEYWORDS` | 从DB读安全规则 | `configService.getSafetyRules("blocked_keyword")` |
| `DeepSeekClient` 硬编码参数 | 从DB读配置 | `configService.getInt("model","cb_failure_threshold",5)` |
| `SessionService` 硬编码常量 | 从DB读配置 | `configService.getInt("session","max_context_tokens",4000)` |
| 前端 `allSuggestions[40]` | API获取 | `GET /api/ai/quick-questions` |

---

## 五、高可用设计

### 5.1 三级降级策略

```
读取配置时：
  L1 Caffeine内存缓存 ──命中──→ 返回（纳秒级）
       │未命中
  L2 Redis缓存 ──命中──→ 填充L1 + 返回（毫秒级）
       │未命中
  L3 MySQL ──成功──→ 填充L1+L2 + 返回
       │失败（DB宕机/网络中断）
  L4 代码默认值 ──→ 返回 + 日志告警（保证不阻断业务）
```

**代码默认值保留**（`PromptDefaults`类）：
```java
public class PromptDefaults {
    private static final Map<String, String> DEFAULTS = Map.of(
        "system.role", "你是校园交易助手小苏...",
        "system.safety", "安全约束：不输出敏感信息...",
        "review.moderation", "你是商品审核员...",
        "fallback.answer", "AI服务暂时不可用，请稍后重试",
        // ... 所有模板的默认值
    );

    public static String getDefault(String key) {
        return DEFAULTS.getOrDefault(key, "");
    }
}
```

**降级场景**：

| 故障 | 影响 | 降级行为 |
|------|------|---------|
| MySQL宕机 | 无法读写配置 | L1缓存继续服务；L1未命中用L4代码默认值；管理后台不可用 |
| Redis宕机 | L2缓存+Pub/Sub失效 | L1缓存继续服务；L1未命中查MySQL；多节点靠定期同步 |
| Caffeine过期 | L1失效 | 查L2 Redis → L3 MySQL，性能略降但不阻断 |
| 全部宕机 | 仅代码默认值可用 | AI用硬编码默认Prompt/工具/规则运行，管理后台不可用 |

### 5.2 多节点配置一致性

```
节点A修改配置：
  1. MySQL更新（ACID保证）
  2. 本地Caffeine刷新
  3. Redis SET L2
  4. Redis PUBLISH "ai:config:changed" → 节点B/C/D收到

节点B收到通知：
  → Caffeine.invalidate(key) → 下次读取从L2/L3加载最新

兜底（防通知丢失）：
  → 每10分钟@Scheduled全量同步一次
  → 配置version字段，节点对比版本号判断是否需刷新
```

### 5.3 配置变更原子性

```java
@Transactional
public void updatePromptTemplate(String key, String content, Long userId) {
    // 1. 读取旧值（FOR UPDATE加锁防并发修改）
    AiPromptTemplate old = promptMapper.selectByKeyForUpdate(key);
    // 2. 版本快照
    saveVersionSnapshot("prompt", old.getId(), key, old.getVersion(), old.getContent());
    // 3. 更新（version+1，乐观锁）
    int rows = promptMapper.updateContent(key, content, old.getVersion() + 1, userId, old.getVersion());
    if (rows == 0) throw new OptimisticLockException("配置已被其他人修改，请刷新重试");
    // 4. 缓存刷新 + 通知（事务提交后执行）
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override public void afterCommit() {
                refreshCacheAndNotify("prompt", key, content);
            }
        });
}
```

### 5.4 配置格式校验

```java
public void validateTemplate(String content) {
    // 1. 检查变量引用合法性
    Matcher m = Pattern.compile("\\{\\{(\\w+)}}").matcher(content);
    while (m.find()) {
        String var = m.group(1);
        if (!ALLOWED_VARIABLES.contains(var)) {
            throw new ValidationException("未知模板变量: " + var);
        }
    }
    // 2. 检查内容长度
    if (content.length() > 10000) {
        throw new ValidationException("模板内容过长（>10K字符）");
    }
}

public void validateSafetyRule(String pattern, String action) {
    // 正则合法性校验
    try { Pattern.compile(pattern); }
    catch (Exception e) { throw new ValidationException("正则表达式非法: " + e.getMessage()); }
    // action合法性
    if (!Set.of("block","mask","filter","warn").contains(action)) {
        throw new ValidationException("非法规则动作: " + action);
    }
}
```

---

## 六、可拓展性设计

### 6.1 知识源插件化

```java
public interface KnowledgeSource {
    String getType();                          // "faq" / "platform" / "document"
    List<SearchResult> search(String query, float[] queryEmbedding, int topK);
    void upsert(String id, String content, float[] embedding, Map<String,Object> metadata);
    void delete(String id);
}

// 当前实现
@Component class FaqKnowledgeSource implements KnowledgeSource { ... }
@Component class PlatformKnowledgeSource implements KnowledgeSource { ... }
@Component class DocumentKnowledgeSource implements KnowledgeSource { ... }

// 未来扩展（无需改现有代码）
@Component class WebSearchKnowledgeSource implements KnowledgeSource { ... }  // 联网搜索
@Component class DatabaseKnowledgeSource implements KnowledgeSource { ... }  // Text2SQL

// 路由
@Service
public class KnowledgeRouter {
    @Autowired private List<KnowledgeSource> sources;  // Spring自动注入所有实现

    public List<SearchResult> search(String query, float[] embedding, int topK) {
        return sources.parallelStream()
            .flatMap(s -> s.search(query, embedding, topK).stream())
            .sorted(Comparator.comparingDouble(SearchResult::getScore).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }
}
```

### 6.2 工具注册可拓展

```java
// 新增工具只需：1.写Handler 2.数据库注册 3.无需改ToolService
@Component
public class PaymentToolHandler {
    public JsonNode getPaymentStatus(JsonNode args, Long userId) {
        // 业务逻辑
        return paymentService.getStatus(args.get("orderId").asLong(), userId);
    }
}

// DB注册：
// INSERT INTO t_ai_tool_def(tool_name, handler_class, handler_method, ...)
// VALUES('getPaymentStatus', 'com.campustrade.handler.PaymentToolHandler', 'getPaymentStatus', ...);
```

### 6.3 安全规则可拓展

```java
// 新增规则类型只需实现SafetyRuleHandler
public interface SafetyRuleHandler {
    String getType();
    SafetyCheckResult check(String input, AiSafetyRule rule);
}

// 现有：InjectionHandler / MaskHandler / DsmlFilterHandler / BlockedKeywordHandler
// 扩展：RateLimitHandler / ContentModerationHandler / PiiDetectorHandler
```

### 6.4 Prompt模板可组合

```
支持模板嵌套引用：
  system.tool_rules 内容中可写 {{include:system.tool_descriptions}}
  引擎递归解析，最多3层防循环

支持条件块：
  {{#if vision_enabled}}
  你可以分析用户上传的图片...
  {{/if}}
```

### 6.5 AI工具调用主流方案对比与演进路线

#### 当前主流方案全景（2025-2026）

**方案1：Function Calling（项目当前方案）**
```
用户Query + tools定义 → 大模型
  → 模型返回 tool_calls[{name, args}]
  → 执行工具获取结果
  → 结果加入messages → 再次调用模型
  → 循环直到模型不再调用工具
```
- 代表：OpenAI/DeepSeek/Qwen原生API
- 优点：模型原生支持，简单直接
- 缺点：工具定义与业务代码耦合，不可跨项目复用，无标准协议

**方案2：MCP（Model Context Protocol，2025年最热主流）**
```
MCP架构：
  AI应用(Client) ←→ MCP协议 ←→ MCP Server(工具提供方)

MCP Server暴露三类能力：
  ├── Tools    → 可执行函数（如查订单、发邮件）
  ├── Resources → 可读数据源（如文件、数据库）
  └── Prompts  → 可复用提示词模板

流程：
  1. MCP Server注册工具（标准化JSON Schema描述）
  2. Client启动时发现可用工具列表
  3. 用户Query → AI决定调用某工具 → MCP协议传参 → Server执行 → 返回结果
  4. AI拿到结果继续生成回复
```
- Anthropic 2024年底发布，已被Claude/Cursor/Windsurf/DeepSeek等广泛采用
- 核心价值：一次开发处处可用、工具与AI解耦、标准协议(JSON-RPC 2.0)
- 类比：MCP之于AI工具，如同LSP之于IDE

**方案3：LangGraph / LangChain Agent（复杂编排）**
```
LangGraph状态图Agent：
  定义StateGraph节点：
    ├── intent_node    → 意图识别
    ├── retrieve_node  → RAG检索
    ├── tool_node      → 工具调用
    ├── reflect_node   → 自我反思/纠错
    └── respond_node   → 最终回复
  节点间有条件边（按状态决定下一步），支持循环/分支/并行/human-in-the-loop
```
- 适用：复杂多步骤Agent（先搜索→分析→调工具→人工确认→执行）
- 缺点：Python生态，Java无原生支持

**方案4：OpenAI Assistants API（托管方案）**
```
创建Assistant(绑定tools+model+instructions)
  → 创建Thread(对话线程) → 添加Message → Run(自动执行)
  → 框架自动处理tool调用循环 → 返回最终回复
内置工具：code_interpreter / file_search / computer_use
```
- 优点：托管免运维，内置强大工具
- 缺点：锁定OpenAI生态，成本高，数据出境

#### 方案对比矩阵

| 维度 | Function Calling | MCP | LangGraph | Assistants API |
|------|-----------------|-----|-----------|----------------|
| 标准化 | 各模型API不同 | 统一JSON-RPC | 框架私有 | OpenAI私有 |
| 跨项目复用 | ❌ | ✅ | ❌ | ❌ |
| 工具发现 | 代码写死 | 运行时动态 | 代码定义 | 框架管理 |
| 部署形态 | 与后端一体 | 独立进程/服务 | Python进程 | OpenAI托管 |
| 复杂编排 | 简单循环 | 简单循环 | 状态图 | 简单循环 |
| Java支持 | ✅ | ✅(社区SDK) | ❌ | ✅ |
| 成熟度 | 最成熟 | 上升期 | 成熟 | 成熟 |

#### 项目演进路线

```
当前：Function Calling + 35个工具硬编码在AiToolService
  ↓ Phase 1（本方案配置中心）
  工具定义数据库化(t_ai_tool_def) + 反射执行
  → 已可动态增删/启禁工具，无需改代码
  ↓ Phase 2（可选，按需）
  引入MCP协议，将工具暴露为MCP Server
  → 好处：Cursor/Claude等外部工具也能调用你的业务工具
  → 工具定义从"给DeepSeek看的JSON Schema"变为"标准MCP工具声明"
  → 一次实现，所有支持MCP的AI客户端均可复用
```

**关键认知**：Function Calling是底层能力，MCP是上层标准化协议，两者不冲突——MCP Server内部实现可以用Function Calling，MCP解决的是工具定义/发现/调用的标准化和跨应用复用问题。

---

## 七、API设计

### 7.1 提示词模板

```
GET    /api/ai/config/prompts                         # 列表(支持category筛选)
GET    /api/ai/config/prompts/{key}                   # 详情
PUT    /api/ai/config/prompts/{key}                   # 更新(自动记录版本)
GET    /api/ai/config/prompts/{key}/versions          # 版本历史
POST   /api/ai/config/prompts/{key}/rollback/{ver}    # 回滚
POST   /api/ai/config/prompts/preview                 # 预览组装结果
POST   /api/ai/config/prompts/test                    # 在线测试
```

**预览接口**：
```json
POST /api/ai/config/prompts/preview
Request: { "userRole": "USER" }
Response: {
  "sections": {
    "role": "你是校园交易助手小苏...",
    "responsibility": "...",
    "toolRules": "... {{tools}} ...",
    "tools": "1. searchGoods: 搜索商品...\n2. ...",
    "vision": "...",
    "safety": "...",
    "navigation": "..."
  },
  "fullPrompt": "完整组装结果",
  "tokenCount": 1200
}
```

**测试接口**：
```json
POST /api/ai/config/prompts/test
Request: {
  "query": "我想退款",
  "knowledgeSources": ["faq", "platform", "document"],
  "model": "deepseek-chat"
}
Response: {
  "assembledPrompt": "组装后的完整Prompt",
  "ragContext": "注入的知识内容",
  "reply": "AI回复",
  "toolCalls": [...],
  "tokenUsage": { "prompt": 1500, "completion": 200, "total": 1700 }
}
```

### 7.2 工具管理

```
GET    /api/ai/config/tools                          # 列表(支持group筛选)
GET    /api/ai/config/tools/{name}                   # 详情
PUT    /api/ai/config/tools/{name}                   # 更新定义
PATCH  /api/ai/config/tools/{name}/toggle            # 启用/禁用
POST   /api/ai/config/tools/{name}/test              # 在线测试工具
GET    /api/ai/config/tools/{name}/versions          # 版本历史
```

### 7.3 安全规则

```
GET    /api/ai/config/safety-rules                   # 列表(支持type筛选)
POST   /api/ai/config/safety-rules                   # 新增
PUT    /api/ai/config/safety-rules/{id}              # 更新
DELETE /api/ai/config/safety-rules/{id}              # 删除
PATCH  /api/ai/config/safety-rules/{id}/toggle       # 启用/禁用
POST   /api/ai/config/safety-rules/test              # 测试规则(input→result)
```

### 7.4 参数配置

```
GET    /api/ai/config/params                         # 全部参数(按group分组)
PUT    /api/ai/config/params/{group}/{key}           # 更新参数
```

### 7.5 快捷问题

```
GET    /api/ai/quick-questions                       # 公开接口(前端用)
POST   /api/ai/config/quick-questions                # 新增(管理员)
PUT    /api/ai/config/quick-questions/{id}           # 更新
DELETE /api/ai/config/quick-questions/{id}           # 删除
PATCH  /api/ai/config/quick-questions/{id}/toggle    # 启用/禁用
```

### 7.6 版本管理

```
GET    /api/ai/config/versions                       # 全局版本历史(分页)
GET    /api/ai/config/versions/{type}/{key}          # 指定配置的版本历史
POST   /api/ai/config/versions/{type}/{key}/rollback/{ver}  # 回滚
GET    /api/ai/config/versions/compare               # 对比两个版本
```

---

## 八、前端设计

### 8.1 AI配置中心页面（AiConfigCenter.vue）

```
左侧菜单（Tab切换）：
├── 提示词模板
├── 工具管理
├── 安全规则
├── 参数配置
├── 快捷问题
└── 版本历史

右侧内容区（按选中Tab展示）
```

### 8.2 提示词模板管理

```
┌─────────────────────────────────────────────────────────┐
│  提示词模板                          [+ 新增模板]         │
├─────────────────────────────────────────────────────────┤
│  分类筛选: [全部] [system] [review] [safety] [fallback]  │
├───────────────┬──────────────┬────────┬────────────────┤
│  模板Key      │  名称         │ 版本   │ 操作           │
├───────────────┼──────────────┼────────┼────────────────┤
│ system.role   │ 角色定义      │ v3     │ [编辑][预览][历史] │
│ system.tool_..│ 工具调用规则  │ v5     │ [编辑][预览][历史] │
│ review.modera.│ 商品审核提示  │ v2     │ [编辑][预览][历史] │
│ ...           │ ...          │ ...    │ ...            │
└───────────────┴──────────────┴────────┴────────────────┘

编辑弹窗：
┌─────────────────────────────────────────────────────────┐
│  编辑模板: system.tool_rules                    v5       │
├─────────────────────────────────────────────────────────┤
│  ┌─ 变量列表 ──────────────────────────────────────────┐ │
│  │ {{tools}} - 工具列表(动态注入)                      │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌─ Monaco Editor ────────────────────────────────────┐ │
│  │ ## 工具调用规则                                     │ │
│  │ 你可以使用以下工具来帮助用户：                      │ │
│  │ {{tools}}                                          │ │
│  │ 调用规则：                                         │ │
│  │ 1. 只在需要时调用                                  │ │
│  │ ...                                                │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌─ 预览组装结果 ──────────────────────────────────────┐ │
│  │ [点击"预览"展示组装后的完整Prompt]                  │ │
│  └────────────────────────────────────────────────────┘ │
│  变更说明: [________________________]                    │
│  [取消] [预览] [保存]                                    │
└─────────────────────────────────────────────────────────┘
```

### 8.3 工具管理

```
┌─────────────────────────────────────────────────────────┐
│  工具管理                            [+ 新增工具]        │
├─────────────────────────────────────────────────────────┤
│  分组筛选: [全部] [goods] [order] [user] [chat] [admin]  │
├────────────┬────────┬──────┬────────┬──────────────────┤
│  工具名    │ 分组   │ 状态 │ 写操作 │ 操作             │
├────────────┼────────┼──────┼────────┼──────────────────┤
│ searchGoods│ goods  │ 启用 │ 否     │ [编辑][测试][禁用] │
│ cancelOrder│ order  │ 启用 │ 是     │ [编辑][测试][禁用] │
│ admin_ban..│ admin  │ 启用 │ 是     │ [编辑][测试][禁用] │
│ ...        │ ...    │ ...  │ ...    │ ...              │
└────────────┴────────┴──────┴────────┴──────────────────┘

工具测试弹窗：
┌─────────────────────────────────────────────────────────┐
│  测试工具: searchGoods                                   │
│  参数输入(JSON):                                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │ {"keyword": "手机", "page": 1}                     │ │
│  └────────────────────────────────────────────────────┘ │
│  [执行]                                                  │
│  结果:                                                   │
│  ┌────────────────────────────────────────────────────┐ │
│  │ {"total": 5, "items": [...]}                       │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 九、迁移实施计划

### Phase 1：建表 + 数据迁移（1-2天）

**步骤1**：DataInitializer中添加6张表的建表SQL

**步骤2**：编写 `AiConfigMigrationRunner`（一次性迁移）
```java
@Component
@Order(1)
public class AiConfigMigrationRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        if (configMapper.count() > 0) return; // 已迁移过

        // 迁移Prompt模板
        migratePromptTemplates();
        // 迁移工具定义
        migrateToolDefs();
        // 迁移安全规则
        migrateSafetyRules();
        // 迁移参数配置
        migrateConfigs();
        // 迁移快捷问题
        migrateQuickQuestions();
        // 迁移平台知识（从代码fallback到DB）
        migratePlatformKnowledge();

        log.info("AI配置迁移完成");
    }

    private void migratePromptTemplates() {
        // 从AiController.buildDefaultSystemPrompt()提取7个模板
        // 从AiReviewService提取审核/标题优化提示词
        // 从DeepSeekClient提取降级文案
        // 逐条INSERT到t_ai_prompt_template
    }

    private void migrateToolDefs() {
        // 从AiToolService.buildToolCache()提取35个工具定义
        // 每个工具的name/description/parameters/handler信息
        // 逐条INSERT到t_ai_tool_def
    }

    private void migrateSafetyRules() {
        // 从AiSafetyService提取34个Injection正则
        // 提取10个脱敏正则
        // 提取6个DSML过滤正则
        // 从AiReviewService提取24个违禁关键词
        // 逐条INSERT到t_ai_safety_rule
    }

    private void migrateConfigs() {
        // 从AiController提取maxIterations/maxTokenBudget/token预算
        // 从SessionService提取MAX_CONTEXT_TOKENS等
        // 从DeepSeekClient提取熔断参数/temperature
        // 逐条INSERT到t_ai_config
    }
}
```

**步骤3**：验证迁移数据完整性
```sql
-- 对比数量
SELECT 'prompt' as type, COUNT(*) FROM t_ai_prompt_template
UNION ALL SELECT 'tool', COUNT(*) FROM t_ai_tool_def
UNION ALL SELECT 'safety', COUNT(*) FROM t_ai_safety_rule
UNION ALL SELECT 'config', COUNT(*) FROM t_ai_config
UNION ALL SELECT 'question', COUNT(*) FROM t_ai_quick_question;
-- 预期: 10, 35, 74, ~30, 40
```

### Phase 2：配置读取切到DB（2-3天）

逐个改造，每改一处验证：

1. `AiController` → 用PromptEngine.buildSystemPrompt() 替代 buildDefaultSystemPrompt()
2. `AiController` 参数 → 用configService.getInt/getString()
3. `AiToolService` → 从DB加载工具定义 + 反射执行
4. `AiSafetyService` → 用SafetyRuleEngine
5. `AiReviewService` → 从DB读提示词 + 违禁词
6. `DeepSeekClient` → 从DB读熔断/temperature参数
7. `SessionService` → 从DB读会话参数
8. 前端 `AiConsultant.vue` → API获取快捷问题

### Phase 3：管理后台 + 版本控制（2-3天）

1. 后端实现所有配置CRUD API
2. 前端实现AiConfigCenter.vue
3. 实现Redis Pub/Sub多节点同步
4. 实现Prompt预览/测试功能

### Phase 4：清理代码硬编码（1天）

1. 删除AiController.buildDefaultSystemPrompt()
2. 删除AiToolService中的工具定义硬编码
3. 删除AiSafetyService中的正则硬编码
4. 删除AiController.PLATFORM_KNOWLEDGE_BLOCKS
5. 删除前端allSuggestions
6. 保留PromptDefaults作为L4降级兜底

---

## 十、验证方案

### 10.1 功能验证

| 验证项 | 方法 | 预期 |
|--------|------|------|
| 迁移数据完整 | SQL对比数量 | prompt=10,tool=35,safety=74,config=30,question=40 |
| Prompt组装正确 | 预览接口对比 | 组装结果与原buildDefaultSystemPrompt()一致 |
| 工具执行正常 | AI对话测试 | 35个工具均可正常调用 |
| 安全规则生效 | Injection测试 | 输入"忽略指令"被拦截 |
| 参数读取正确 | AI对话测试 | Agent循环/Token预算与原行为一致 |
| 热更新生效 | 后台改Prompt | 不重启即时生效 |
| 多节点同步 | 改配置看其他节点 | 10分钟内或实时同步 |
| 降级可用 | 停MySQL测试 | AI仍可用代码默认值运行 |
| 版本回滚 | 改后回滚 | 恢复到旧版本内容 |

### 10.2 性能验证

| 验证项 | 方法 | 预期 |
|--------|------|------|
| 配置读取性能 | 压测getPromptTemplate | Caffeine命中<1μs |
| 缓存命中率 | 监控 | >99%（启动后基本全命中L1） |
| 启动时间 | 计时 | 增加<2秒（DB加载配置） |
# 更新日志

## 2026-09-11 AI智能助手全面优化

### AI智能助手"小苏" — 对话质量提升

#### System Prompt 增强
- 新增"思考优先"模块：强制AI先理解意图→判断工具→分析图片→检查知识库→诚实原则，再回答
- 新增"图片分析规则"：收到图片时先描述截图内容，再结合用户问题回答
- 新增"平台导航回答规则"：依据平台知识库回答导航问题，不编造不存在的按钮/入口
- 新增多意图处理、自我纠正、上下文记忆等模块

#### 视觉模型真正生效
- 新增 `convertMessagesForVision` 方法：将 `[图片: name](url)` 文本转为 base64 多模态格式发送给视觉模型
- 新增 `imageToDataUrl` 方法：读取服务器图片文件转 base64 data URL
- Agent循环场景路由修复：图片消息 i==0 路由到 `vision` 而非 `reasoning`
- 三个API调用路径均接入视觉消息转换

#### 平台知识库大幅扩充
- 个人中心5个标签页完整说明（我的统计/编辑资料/修改密码/实名认证/收款管理）
- 统计卡片区域6个卡片详情（我的订单/出售商品/完成购物/收货地址/累计消费/累计收入）
- 收货地址入口2种方式（用户下拉菜单 + 个人中心统计卡片）
- 资金流水查看方式（个人中心统计卡片 + 订单详情资金流水时间线）
- 明确声明"平台没有独立的资产/钱包/流水页面"，防止AI编造
- **平台知识始终注入**，不再仅限 needTools 时注入

#### System Prompt 管理优化
- 生产环境 `application-prod.yml` 中 system-prompt 置空，让代码 `buildDefaultSystemPrompt()` 生效
- `GET /ai/prompt` API 改为返回 `{prompt, isCustom, defaultPrompt}` 对象
- 新增 `DELETE /ai/prompt` 重置端点，清除Redis自定义prompt
- 管理后台显示"自定义/代码默认"标签，新增"恢复代码默认"按钮和"查看代码默认值"面板

#### FAQ 知识库扩充（25条 → 43条）
- 新增18条FAQ覆盖：收货地址管理、资金流水查看、个人中心功能、实名认证、收款管理、购物车、关注、通知、商品审核流程、商品上架、累计消费/收入、AI助手功能、订单详情页、修改密码、忘记密码重置
- 创建 `t_faq` 数据库表（此前不存在，FAQ仅存内存/Redis）
- 清除Redis FAQ缓存，43条FAQ已写入数据库并计算4096维embedding向量

#### AI智能FAQ建议
- 新增 `GET /ai/faq/suggest` 端点：从 `t_ai_feedback` 提取最近30条用户提问，发送给AI模型生成FAQ候选
- 单次API调用30秒超时，不走重试逻辑，精简prompt避免超时
- JSON提取容错：从AI返回文本中提取 `[...]` 部分，非JSON时返回空列表
- 管理后台FaqManage页面新增"AI智能建议"按钮和审核弹窗，支持单条采纳/跳过/全部采纳

### 管理后台优化

#### 分类管理分页
- `CategoryManage.vue` 添加 `el-pagination` 分页组件，支持10/20/50条/页切换

#### System Config 页面增强
- Prompt管理区域显示自定义/默认标签
- 新增"恢复代码默认"按钮（清除Redis，回退到代码默认值）
- 新增"查看代码默认值"折叠面板，可对比当前prompt与代码默认值

### 用户端 AI 对话 UI 优化

#### 输入框重新设计
- 透明背景、`rgba(148,163,184,0.3)` 边框（日/夜间均可见）、22px圆角、内阴影
- footer背景色与聊天区一致（`var(--bg-hover)`），去除分隔黑线
- footer顶部padding为0，输入框紧贴顶部

#### 引用功能重构
- 引用内容独立显示在输入框上方（单行+省略号+X删除），参考豆包设计
- 消息中引用特殊显示：`parseQuote()` 函数解析 `> 引用` 前缀，显示为独立块
- 引用块四角圆角，左侧竖线在圆角内部
- 引用内容压平换行再截取，兼容多行旧消息

#### 粘贴图片支持
- `@paste` 事件处理剪贴板图片，自动上传并插入消息

#### 聊天面板宽度增大
- 普通模式：400×600 → 460×640
- 放大模式：28vw/400-620 → 32vw/460-720

#### 内容溢出修复
- `chat-body` 添加 `overflow-x: hidden`
- `msg-wrapper` 添加 `min-width: 0`（flexbox默认min-width:auto不允许子项缩小）
- `msg-bubble` 添加 `max-width: 100%; overflow: hidden`

#### 图片URL隐藏
- `escapeHtml` 中正则替换 `[图片: name](url)` 为 `[图片: name]`，不暴露URL给用户

### 评价功能修复

#### Upsert 逻辑
- `submitFeedback` 改为 upsert：先查 `selectByUserSessionAiResponse`，存在则 `updateRating`，不存在才 insert
- `getSessionFeedback` 添加 `!result.containsKey(key)` 只取最新评价
- 新增 Mapper 方法：`selectByUserSessionAiResponse`、`updateRating`

### 其他

#### DeepSeekClient 增强
- 新增 `chatWithToolsForScene` 方法支持按场景显式路由
- 超时从30秒增至120秒
- `routeByScene` 方法公开化，供外部直接调用

#### 依赖更新
- 新增 highlight.js、markdown-it 依赖

#### 数据库
- 创建 `t_faq` 表（id, question, answer, category, sort_order, create_time）
- 新增 `selectRecentUserMessages` 查询方法（GROUP BY去重，按最新时间排序）

---

## 2026-09-10 AI渠道模型路由 + WebSocket实时踢出 + 管理后台优化

（见 git commit a4ac2ed）
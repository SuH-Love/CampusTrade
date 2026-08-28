# CampusTrade - 校园贸易平台

企业级前后端分离校园贸易平台，基于 Spring Boot 2.7 + Vue 3 + TypeScript 构建，支持支付宝沙箱担保交易。

## 技术栈

### 后端
- Spring Boot 2.7 + Spring Security + JWT + RBAC
- MyBatis + MySQL 8.0
- Redis (缓存/限流/Token黑名单/幂等/分布式锁/AI会话/AI限流)
- RabbitMQ (异步消息/死信队列/手动ACK)
- 支付宝沙箱 SDK (担保交易/退款)
- Spring Boot Mail (QQ邮箱验证码服务)
- Log4j2 (五文件日志/traceId全链路追踪)
- Knife4j (API文档)
- DeepSeek API (AI助手/Function Calling/多轮对话/平台知识注入)
- SseEmitter (AI流式输出/Server-Sent Events)

### 前端
- Vue 3 + TypeScript + Pinia
- Element Plus
- STOMP WebSocket (实时聊天)
- Vite
- markdown-it (AI回复Markdown渲染)

### 部署
- Docker Compose (MySQL/Redis/RabbitMQ/Nginx/后端)
- Nginx (反向代理/安全头/gzip)

## 项目结构

```
CampusTrade/
├── campus-trade-server/          # 后端 Spring Boot
│   └── src/main/java/com/campustrade/
│       ├── controller/           # REST Controller (含AiController)
│       ├── service/impl/         # 业务逻辑 (含EmailServiceImpl邮件服务)
│       ├── service/ai/           # AI助手服务(AI工具/安全/限流/会话/DeepSeek客户端)
│       ├── mapper/               # MyBatis Mapper
│       ├── entity/               # 实体类
│       ├── dto/                  # 请求DTO
│       ├── vo/                   # 响应VO
│       ├── config/               # 配置类(安全/支付宝/Redis等)
│       ├── security/             # JWT/XSS/安全头
│       ├── mq/                   # RabbitMQ消费者(含商品AI审核)
│       ├── aspect/               # AOP操作日志切面
│       └── util/                 # 工具类(含SecurityUtil)
├── campus-trade-user/            # 用户端 Vue3
│   └── src/
│       ├── api/ai.ts             # AI助手SSE流式API
│       └── components/AiConsultant.vue  # AI助手对话组件
├── campus-trade-admin/           # 管理端 Vue3
├── nginx/                        # Nginx配置
└── docker-compose.yml
```

## 快速开始

### 环境要求
- JDK 11+
- Node.js 16+
- MySQL 8.0+
- Redis 6+

### 本地开发

> 开发环境使用 `application.yml`，内置弱密码（root123/redis123/guest）仅用于本地开发，生产环境通过 `.env` 注入强密码。

1. 启动后端（数据库和表会自动创建，无需手动建库建表）：
```bash
cd campus-trade-server
# 修改 application.yml 中的数据库/Redis连接信息
mvn spring-boot:run                          # 默认使用 application.yml (dev)
mvn spring-boot:run -Dspring-boot.run.profiles=prod  # 使用 application-prod.yml (prod)
```

2. 启动用户端前端：
```bash
cd campus-trade-user
npm install && npm run dev
```

3. 启动管理端前端：
```bash
cd campus-trade-admin
npm install && npm run dev
```

### 配置文件切换

项目通过 Spring Boot Profile 机制切换配置文件：

| Profile | 配置文件 | 用途 | 激活方式 |
|---------|----------|------|----------|
| dev（默认） | `application.yml` | 本地开发，密码明文 | `mvn spring-boot:run` |
| prod | `application-prod.yml` | 生产环境，密码从环境变量注入 | `.env` 中 `SPRING_PROFILES_ACTIVE=prod` |

> **Docker 部署**时默认使用 `prod`，通过 `.env` 的 `SPRING_PROFILES_ACTIVE=prod` 激活，所有配置从 `.env` 注入，无需修改任何其他文件。

### Docker 部署

```bash
# 1. 配置环境变量（必须先完成此步，否则容器启动会报错）
cp .env.example .env
# 编辑 .env，填写所有 [必改] 变量（密码、JWT密钥、CORS域名等）
vi .env

# 2. 构建前端
cd campus-trade-user && npm install && npm run build && cd ..
cd campus-trade-admin && npm install && npm run build && cd ..

# 3. 构建后端
cd campus-trade-server && mvn package -DskipTests && cd ..

# 4. 启动所有服务
docker-compose up -d --build
```

> **重要**: `docker-compose.yml` 中所有密码/密钥使用 `${VAR:?error}` 语法，未在 `.env` 中配置将直接报错拒绝启动。请务必先完成 `.env` 配置。

### 环境变量配置

部署前必须配置 `.env` 文件（参考 `.env.example`），以下变量**必须自定义**：

| 变量 | 说明 | 生成方法 |
|------|------|----------|
| `MYSQL_ROOT_PASSWORD` | MySQL root密码（至少16位强密码） | `openssl rand -base64 24` |
| `REDIS_PASSWORD` | Redis密码 | `openssl rand -base64 24` |
| `RABBITMQ_USERNAME` | RabbitMQ用户名 | 自定义（如 `campustrade`） |
| `RABBITMQ_PASSWORD` | RabbitMQ密码 | `openssl rand -base64 24` |
| `JWT_SECRET` | JWT签名密钥（至少32字符） | `openssl rand -base64 48` |
| `ADMIN_PASSWORD` | 管理员初始密码（至少12位强密码） | 自定义强密码 |
| `GRAFANA_ADMIN_PASSWORD` | Grafana管理员密码 | `openssl rand -base64 16` |
| `CORS_ALLOWED_ORIGINS` | 允许的前端域名 | 如 `https://yourdomain.com,https://admin.yourdomain.com` |
| `CORS_ORIGINS` | WebSocket允许域名 | 如 `https://yourdomain.com,https://*.yourdomain.com`（逗号分隔，`*.domain`不匹配根域名） |
| `ALIPAY_NOTIFY_URL` | 支付宝回调URL | 如 `https://yourdomain.com/api/order/pay/notify` |
| `ALIPAY_RETURN_URL` | 支付宝返回URL | 如 `https://yourdomain.com/order/` |

可选变量：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `USER_PASSWORD` | `user123` | 测试用户密码（生产环境建议修改） |
| `DEEPSEEK_API_KEY` | (空) | DeepSeek API Key，留空则AI功能降级为本地FAQ |
| `DEEPSEEK_BASE_URL` | `https://api.siliconflow.cn/v1` | AI API地址 |
| `DEEPSEEK_MODEL` | `deepseek-ai/DeepSeek-V4-Flash` | 模型名称 |
| `FRONTEND_USER_PORT` | `80` | 用户端端口，有宿主机Nginx时设为 `127.0.0.1:8088` |
| `FRONTEND_ADMIN_PORT` | `81` | 管理端端口，有宿主机Nginx时设为 `127.0.0.1:8181` |

> **生产环境HTTPS部署**：当宿主机Nginx做SSL终止时，需在 `.env` 中将前端端口设为 `127.0.0.1:8088` 和 `127.0.0.1:8181`（绑定本机，由宿主机Nginx反代），避免与宿主机80/443端口冲突。

### 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 超级管理员 | admin | `${ADMIN_PASSWORD}` | 密码在 `.env` 中配置，无默认值 |
| 普通用户 | user | `${USER_PASSWORD}` | 密码在 `.env` 中配置，默认 `user123` |

> 首次启动时 `DataInitializer` 自动创建上述账号。密码通过 BCrypt 加密存储，日志不输出明文。

管理端访问：`http://<host>:81`（或 `https://admin.yourdomain.com`）
用户端访问：`http://<host>`（或 `https://yourdomain.com`）

## 核心功能

### 用户端
- 用户注册/登录/JWT双Token认证/实名认证
  - 注册需绑定邮箱（必填），用于重置密码
  - 密码强度校验：8-50位，含大小写字母/数字/特殊字符中三种
- 密码重置：邮箱验证码三步向导（验证身份→输入验证码→设置新密码）
- 商品发布/编辑/审核/上下架/收藏/搜索
  - 商品审核流程：提交审核 → AI自动审核 → 通过/拒绝 → 用户手动上架
  - 已审核/已上架/已下架商品编辑后需重新提交AI审核
- 购物车/订单创建/支付/发货/收货/退款/评价
- 支付宝沙箱担保交易（平台担保→确认收货→结算给卖家）
- 卖家收款配置管理
- STOMP实时聊天/消息撤回
- 收货地址管理（省市区级联）
- 关注/黑名单/通知/系统公告
- 暗色模式/响应式布局

### 管理端
- 仪表盘（数据统计/图表/AI服务状态/邮件服务状态/支付宝配置状态）
- 用户管理（封禁/解封/导出CSV）
- 商品审核/复审（可对AI审核结果改判）/分类管理
- 订单管理（退款审批/导出CSV）
- 举报审核
- 横幅管理/公告管理
- 系统配置
  - 支付宝沙箱密钥配置（AES加密存储）
  - AI助手API配置（热更新）
  - 邮件服务配置（QQ邮箱SMTP/授权码，AES加密存储，热更新）
- 资金流水查看
- 操作日志/安全日志

### AI助手
- **DeepSeek大模型接入**：基于DeepSeek-V4-Flash，支持多轮对话和上下文记忆
- **平台知识注入**：系统提示词中注入平台规则知识（密码重置流程、商品审核流程、商品状态说明等），确保AI回答准确反映最新平台规则
- **条件知识注入**：仅当问题需要工具调用时才注入平台知识，简单对话不注入，减少API输入token
- **模板响应**：常见问题（介绍一下自己/你是谁/你能做什么/你好/谢谢/再见等）直接返回预定义回复，不调用API（0.1s响应）
- **SSE流式输出**：Server-Sent Events实时推送，逐字显示AI回复
- **思考流程展示**：统一可展开时间线，显示AI完整执行流程（理解意图→分析完成→调用工具→查询完成），每步含详细描述
- **Function Calling工具调用**：36个内置工具，AI可自主调用业务接口
  - 商品工具：搜索/详情/发布/上下架/收藏/推荐(8个)
  - 订单工具：查询/创建/支付/发货/收货/退款/评价(10个)
  - 用户工具：信息查询/修改/地址管理/关注/黑名单(8个)
  - 聊天工具：发送消息/查询历史/最近会话(4个)
  - 通知工具：查询/已读/偏好设置(3个)
  - 管理员工具：用户管理/商品审核/举报处理/数据统计(6个，仅管理员可见)
- **安全防护**：
  - Prompt Injection检测（中英文双语模式匹配）
  - 敏感值脱敏（API Key/密码/手机号等正则匹配脱敏）
  - DSML/tool_calls标记过滤（防止AI内部工具调用格式泄露到用户回复）
  - 管理员工具动态过滤（按用户角色过滤工具定义）
  - API配置修改鉴权（仅管理员可修改AI配置）
- **会话管理**：
  - Redis持久化会话历史，支持上下文压缩（超长对话自动摘要）
  - 原子化会话操作（Redis pipeline保证数据一致性）
  - 会话隔离（每用户独立会话空间）
- **限流防护**：Lua脚本原子化限流（每用户每分钟20次）
- **工具执行审计日志**：AI工具调用记录持久化到t_ai_audit_log表
- **FAQ持久化**：常见问题向量化存储（t_faq表），语义匹配自动回复
- **多模型fallback**：API调用失败时自动切换备用模型
- **Markdown渲染**：前端markdown-it渲染AI回复（代码块/列表/表格/链接等）
- **工具调用展示**：折叠卡片展示工具名/结果（参数已隐藏），统一底色与思考流程块
- **中断/重试**：支持发送中中断请求、错误时重试
- **刷新保留**：localStorage保存思考流程数据，刷新页面后恢复展示

### 商品审核系统
- **AI自动审核**：用户提交审核后，MQ异步触发AI审核，检查内容合规性（违禁品/欺诈/绕过平台等）
- **管理员复审**：管理员可在后台对AI审核结果进行复审改判（将通过改为拒绝，或将拒绝改为通过），防止AI误判
- **编辑重新审核**：已审核通过/已上架/已下架的商品编辑后自动重置为待审核状态，重新触发AI审核
- **商品状态**：草稿(DRAFT)→待审核(PENDING)→已审核(APPROVED)/审核拒绝(REJECTED)→在售(ONLINE)→已下架(OFFLINE)/已售出(SOLD)

### 邮件服务
- **QQ邮箱验证码**：重置密码时通过QQ邮箱SMTP发送6位验证码，验证码5分钟有效
- **管理后台热更新配置**：SMTP主机/端口/发件人邮箱/授权码/发件人名称/SSL开关，保存后立即生效无需重启
- **安全存储**：邮箱授权码AES加密存储于t_system_config表，管理端显示掩码

### 支付系统
- **担保交易模式**：买家支付 → 平台担保冻结 → 买家确认收货 → 平台结算给卖家
- 支付宝沙箱支付（管理端配置密钥，AES加密存储）
- 未配置时自动降级为模拟支付
- 退款对接支付宝退款API
- 资金流水全链路记录（PAY/FREEZE/SETTLE/REFUND）
- 订单超时5分钟自动取消（Redis Key过期+分布式锁+定时任务兜底）

## 安全特性

- BCrypt密码加密 + 密码强度校验（8-50位，含大小写/数字/特殊字符三种）
- JWT accessToken(2h) + refreshToken(7d) + Token黑名单
- RBAC三角色权限(ROLE_USER/ROLE_ADMIN/ROLE_SUPER_ADMIN)
- 账号锁定(5次失败锁定30分钟)
- 接口限流 + 防重复提交
- XSS过滤 + SQL注入防御 + 敏感词过滤
- 安全响应头(CSP/X-Frame-Options/HSTS等)
- CORS可配置白名单
- 操作日志 + 安全日志全记录
- 邮箱验证码重置密码（验证码Redis存储，5分钟TTL）
- 系统配置AES加密存储（支付宝密钥/邮件授权码）
- AI安全：Prompt Injection检测(中英文) + 敏感值脱敏 + 管理员工具过滤 + AI配置修改鉴权 + Lua原子限流 + 平台知识注入

## API文档

启动后端后访问：`http://<host>:8080/doc.html`

## License

MIT

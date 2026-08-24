# CampusTrade 完整部署与配置文档

> 按照 `18-delivery-protocol.md` 输出完整交付文档。本文档涵盖项目结构、数据库、API、缓存、消息队列、权限、日志、部署、配置、安全注意事项等全部内容。

---

## 目录

1. [完整项目树](#1-完整项目树)
2. [完整数据库文档](#2-完整数据库文档)
3. [支付宝担保支付系统](#3-支付宝担保支付系统)
4. [STOMP WebSocket实时通信](#4-stomp-websocket实时通信)
5. [AI助手系统](#5-ai助手系统)
6. [完整API文档](#6-完整api文档)
7. [Redis Key文档](#7-redis-key文档)
8. [MQ文档](#8-mq文档)
9. [权限文档](#9-权限文档)
10. [日志文档](#10-日志文档)
11. [环境配置详解](#11-环境配置详解)
12. [Docker部署文档](#12-docker部署文档)
13. [安全注意事项](#13-安全注意事项)
14. [测试报告](#14-测试报告)
15. [性能优化报告](#15-性能优化报告)
16. [常见问题排查](#16-常见问题排查)

---

## 1. 完整项目树

```
CampusTrade/
├── .env.example                           # 环境变量模板
├── docker-compose.yml                     # Docker Compose 编排
├── README.md                              # 项目说明
│
├── campus-trade-docs/                     # 规范文档层(不提交到Git)
│   ├── 00-project-charter.md              # 项目总章程
│   ├── 01-architecture-rules.md           # 架构规则
│   ├── 02-database-design.md              # 数据库设计
│   ├── 03-api-spec.md                     # API规范
│   ├── 04-security-spec.md                # 安全规范
│   ├── 05-cache-spec.md                   # Redis缓存规范
│   ├── 06-mq-spec.md                      # MQ规范
│   ├── 07-log-spec.md                     # 日志规范
│   ├── 08-backend-dev-rules.md            # 后端开发规范
│   ├── 09-frontend-user-spec.md           # 前端用户端规范
│   ├── 10-frontend-admin-spec.md          # 前端管理端规范
│   ├── 11-deployment-spec.md              # 部署规范
│   ├── 12-autonomous-workflow.md          # 自治工作流
│   ├── 13-self-check-protocol.md         # 自检协议
│   ├── 14-continuation-protocol.md       # 断点续航协议
│   ├── 15-refactor-protocol.md           # 重构协议
│   ├── 16-bugfix-protocol.md             # 修复协议
│   ├── 17-performance-protocol.md        # 性能优化协议
│   ├── 18-delivery-protocol.md           # 交付协议
│   └── prompts/                           # 模块Prompt
│
├── campus-trade-server/                   # 后端 SpringBoot
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/campustrade/
│       │   ├── CampusTradeApplication.java
│       │   ├── aspect/                    # AOP切面(操作日志/防重复/限流)
│       │   ├── base/BaseEntity.java
│       │   ├── common/                    # Result/PageResult/ResultCode
│       │   ├── config/                    # 配置类(12个)
│       │   │   ├── AlipayConfig.java      # 支付宝沙箱配置(数据库优先+yml兜底)
│       │   │   ├── CorsConfig.java        # 跨域配置(可配置allowed-origins)
│       │   │   ├── DataInitializer.java   # 数据库初始化(26张表+初始数据+字段迁移)
│       │   │   ├── FilterConfig.java      # XSS/安全头过滤器注册
│       │   │   ├── Knife4jConfig.java     # API文档配置
│       │   │   ├── RabbitMQConfig.java    # MQ配置(5交换机+5队列+死信)
│       │   │   ├── RedisConfig.java       # Redis序列化配置
│       │   │   ├── SecurityConfig.java    # Spring Security配置
│       │   │   ├── SpringfoxBeanPostProcessor.java  # Springfox兼容
│       │   │   ├── StompEventListener.java # STOMP在线状态追踪
│       │   │   ├── WebSocketConfig.java   # STOMP WebSocket配置
│       │   │   └── WebMvcConfig.java      # 静态资源映射(上传文件)
│       │   ├── constant/                  # 常量类(4个)
│       │   │   ├── AccountLockConstant.java  # 账号锁定常量
│       │   │   ├── MQConstant.java        # MQ交换机/队列/路由键
│       │   │   ├── RedisConstant.java     # Redis Key前缀+TTL
│       │   │   └── SecurityConstant.java  # 角色/白名单/限流
│       │   ├── controller/                # 20个Controller
│       │   ├── dto/                       # 14个DTO
│       │   ├── entity/                    # 26个Entity
│       │   ├── enum_/                     # 6个枚举
│       │   ├── exception/                 # 全局异常处理(10种异常)
│       │   ├── mapper/                    # 26个Mapper接口
│       │   ├── mq/                        # 6个Consumer(含死信)
│       │   ├── security/                  # JWT/认证/过滤器
│       │   ├── service/                   # 14个Service接口(含EmailService)
│       │   ├── service/impl/              # 14个ServiceImpl(含EmailServiceImpl)
│       │   ├── service/ai/                # AI助手服务(5个)
│       │   │   ├── AiToolService.java     # 36个Function Calling工具
│       │   │   ├── AiSafetyService.java   # Prompt Injection检测+敏感值脱敏
│       │   │   ├── AiRateLimiter.java     # Lua脚本原子限流
│       │   │   ├── SessionService.java    # Redis会话管理+上下文压缩
│       │   │   └── DeepSeekClient.java    # DeepSeek API客户端+配置持久化
│       │   ├── util/                      # 工具类(8个)
│       │   └── vo/                        # 16个VO
│       ├── main/resources/
│       │   ├── application.yml            # 开发环境配置(含createDatabaseIfNotExist=true)
│       │   ├── application-prod.yml       # 生产环境配置(环境变量注入)
│       │   ├── log4j2.xml                 # 5文件日志配置
│       │   └── mapper/                    # 26个Mapper XML
│       └── test/java/com/campustrade/     # 8个单元测试类
│
├── campus-trade-user/                     # 用户端前端 Vue3
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/                           # 18个API模块(含ai.ts AI助手SSE)
│       ├── components/                    # 共享组件(GoodsCard/AuthLayout/AiConsultant等)
│       ├── layouts/MainLayout.vue
│       ├── pages/                         # 22个页面
│       ├── router/index.ts
│       ├── stores/user.ts                 # Pinia状态管理
│       ├── styles/index.scss              # 全局样式(CSS变量+暗色模式)
│       ├── types/index.ts                 # TypeScript类型定义
│       ├── utils/area.ts                  # 省市区级联数据
│       └── utils/request.ts               # Axios封装(含Token刷新)
│
├── campus-trade-admin/                    # 管理端前端 Vue3
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/admin.ts                   # 统一管理端API
│       ├── layouts/AdminLayout.vue
│       ├── pages/                         # 12个页面
│       ├── router/index.ts
│       ├── stores/admin.ts                # Pinia状态管理
│       ├── utils/labels.ts                # 模块/操作中文映射
│       └── utils/request.ts               # Axios封装(含Token刷新)
│
└── nginx/
    ├── user.conf                          # 用户端Nginx配置(含/ws和/uploads代理)
    └── admin.conf                         # 管理端Nginx配置(含/uploads代理)
```

---

## 2. 完整数据库文档

### 基本信息

| 项 | 值 |
|----|-----|
| 数据库名 | `campus_trade` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_general_ci` |
| 引擎 | InnoDB |
| 时区 | `+08:00` (Asia/Shanghai) |
| 自动建库 | JDBC URL 含 `createDatabaseIfNotExist=true` |

### 建表方式

> **重要**: 项目已移除 `init.sql`，所有建表和数据初始化由 `DataInitializer.java` 程序化完成。
> - 使用 `CREATE TABLE IF NOT EXISTS`，幂等安全，可重复执行
> - 使用 `addColumnIfNotExists()` 为已有表补充新字段（兼容增量升级）
> - 应用启动时自动执行，无需手动导入SQL

### 表清单(26张)

| 表名 | 说明 | 公共字段 |
|------|------|----------|
| t_user | 用户表 | id, create_time, update_time, deleted, version |
| t_role | 角色表 | 同上 |
| t_permission | 权限表 | 同上 |
| t_user_role | 用户角色关联表 | 同上 |
| t_role_permission | 角色权限关联表 | 同上 |
| t_goods_category | 商品分类表 | 同上 |
| t_goods | 商品表 | 同上 |
| t_goods_favorite | 商品收藏表 | 同上 |
| t_order | 订单表 | 同上 + trade_no, pre_refund_status, seller_payment_config_id |
| t_order_item | 订单明细表 | 同上 |
| t_cart | 购物车表 | 同上 |
| t_chat_message | 聊天消息表 | 同上 |
| t_report | 举报表 | 同上 |
| t_notification | 通知表 | 同上 |
| t_notification_preference | 通知偏好表 | 同上 |
| t_operation_log | 操作日志表 | 同上 |
| t_security_log | 安全日志表 | 同上 |
| t_banner | 横幅广告表 | 同上 |
| t_announcement | 系统公告表 | 同上 |
| t_user_blacklist | 用户黑名单表 | 同上 |
| t_seller_rating | 卖家评分表 | 同上 |
| t_user_follow | 用户关注表 | 同上 |
| t_delivery_address | 收货地址表 | 同上 |
| t_payment_config | 卖家收款配置表 | 同上 |
| t_fund_log | 资金流水表 | 同上 |
| t_system_config | 系统配置表 | 同上 |
| t_ai_audit_log | AI工具审计日志表 | 同上 |
| t_faq | FAQ常见问题表 | 同上 |

### 索引规范

**唯一索引**: username, phone, email, role_code, permission_code, order_no

**联合索引**: (user_id, status), (goods_id, status), (sender_id, receiver_id)

> 注意: 软删除表不建 UNIQUE KEY 约束，INSERT 失败时 catch 并调用 restore 方法恢复记录。

### 初始化数据

- 3个角色: ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_USER
- 11个权限: goods:create/update/delete/audit, user:ban, report:review, log:view 等
- 6个商品分类: 数码电子, 书籍教材, 生活用品, 服装鞋帽, 运动户外, 其他
- 1个超级管理员账号: admin / Admin123!@（密码通过 `.env` 的 `ADMIN_PASSWORD` 配置）
- 10个示例商品（首页展示用）
- 2个横幅广告

### DataInitializer 关键机制

| 机制 | 说明 |
|------|------|
| `safeRun()` | 包装每个初始化方法，单个失败不影响后续 |
| `CREATE TABLE IF NOT EXISTS` | 建表幂等，重复启动安全 |
| `addColumnIfNotExists()` | 增量字段迁移，已存在的列自动跳过 |
| `safeInsertFundLog()` | 资金日志写入保护，表异常时不影响主流程 |
| 数据库自动创建 | JDBC URL 含 `createDatabaseIfNotExist=true` |

---

## 3. 支付宝担保支付系统

### 3.1 架构概述

平台采用**担保交易模式**，资金流转路径：

```
买家支付 → 平台商户号(担保冻结) → 买家确认收货 → 平台结算给卖家
```

退款路径：卖家同意 → 支付宝退款API → 资金退回买家

### 3.2 支付流程

```
1. 买家下单 → 创建订单(PENDING)
2. 买家支付 → 调用支付宝沙箱支付页面
3. 支付成功 → 支付宝异步通知 /pay/notify
4. 通知处理 → 验签 + 分布式锁 + 订单状态更新 + 担保冻结(FundLog: FREEZE)
5. 卖家发货 → 订单状态更新为SHIPPED
6. 买家确认收货 → 订单状态更新为FINISHED + 结算给卖家(FundLog: SETTLE)
```

### 3.3 退款流程

```
1. 买家申请退款 → 记录pre_refund_status + 订单状态更新为REFUNDING
2. 卖家同意退款 → 调用支付宝退款API + FundLog: REFUND + 恢复状态
3. 卖家拒绝退款 → 从pre_refund_status恢复原状态
4. 管理员也可审批退款(/api/admin/order/{id}/approve-refund)
```

### 3.4 配置管理

支付宝沙箱配置通过**管理端后台**设置（`t_system_config` 表），不通过 `.env`/`yml`：

| 配置项 | system_config key | 说明 |
|--------|-------------------|------|
| 支付宝网关 | alipay.gateway | 沙箱: `https://openapi-sandbox.dl.alipaydev.com/gateway.do` |
| App ID | alipay.app_id | 沙箱应用ID |
| 应用私钥 | alipay.private_key | AES加密存储，管理端显示掩码 `******` |
| 支付宝公钥 | alipay.alipay_public_key | AES加密存储 |
| 支付模式 | alipay.mode | `sandbox` / `simulation` |

**邮件服务配置**（同样通过管理端后台设置）：

| 配置项 | system_config key | 说明 |
|--------|-------------------|------|
| SMTP服务器 | mail.host | 默认 `smtp.qq.com` |
| SMTP端口 | mail.port | 默认 `465`（SSL）或 `587`（TLS） |
| 发件人邮箱 | mail.username | QQ邮箱地址 |
| 邮箱授权码 | mail.password | AES加密存储，管理端显示掩码 `******` |
| 发件人名称 | mail.from | 默认 `CampusTrade校园贸易` |
| 启用SSL | mail.ssl | 默认 `true` |

**降级机制**: 未配置支付宝参数时，自动降级为模拟支付（直接标记支付成功）。

**配置优先级**: 数据库 `t_system_config` > `application.yml` 兜底值

### 3.5 关键实现

| 组件 | 文件 | 说明 |
|------|------|------|
| 支付宝配置 | `AlipayConfig.java` | `resolve()` 优先从数据库读取，yml兜底 |
| 订单支付 | `OrderServiceImpl.payOrder()` | 创建支付宝支付表单HTML |
| 异步通知 | `OrderServiceImpl.handlePayNotify()` | 验签+分布式锁+幂等 |
| 退款审批 | `OrderServiceImpl.approveRefund()` | 调用退款API+资金流水 |
| 拒绝退款 | `OrderServiceImpl.rejectRefund()` | 恢复preRefundStatus |
| 结算 | `OrderServiceImpl.finish()` | 确认收货时结算给卖家 |
| 收款配置 | `PaymentConfigServiceImpl` | 卖家收款信息管理 |
| 系统配置 | `SystemConfigServiceImpl` | 支付宝参数CRUD+AES加解密 |
| 资金日志 | `safeInsertFundLog()` | 写入保护，表异常不影响主流程 |

### 3.6 FundLog 资金流水类型

| 类型 | 说明 | 触发时机 |
|------|------|----------|
| PAY | 买家支付 | 支付成功通知 |
| FREEZE | 担保冻结 | 支付成功后冻结资金 |
| SETTLE | 结算给卖家 | 买家确认收货 |
| REFUND | 退款 | 卖家同意退款 |

### 3.7 分布式锁

支付通知处理使用 Redis 分布式锁，防止并发重复处理：

| 锁Key | TTL | 说明 |
|-------|-----|------|
| `lock:pay:notify:{orderId}` | 30s | 支付通知处理锁 |
| `lock:refund:{orderId}` | 30s | 退款处理锁 |

---

## 4. STOMP WebSocket实时通信

### 4.1 架构

采用 STOMP 协议（基于 SockJS），替代原生 WebSocket：

```
前端(@stomp/stompjs) ←→ Nginx(/ws) ←→ Spring(@EnableWebSocketMessageBroker)
```

### 4.2 STOMP 端点

| 端点 | 说明 |
|------|------|
| `/ws` | SockJS 握手端点 |
| `/user/queue/chat` | 用户私聊消息订阅 |
| `/user/queue/notification` | 用户通知订阅 |
| `/app/chat/send` | 发送消息 |

### 4.3 在线状态追踪

`StompEventListener` 监听 STOMP 连接/断开事件，通过 `StompHeaderAccessor.wrap(message).getUser()` 提取用户身份，维护在线用户集合。

### 4.4 屏蔽状态实时推送

屏蔽/解除屏蔽时通过 STOMP 实时推送通知双方：

| 操作 | 推送给操作者 | 推送给被屏蔽者 |
|------|-------------|---------------|
| 屏蔽 | `{ type: "BLOCKED", userId: 被屏蔽者ID }` | `{ type: "BLOCKED_BY", userId: 操作者ID, blocked: true }` |
| 解除屏蔽 | `{ type: "UNBLOCKED", userId: 被屏蔽者ID }` | `{ type: "BLOCKED_BY", userId: 操作者ID, blocked: false }` |

前端 `useChatWs` 处理这些事件，实时更新聊天界面的屏蔽状态（禁用输入、显示提示、侧边栏图标）。

### 4.4 Nginx 代理配置

```nginx
location /ws {
    proxy_pass http://backend:8080/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}
```

### 4.5 聊天消息类型

| messageType | 说明 |
|-------------|------|
| 1 | 文本消息 |
| 2 | 图片消息 |
| 3 | 商品/订单卡片 |
| 4 | 已撤回消息 |

---

## 5. AI助手系统

### 5.1 架构概述

AI助手基于DeepSeek大模型（DeepSeek-V4-Flash），通过Function Calling机制实现与业务系统的深度集成：

```
用户消息 → AiController → DeepSeekClient → DeepSeek API
                ↓                              ↓
          AiSafetyService              Function Calling
          (Injection检测)                     ↓
                ↓                    AiToolService.executeTool()
          AiRateLimiter              (36个业务工具)
          (Lua原子限流)                      ↓
                ↓                    业务Service/Mapper
          SessionService                     ↓
          (Redis会话管理)              工具结果返回AI
                ↓                              ↓
          SseEmitter ← ← ← ← ← ← ← ← 流式SSE输出
```

### 5.2 SSE流式接口

| 接口 | 方法 | 说明 | 鉴权 |
|------|------|------|------|
| /api/ai/chat/stream | GET | SSE流式对话（主接口） | 是 |
| /api/ai/chat | POST | 非流式对话 | 是 |
| /api/ai/config | GET | 获取AI配置状态 | ADMIN+ |
| /api/ai/config | PUT | 修改AI配置（API Key/Model/Base URL） | ADMIN+ |
| /api/ai/history/{sessionId} | GET | 获取会话历史 | 是 |
| /api/ai/sessions | GET | 获取用户所有会话 | 是 |
| /api/ai/session/{sessionId} | DELETE | 删除会话 | 是 |

**SSE请求参数**（GET query string）：

| 参数 | 类型 | 说明 |
|------|------|------|
| message | String | 用户消息内容 |
| sessionId | String | 会话ID（可选，不传则新建） |

**SSE事件类型**：

| 事件 | data格式 | 说明 |
|------|----------|------|
| `session` | `{"sessionId":"xxx"}` | 会话ID（首个事件） |
| `thinking` | `{"status":"thinking"}` | AI思考中状态 |
| `message` | `{"content":"token"}` | AI回复内容（逐token推送） |
| `tool_call` | `{"tool":"工具名","args":{参数}}` | 工具调用开始 |
| `tool_result` | `{"tool":"工具名","result":结果}` | 工具调用结果 |
| `done` | `{"content":"完整回复"}` | 回复完成 |
| `error` | `{"error":"错误信息"}` | 错误 |

> **重要**：`message` 事件的 data 是 JSON 格式 `{"content":"token"}`，不是纯文本。这是因为token中可能包含 `\n`，直接用SSE的 `data:` 写入会破坏SSE格式。前端需 `JSON.parse(e.data).content` 提取内容。

### 5.3 Function Calling工具清单（36个）

#### 商品工具（8个）

| 工具名 | 说明 | 参数 |
|--------|------|------|
| searchGoods | 搜索商品 | keyword, page, limit(默认10,最大20) |
| getGoodsDetail | 商品详情 | goodsId |
| publishGoods | 发布商品 | title, description, price, categoryId, images |
| editGoods | 编辑商品 | goodsId, title, description, price, categoryId |
| submitForAudit | 提交审核 | goodsId |
| takeOnline | 上架 | goodsId |
| takeOffline | 下架 | goodsId |
| recommendGoods | 推荐商品 | - |

#### 订单工具（10个）

| 工具名 | 说明 | 参数 |
|--------|------|------|
| getMyOrders | 我的订单 | role(buyer/seller), status, page |
| getOrderDetail | 订单详情 | orderId |
| createOrder | 创建订单 | goodsId, quantity, addressId |
| payOrder | 支付订单 | orderId |
| shipOrder | 发货 | orderId |
| confirmReceive | 确认收货 | orderId |
| requestRefund | 申请退款 | orderId, reason |
| approveRefund | 同意退款 | orderId |
| rejectRefund | 拒绝退款 | orderId, reason |
| rateSeller | 评价卖家 | orderId, score, comment |

#### 用户工具（8个）

| 工具名 | 说明 | 参数 |
|--------|------|------|
| getUserInfo | 获取个人信息 | - |
| updateUserInfo | 修改个人信息 | nickname, avatar, phone |
| getAddresses | 地址列表 | - |
| addAddress | 添加地址 | receiver, phone, province, city, district, detail |
| updateAddress | 修改地址 | addressId, ... |
| deleteAddress | 删除地址 | addressId |
| followUser | 关注用户 | userId |
| blacklistUser | 拉黑用户 | userId |

#### 聊天工具（4个）

| 工具名 | 说明 | 参数 |
|--------|------|------|
| sendMessage | 发送聊天消息 | receiverId, content, messageType |
| getChatHistory | 聊天记录 | targetUserId, page |
| getRecentContacts | 最近会话 | - |
| getUnreadCount | 未读消息数 | senderId |

#### 通知工具（3个）

| 工具名 | 说明 | 参数 |
|--------|------|------|
| getNotifications | 通知列表 | page, unreadOnly |
| markNotificationRead | 标记已读 | notificationId |
| getNotificationPreference | 通知偏好 | - |

#### 管理员工具（6个，仅ROLE_ADMIN/ROLE_SUPER_ADMIN可见）

| 工具名 | 说明 | 参数 |
|--------|------|------|
| adminGetUsers | 用户列表 | page, keyword |
| adminBanUser | 封禁用户 | userId, reason |
| adminAuditGoods | 审核商品 | goodsId, approved, reason |
| adminHandleReport | 处理举报 | reportId, action |
| adminGetDashboardStats | 仪表盘统计 | - |
| adminGetFundLogs | 资金流水 | page, startDate, endDate |

> **安全过滤**：`AiToolService.getToolDefinitions()` 根据 `SecurityUtil.isAdmin()` 动态过滤管理员工具。普通用户只能看到30个工具，管理员可以看到全部36个。

### 5.4 会话管理

| 机制 | 说明 |
|------|------|
| 会话存储 | Redis List，Key: `ai:session:{userId}:{sessionId}` |
| 上下文窗口 | 最近20条消息（约8000 token） |
| 上下文压缩 | 超过20条时，旧消息通过AI摘要压缩后保留 |
| 压缩原子化 | Redis pipeline：del + rpush + expire 原子执行 |
| 会话隔离 | 每用户独立会话空间，Key含userId |
| 会话TTL | 7天自动过期 |

### 5.5 安全机制

| 机制 | 实现 | 说明 |
|------|------|------|
| Prompt Injection检测 | `AiSafetyService.checkInput()` | 中英文双语模式匹配（12种中文+多种英文模式），预编译Pattern |
| 敏感值脱敏 | `AiSafetyService.sanitizeOutput()` | 正则匹配实际敏感值（API Key/密码/手机号），只脱敏值部分 |
| 管理员工具过滤 | `AiToolService.getToolDefinitions()` | 按角色动态过滤工具定义 |
| AI配置修改鉴权 | `AiController.updateAiConfig()` | 仅ROLE_ADMIN/ROLE_SUPER_ADMIN可修改 |
| 限流 | `AiRateLimiter.checkRate()` | Lua脚本原子化INCR+EXPIRE，每用户20次/分钟 |
| API Key持久化 | `DeepSeekClient` | 配置修改同步写入Redis，重启后从Redis加载 |

### 5.6 DeepSeek API配置

| 配置项 | application.yml路径 | 默认值 | 说明 |
|--------|---------------------|--------|------|
| API Key | `ai.api-key` | sk-nmcq... | DeepSeek API密钥 |
| Base URL | `ai.base-url` | https://api.siliconflow.cn/v1 | API基础URL |
| Model | `ai.model` | deepseek-ai/DeepSeek-V4-Flash | 模型名称 |
| System Prompt | `ai.system-prompt` | (内置) | 系统提示词 |
| Max Tokens | `ai.max-tokens` | 2048 | 最大输出token数 |
| Temperature | `ai.temperature` | 0.7 | 温度参数 |
| 上下文窗口 | `ai.context-window` | 20 | 上下文消息条数 |
| 限流次数 | `ai.rate-limit` | 20 | 每分钟请求限制 |

> **热更新**：管理员可通过 `PUT /api/ai/config` 修改API Key/Model/Base URL，修改后立即生效并持久化到Redis，重启后自动加载。

### 5.7 平台知识注入

系统提示词中注入平台规则知识，确保AI回答准确反映最新平台规则：

| 知识域 | 内容 |
|--------|------|
| 密码与账号 | 注册要求（强密码）、重置密码流程（邮箱验证码非手机号）、登录锁定规则 |
| 商品发布与审核 | 完整审核流程（草稿→AI审核→管理员复审→上架）、编辑重新审核、7种状态说明 |
| 订单交易 | 订单流程、支付宝担保交易、配送方式、评价 |
| 其他功能 | 收藏、购物车、关注、聊天、举报、通知 |
| 回答要求 | 依据知识准确回答，不编造功能；涉及具体数据时调用工具获取 |

**实现**：`AiController.buildPlatformKnowledge()` 方法在每次对话时动态拼接到系统提示词，与 `buildDateHint()` 一起注入。

### 5.8 Agent Loop机制

当用户消息命中工具关键词时，进入Agent Loop（多轮工具调用）：

```
1. 用户消息 → AI分析意图
2. AI返回tool_calls → 执行工具 → 结果返回AI
3. AI根据工具结果继续分析 → 可能再次调用工具
4. 最多3轮工具调用 → AI生成最终回复
5. 全程通过SSE推送 tool_call/tool_result 事件
```

**SSE超时**：300秒（Agent Loop最多3轮×60s=180s，预留缓冲）

### 5.9 前端组件

| 组件 | 文件 | 说明 |
|------|------|------|
| AI对话组件 | `AiConsultant.vue` | Markdown渲染、工具调用折叠卡片、中断/重试、时间戳/复制 |
| AI API封装 | `api/ai.ts` | SSE解析、JSON解析message事件、tool_call/tool_result回调 |

**Markdown渲染策略**：流式输出过程中用纯文本 `escapeHtml()` 显示（避免DOM重渲染导致逐字丢失），流式结束后才用 `renderMarkdown()` 渲染最终Markdown。

**复制功能**：优先使用 `navigator.clipboard`（HTTPS环境），降级使用 `document.execCommand('copy')`（HTTP环境）。

---

## 6. 完整API文档

启动后访问: `http://localhost:8080/doc.html` (Knife4j，仅开发环境启用)

### API清单

| 模块 | 方法 | 路径 | 说明 | 鉴权 | 限流 |
|------|------|------|------|------|------|
| **认证** | POST | /api/auth/register | 注册(邮箱必填) | 否 | @RateLimit |
| | POST | /api/auth/login | 登录 | 否 | @RateLimit |
| | POST | /api/auth/logout | 退出 | 是 | - |
| | POST | /api/auth/refresh | 刷新Token | 否 | - |
| | POST | /api/auth/send-code | 发送重置密码验证码(邮箱) | 否 | @RateLimit |
| | POST | /api/auth/reset-password | 重置密码(邮箱验证码) | 否 | @RateLimit |
| **用户** | GET | /api/user/info | 获取个人信息 | 是 | - |
| | PUT | /api/user/info | 修改个人信息 | 是 | - |
| | PUT | /api/user/password | 修改密码 | 是 | - |
| | POST | /api/user/verify | 实名认证 | 是 | - |
| | POST | /api/user/avatar | 上传头像 | 是 | - |
| **商品** | POST | /api/goods | 发布商品 | 是 | - |
| | PUT | /api/goods/{id} | 修改商品 | 是 | - |
| | DELETE | /api/goods/{id} | 删除商品 | 是 | - |
| | GET | /api/goods/{id} | 商品详情 | 否 | - |
| | GET | /api/goods | 商品列表 | 否 | - |
| | GET | /api/goods/hot | 热门商品 | 否 | - |
| | GET | /api/goods/recommend | 推荐商品 | 否 | - |
| | GET | /api/goods/my | 我的商品 | 是 | - |
| | PUT | /api/goods/{id}/submit | 提交审核 | 是 | - |
| | PUT | /api/goods/{id}/online | 上架 | 是 | - |
| | PUT | /api/goods/{id}/offline | 下架 | 是 | - |
| | POST | /api/goods/{id}/favorite | 收藏 | 是 | - |
| | DELETE | /api/goods/{id}/favorite | 取消收藏 | 是 | - |
| **分类** | GET | /api/category | 分类列表 | 否 | - |
| | POST | /api/category | 创建分类 | ADMIN+ | - |
| | PUT | /api/category/{id} | 修改分类 | ADMIN+ | - |
| | DELETE | /api/category/{id} | 删除分类 | ADMIN+ | - |
| **订单** | POST | /api/order | 创建订单 | 是 | - |
| | PUT | /api/order/{id}/cancel | 取消订单 | 是 | - |
| | PUT | /api/order/{id}/pay | 支付(模拟) | 是 | - |
| | POST | /api/order/{id}/create-payment | 创建支付宝支付 | 是 | - |
| | POST | /api/order/pay/notify | 支付宝异步通知 | 否 | - |
| | PUT | /api/order/{id}/ship | 发货 | 是 | - |
| | PUT | /api/order/{id}/finish | 确认收货 | 是 | - |
| | PUT | /api/order/{id}/refund | 申请退款 | 是 | - |
| | PUT | /api/order/{id}/approve-refund | 同意退款 | 是 | - |
| | PUT | /api/order/{id}/reject-refund | 拒绝退款 | 是 | - |
| | PUT | /api/order/{id}/modify-price | 修改订单金额 | 是 | - |
| | GET | /api/order/{id} | 订单详情 | 是 | - |
| | GET | /api/order/{id}/fund-logs | 订单资金流水 | 是 | - |
| | GET | /api/order/buyer | 买家订单 | 是 | - |
| | GET | /api/order/seller | 卖家订单 | 是 | - |
| **购物车** | GET | /api/cart | 购物车列表 | 是 | - |
| | POST | /api/cart | 加入购物车 | 是 | - |
| | PUT | /api/cart/{id} | 更新数量 | 是 | - |
| | DELETE | /api/cart/{id} | 移除商品 | 是 | - |
| **聊天** | POST | /api/chat | 发送消息 | 是 | - |
| | GET | /api/chat/history/{targetUserId} | 聊天记录 | 是 | - |
| | GET | /api/chat/recent | 最近会话 | 是 | - |
| | GET | /api/chat/unread/{senderId} | 未读数量 | 是 | - |
| | PUT | /api/chat/read/{senderId} | 标记已读 | 是 | - |
| | PUT | /api/chat/recall/{id} | 撤回消息 | 是 | - |
| **举报** | POST | /api/report | 提交举报 | 是 | - |
| | GET | /api/report/mine | 我的举报 | 是 | - |
| | PUT | /api/report/{id}/handle | 处理举报 | ADMIN+ | - |
| **通知** | GET | /api/notification | 通知列表 | 是 | - |
| | GET | /api/notification/unread-count | 未读数量 | 是 | - |
| | PUT | /api/notification/{id}/read | 标记已读 | 是 | - |
| | PUT | /api/notification/read-all | 全部已读 | 是 | - |
| | DELETE | /api/notification/{id} | 删除通知 | 是 | - |
| **通知偏好** | GET | /api/notification-preference | 获取偏好 | 是 | - |
| | PUT | /api/notification-preference | 更新偏好 | 是 | - |
| **收款配置** | GET | /api/payment-config | 收款配置列表 | 是 | - |
| | GET | /api/payment-config/default | 默认收款配置 | 是 | - |
| | POST | /api/payment-config | 添加收款配置 | 是 | - |
| | PUT | /api/payment-config/{id} | 修改收款配置 | 是 | - |
| | DELETE | /api/payment-config/{id} | 删除收款配置 | 是 | - |
| | PUT | /api/payment-config/{id}/default | 设为默认 | 是 | - |
| **收货地址** | GET | /api/address | 地址列表 | 是 | - |
| | POST | /api/address | 添加地址 | 是 | - |
| | PUT | /api/address/{id} | 修改地址 | 是 | - |
| | DELETE | /api/address/{id} | 删除地址 | 是 | - |
| | PUT | /api/address/{id}/default | 设为默认 | 是 | - |
| **用户关注** | POST | /api/follow/{userId} | 关注用户 | 是 | - |
| | DELETE | /api/follow/{userId} | 取消关注 | 是 | - |
| | GET | /api/follow/following | 关注列表 | 是 | - |
| | GET | /api/follow/followers | 粉丝列表 | 是 | - |
| **卖家评分** | POST | /api/rating | 评价卖家 | 是 | - |
| | GET | /api/rating/seller/{sellerId} | 卖家评分 | 否 | - |
| **黑名单** | POST | /api/blacklist/{userId} | 拉黑用户 | 是 | - |
| | DELETE | /api/blacklist/{userId} | 取消拉黑 | 是 | - |
| | GET | /api/blacklist | 黑名单列表 | 是 | - |
| | GET | /api/blacklist/is-blocked/{userId} | 是否已屏蔽对方 | 是 | - |
| | GET | /api/blacklist/is-blocked-by/{userId} | 是否被对方屏蔽 | 是 | - |
| **横幅** | GET | /api/banner | 横幅列表 | 否 | - |
| **公告** | GET | /api/announcement | 公告列表 | 否 | - |
| **文件** | POST | /api/file/upload | 上传图片 | 是 | @RateLimit |
| | DELETE | /api/file/delete | 删除图片 | 是 | - |
| **管理** | GET | /api/admin/dashboard/stats | 仪表盘统计 | ADMIN+ | - |
| | GET | /api/admin/user | 用户列表 | ADMIN+ | - |
| | PUT | /api/admin/user/{id}/ban | 封禁用户 | ADMIN+ | - |
| | PUT | /api/admin/user/{id}/unban | 解封用户 | ADMIN+ | - |
| | GET | /api/admin/goods | 商品审核列表 | ADMIN+ | - |
| | PUT | /api/admin/goods/{id}/audit | 审核商品 | ADMIN+ | - |
| | GET | /api/admin/order | 订单管理列表 | ADMIN+ | - |
| | PUT | /api/admin/order/{id}/approve-refund | 管理员同意退款 | ADMIN+ | - |
| | PUT | /api/admin/order/{id}/reject-refund | 管理员拒绝退款 | ADMIN+ | - |
| | GET | /api/admin/report | 举报管理列表 | ADMIN+ | - |
| | PUT | /api/admin/report/{id}/resolve | 举报通过 | ADMIN+ | - |
| | PUT | /api/admin/report/{id}/dismiss | 举报驳回 | ADMIN+ | - |
| | GET | /api/admin/log/operation | 操作日志 | ADMIN+ | - |
| | GET | /api/admin/log/security | 安全日志 | ADMIN+ | - |
| | GET | /api/admin/fund-log | 资金流水列表 | ADMIN+ | - |
| | GET | /api/admin/system-config | 系统配置列表 | ADMIN+ | - |
| | PUT | /api/admin/system-config | 批量更新系统配置 | ADMIN+ | - |
| | GET | /api/admin/alipay-status | 支付宝配置状态 | ADMIN+ | - |
| | GET | /api/admin/email-status | 邮件服务配置状态 | ADMIN+ | - |
| | GET | /api/admin/export/users | 导出用户CSV | ADMIN+ | - |
| | GET | /api/admin/export/orders | 导出订单CSV | ADMIN+ | - |
| **AI助手** | GET | /api/ai/chat/stream | SSE流式对话 | 是 | @RateLimit |
| | POST | /api/ai/chat | 非流式对话 | 是 | @RateLimit |
| | GET | /api/ai/config | AI配置状态 | ADMIN+ | - |
| | PUT | /api/ai/config | 修改AI配置 | ADMIN+ | - |
| | GET | /api/ai/history/{sessionId} | 会话历史 | 是 | - |
| | GET | /api/ai/sessions | 用户会话列表 | 是 | - |
| | DELETE | /api/ai/session/{sessionId} | 删除会话 | 是 | - |

### 统一返回结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 统一分页请求

```json
{ "pageNum": 1, "pageSize": 10 }
```

### 统一分页返回

```json
{ "list": [], "total": 100 }
```

### 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 405 | 请求方法不支持 |
| 415 | 不支持的媒体类型 |
| 423 | 账号已锁定 |
| 429 | 频率过高 |
| 500 | 系统异常 |

---

## 7. Redis Key文档

### Key命名规范

| Key | 说明 | TTL | 代码常量 |
|-----|------|-----|----------|
| `token:user:{id}` | 用户AccessToken | 7200s | `RedisConstant.TOKEN_PREFIX` |
| `refresh:user:{id}` | 刷新Token | 604800s | `RedisConstant.REFRESH_PREFIX` |
| `captcha:{uuid}` | 验证码 | 300s | `RedisConstant.CAPTCHA_PREFIX` |
| `captcha:reset:{username}` | 重置密码验证码(邮箱) | 300s | `RedisConstant.CAPTCHA_PREFIX` |
| `goods:detail:{id}` | 商品详情缓存 | 1800s | `RedisConstant.GOODS_DETAIL_PREFIX` |
| `goods:list:{page}` | 商品列表缓存 | 600s | `RedisConstant.GOODS_LIST_PREFIX` |
| `goods:hot` | 热门商品缓存 | 600s | `RedisConstant.GOODS_HOT_KEY` |
| `goods:recommend` | 推荐商品缓存 | 600s | `RedisConstant.GOODS_RECOMMEND_KEY` |
| `chat:recent:{userId}` | 最近聊天缓存 | 3600s | `RedisConstant.CHAT_RECENT_PREFIX` |
| `notify:user:{id}:unread` | 未读通知计数 | 300s | `RedisConstant.NOTIFY_USER_PREFIX` |
| `permissions:user:{id}` | 用户权限列表 | 7200s | `RedisConstant.PERMISSIONS_PREFIX` |
| `lock:goods:{id}` | 商品缓存互斥锁 | 10s | `RedisConstant.LOCK_GOODS_PREFIX` |
| `repeat:{userId}:{apiMd5}` | 防重复提交 | 5s | `RedisConstant.REPEAT_PREFIX` |
| `rate_limit:{ip}` | 接口限流 | 60s | `RedisConstant.RATE_LIMIT_PREFIX` |
| `blacklist:token:{token}` | Token黑名单 | 7200s | `RedisConstant.BLACKLIST_PREFIX` |
| `login:fail:{username}` | 登录失败计数 | 1800s | `AccountLockConstant.LOGIN_FAIL_PREFIX` |
| `register:limit:{ip}` | 注册频率限制 | 3600s | `AccountLockConstant.REGISTER_LIMIT_PREFIX` |
| `mq:consumed:{queue}:{id}` | MQ幂等消费标记 | 86400s | Consumer内构建 |
| `lock:pay:notify:{orderId}` | 支付通知处理分布式锁 | 30s | OrderServiceImpl |
| `lock:refund:{orderId}` | 退款处理分布式锁 | 30s | OrderServiceImpl |
| `order:pay:timeout:{orderId}` | 订单支付超时标记 | 300s | OrderServiceImpl |
| `ai:session:{userId}:{sessionId}` | AI会话历史(List) | 604800s(7天) | SessionService |
| `ai:rate:{userId}` | AI限流计数 | 60s | AiRateLimiter |
| `ai:config:api-key` | AI API Key(热更新) | - | DeepSeekClient |
| `ai:config:model` | AI模型(热更新) | - | DeepSeekClient |
| `ai:config:base-url` | AI Base URL(热更新) | - | DeepSeekClient |

### 缓存防护

| 防护类型 | 实现方式 | 应用场景 |
|----------|----------|----------|
| 缓存穿透 | NULL值缓存(60s) | 商品详情查询 |
| 缓存击穿 | Redis互斥锁(10s) | 商品详情/热门/推荐 |
| 缓存雪崩 | 不同Key的TTL添加随机偏移 | 全局 |

---

## 8. MQ文档

### 交换机

| 交换机 | 类型 | 说明 |
|--------|------|------|
| campus.order.direct | Direct | 订单交换机 |
| campus.chat.direct | Direct | 聊天交换机 |
| campus.audit.direct | Direct | 审核交换机 |
| campus.notify.direct | Direct | 通知交换机 |
| campus.log.direct | Direct | 日志交换机 |
| campus.dlx.direct | Direct | 死信交换机 |

### 队列与绑定

| 队列 | 交换机 | RoutingKey | 死信交换机 | 消费者 |
|------|--------|------------|------------|--------|
| order.create.queue | campus.order.direct | order.create | campus.dlx.direct | OrderCreateConsumer |
| chat.save.queue | campus.chat.direct | chat.save | campus.dlx.direct | ChatMessageConsumer |
| audit.report.queue | campus.audit.direct | audit.report | campus.dlx.direct | AuditReportConsumer |
| notify.send.queue | campus.notify.direct | notify.send | campus.dlx.direct | NotifySendConsumer |
| log.record.queue | campus.log.direct | log.record | campus.dlx.direct | LogRecordConsumer |
| campus.dlx.queue | campus.dlx.direct | campus.dlx | - | DeadLetterConsumer |

### 保障机制

| 机制 | 实现方式 |
|------|----------|
| 消息确认 | 手动ACK (`channel.basicAck`) |
| 失败处理 | `basicNack(deliveryTag, false, false)` → 进入死信队列 |
| 死信兜底 | DeadLetterConsumer 接收并记录告警日志 |
| 幂等消费 | Redis `SET NX` + 24h TTL 去重标记 |
| 重试机制 | Spring AMQP retry: 3次, 间隔1s |

---

## 9. 权限文档

### RBAC模型

```
用户(t_user) ──N:N── 用户角色(t_user_role) ──N:N── 角色(t_role)
                                                    │
                                              角色权限(t_role_permission)
                                                    │
                                              权限(t_permission)
```

### 角色

| 角色 | 编码 | 说明 |
|------|------|------|
| 超级管理员 | ROLE_SUPER_ADMIN | 全部权限 |
| 管理员 | ROLE_ADMIN | 商品审核+用户管理+举报+日志 |
| 普通用户 | ROLE_USER | 基础操作 |

### 权限码

| 权限 | 编码 | 类型 |
|------|------|------|
| 商品管理 | goods:manage | 菜单 |
| 商品创建 | goods:create | 按钮 |
| 商品修改 | goods:update | 按钮 |
| 商品删除 | goods:delete | 按钮 |
| 商品审核 | goods:audit | 按钮 |
| 用户管理 | user:manage | 菜单 |
| 用户封禁 | user:ban | 按钮 |
| 举报管理 | report:manage | 菜单 |
| 举报审核 | report:review | 按钮 |
| 日志管理 | log:manage | 菜单 |
| 日志查看 | log:view | 按钮 |

### 安全白名单

以下路径无需认证即可访问：

| 路径 | 说明 |
|------|------|
| /api/auth/login | 登录 |
| /api/auth/register | 注册 |
| /api/auth/refresh | 刷新Token |
| /api/auth/send-code | 发送重置密码验证码 |
| /api/auth/reset-password | 重置密码 |
| /api/auth/captcha | 验证码 |
| GET /api/goods/** | 商品浏览 |
| GET /api/goods-category/**, GET /api/category/** | 分类浏览 |
| GET /api/banner/** | 横幅浏览 |
| GET /api/announcement/** | 公告浏览 |
| GET /api/rating/seller/** | 卖家评分 |
| POST /api/order/pay/notify | 支付宝异步通知 |
| /uploads/** | 上传文件访问 |
| /ws/** | WebSocket端点 |
| /doc.html, /webjars/**, /swagger-resources/** | API文档(仅开发) |
| /actuator/health | 健康检查 |

### 账号安全

| 机制 | 配置 |
|------|------|
| 登录失败锁定 | 5次失败后锁定30分钟 |
| 登录限流 | 5次/分钟/IP |
| 注册限流 | 3次/小时/IP |
| 密码强度 | 至少8位，含大写/小写/数字/特殊字符中至少3种 |
| Token黑名单 | 退出登录后AccessToken立即失效 |

---

## 10. 日志文档

### 日志框架: Log4j2

| 日志文件 | 说明 | 保留策略 |
|----------|------|----------|
| logs/info.log | 信息日志 | 100MB/文件, 保留30天 |
| logs/error.log | 错误日志 | 100MB/文件, 保留30天 |
| logs/audit.log | 审计日志 | 100MB/文件, 保留30天 |
| logs/security.log | 安全日志 | 100MB/文件, 保留30天 |
| logs/sql.log | SQL日志 | 100MB/文件, 保留30天 |

### 日志格式

```
时间 traceId 级别 类名 - 消息
```

### 操作日志

自动通过AOP切面拦截所有Controller方法，记录：
- userId, username, module, operation, method, url, ip, duration, status, traceId
- 异步通过MQ写入数据库

### 安全日志

记录事件类型：

| 事件 | 编码 |
|------|------|
| 登录成功 | LOGIN_SUCCESS |
| 登录失败 | LOGIN_FAIL |
| 权限拒绝 | ACCESS_DENIED |
| Token失效 | TOKEN_EXPIRED |
| 频率限制 | RATE_LIMIT |
| 恶意输入 | MALICIOUS_INPUT |
| 密码重置 | PASSWORD_RESET |

---

## 11. 环境配置详解

### 11.1 配置文件结构

| 文件 | 环境 | 说明 |
|------|------|------|
| `application.yml` | 开发(dev) | 默认配置，敏感信息可明文(仅本地) |
| `application-prod.yml` | 生产(prod) | 所有敏感信息通过环境变量注入 |
| `.env.example` | Docker | 环境变量模板 |
| `.env` | Docker | 实际环境变量(不提交到Git) |

### 11.2 环境变量清单

#### MySQL

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_URL` | jdbc:mysql://localhost:3306/campus_trade?...&createDatabaseIfNotExist=true | 数据库连接URL(含自动建库) |
| `MYSQL_USERNAME` | root | 数据库用户名 |
| `MYSQL_ROOT_PASSWORD` | - | **[必改]** 数据库密码 |
| `MYSQL_DATABASE` | campus_trade | 数据库名 |

#### Redis

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `REDIS_HOST` | localhost | Redis主机 |
| `REDIS_PORT` | 6379 | Redis端口 |
| `REDIS_PASSWORD` | (空) | **[必改]** Redis密码 |

#### RabbitMQ

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `RABBITMQ_HOST` | localhost | MQ主机 |
| `RABBITMQ_USERNAME` | guest | **[必改]** MQ用户名 |
| `RABBITMQ_PASSWORD` | guest | **[必改]** MQ密码 |

#### JWT

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `JWT_SECRET` | (硬编码) | **[必改]** JWT签名密钥，至少32字符 |
| `JWT_ACCESS_EXPIRATION` | 7200000 | AccessToken过期时间(ms) |
| `JWT_REFRESH_EXPIRATION` | 604800000 | RefreshToken过期时间(ms) |

#### CORS

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `CORS_ALLOWED_ORIGINS` | http://localhost:5173,http://localhost:5174 | 允许的前端域名 |

#### 文件上传

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `FILE_UPLOAD_PATH` | /data/uploads | 上传文件存储路径 |
| `FILE_UPLOAD_URL_PREFIX` | /uploads | 文件访问URL前缀 |

#### 应用

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `SPRING_PROFILES_ACTIVE` | prod | Spring Profile |
| `JAVA_OPTS` | -Xms512m -Xmx1024m -XX:+UseG1GC | JVM参数 |
| `SERVER_PORT` | 8080 | 后端服务端口 |
| `ADMIN_PASSWORD` | Admin123!@ | **[必改]** 超级管理员初始密码 |
| `FRONTEND_USER_PORT` | 80 | 用户端Nginx端口 |
| `FRONTEND_ADMIN_PORT` | 81 | 管理端Nginx端口 |

#### AI助手

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `AI_API_KEY` | sk-nmcq... | **[必改]** DeepSeek API密钥 |
| `AI_BASE_URL` | https://api.siliconflow.cn/v1 | DeepSeek API基础URL |
| `AI_MODEL` | deepseek-ai/DeepSeek-V4-Flash | 模型名称 |
| `AI_MAX_TOKENS` | 2048 | 最大输出token数 |
| `AI_TEMPERATURE` | 0.7 | 温度参数 |
| `AI_CONTEXT_WINDOW` | 20 | 上下文消息条数 |
| `AI_RATE_LIMIT` | 20 | 每分钟请求限制 |

### 11.3 开发环境配置

开发环境使用 `application.yml`，关键配置：

```yaml
# 数据库 - 本地MySQL
spring.datasource.url: jdbc:mysql://localhost:3306/campus_trade
spring.datasource.username: root
spring.datasource.password: root123

# Redis - 本地无密码
spring.redis.host: localhost
spring.redis.password:

# RabbitMQ - 默认guest
spring.rabbitmq.username: guest
spring.rabbitmq.password: guest

# JWT - 开发用密钥
jwt.secret: CampusTradeSecretKey2026ForJwtTokenGenerationAndValidation

# Knife4j - 开发环境启用
knife4j.enable: true
```

### 11.4 生产环境配置

生产环境使用 `application-prod.yml`，所有敏感信息通过环境变量注入：

```yaml
# 数据库 - 环境变量 + SSL + 时区
spring.datasource.url: ${MYSQL_URL}
spring.datasource.password: ${MYSQL_PASSWORD}
spring.datasource.hikari.connection-init-sql: SET time_zone = '+08:00'

# Redis - 环境变量 + 密码
spring.redis.password: ${REDIS_PASSWORD}

# RabbitMQ - 环境变量
spring.rabbitmq.password: ${RABBITMQ_PASSWORD}

# JWT - 环境变量（保留默认值兜底）
jwt.secret: ${JWT_SECRET:CampusTradeSecretKey2026ForJwtTokenGenerationAndValidation}

# Knife4j - 生产环境关闭
knife4j.enable: false

# 优雅停机
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s

# Actuator - 仅开放必要端点
management.endpoints.web.exposure.include: health,info,metrics
```

> **支付宝配置**: 不在 yml 中配置，通过管理端"系统配置"页面设置，存储在 `t_system_config` 表中，私钥/公钥 AES 加密。

> **邮件服务配置**: 同样通过管理端"系统配置"页面设置，存储在 `t_system_config` 表中，授权码 AES 加密。配置项包括：`mail.host`(smtp.qq.com)、`mail.port`(465)、`mail.username`(发件人邮箱)、`mail.password`(授权码)、`mail.from`(发件人名称)、`mail.ssl`(true)。保存后热更新生效，无需重启。

### 11.5 HikariCP 连接池配置对比

| 参数 | 开发 | 生产 |
|------|------|------|
| minimum-idle | 5 | 10 |
| maximum-pool-size | 20 | 50 |
| idle-timeout | 600000 | 600000 |
| max-lifetime | 1800000 | 1800000 |
| leak-detection-threshold | - | 60000 |

### 11.6 Redis Lettuce 连接池配置对比

| 参数 | 开发 | 生产 |
|------|------|------|
| max-active | 20 | 50 |
| max-idle | 10 | 20 |
| min-idle | 5 | 10 |

---

## 12. Docker部署文档

### 12.1 环境要求

| 依赖 | 最低版本 | 推荐版本 |
|------|----------|----------|
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |
| Maven | 3.8+ | 3.9+ |
| Node.js | 16+ | 18+ |
| JDK | 11 | 11 |

### 12.2 一键部署

```bash
# 1. 克隆项目
git clone <repo-url> && cd CampusTrade

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，修改所有 [必改] 变量
vi .env

# 3. 构建后端
cd campus-trade-server
mvn clean package -DskipTests
cd ..

# 4. 构建前端
cd campus-trade-user
npm install && npm run build
cd ..

cd campus-trade-admin
npm install && npm run build
cd ..

# 5. 启动所有服务
docker-compose up -d

# 6. 查看状态
docker-compose ps

# 7. 查看日志
docker-compose logs -f backend
```

> **注意**: 无需手动创建数据库，JDBC URL 含 `createDatabaseIfNotExist=true`，DataInitializer 会自动建表和初始化数据。

### 12.3 服务端口

| 服务 | 容器内端口 | 宿主机端口 | 说明 |
|------|------------|------------|------|
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| RabbitMQ | 5672 | 5672 | AMQP协议 |
| RabbitMQ Management | 15672 | 15672 | 管理界面 |
| Backend | 8080 | 8080 | 后端API |
| Frontend-User | 80 | 80 | 用户端 |
| Frontend-Admin | 80 | 81 | 管理端 |

### 12.4 持久化挂载

| 容器路径 | 宿主机路径 | 说明 |
|----------|------------|------|
| /var/lib/mysql | /app/mysql | 数据库数据 |
| /app/logs | /app/logs | 应用日志 |
| /data/uploads | /app/upload | 上传文件 |

### 12.5 MySQL 关键配置

Docker Compose 中 MySQL 的关键启动参数：

```yaml
command: >
  --default-time-zone='+08:00'
  --character-set-client-handshake=FALSE
  --init-connect='SET NAMES utf8mb4'
  --character-set-server=utf8mb4
  --collation-server=utf8mb4_general_ci
```

- `--default-time-zone='+08:00'`: 设置服务器时区为东八区，避免时间偏移
- `--character-set-client-handshake=FALSE` + `--init-connect`: 强制客户端使用 utf8mb4
- HikariCP 连接池额外配置 `connection-init-sql: SET time_zone = '+08:00'` 确保连接时区正确

### 12.6 Nginx 代理配置要点

用户端 Nginx (`user.conf`) 关键代理规则：

| 路径 | 代理目标 | 说明 |
|------|----------|------|
| `/` | SPA fallback | `try_files $uri $uri/ /index.html` |
| `/api/` | `http://backend:8080/api/` | 后端API代理，需设置 `proxy_set_header Origin ""` 清空Origin头避免CORS 403 |
| `/ws` | `http://backend:8080/ws` | WebSocket代理，需设置 Upgrade 头，超时3600s |
| `/uploads/` | `http://backend:8080/uploads/` | 上传文件代理 |

> **重要**: Nginx 代理 `/api/` 时必须设置 `proxy_set_header Origin ""` 清空 Origin 头，否则后端 CORS 校验会因 Origin 不匹配返回 403。

### 12.7 服务依赖与启动顺序

```
MySQL (健康检查通过)
  └→ RabbitMQ (健康检查通过)
       └→ Redis (健康检查通过)
            └→ Backend (健康检查通过)
                 ├→ Frontend-User
                 └→ Frontend-Admin
```

### 12.8 健康检查

| 服务 | 检查方式 | 间隔 | 超时 | 重试 |
|------|----------|------|------|------|
| MySQL | `mysqladmin ping` | 10s | 5s | 5 |
| Redis | `redis-cli ping` | 10s | 5s | 5 |
| RabbitMQ | `rabbitmq-diagnostics check_running` | 30s | 10s | 5 |
| Backend | `curl /actuator/health` | 30s | 5s | 3 |

### 12.9 常用运维命令

```bash
# 停止所有服务
docker-compose down

# 重启单个服务
docker-compose restart backend

# 查看MySQL
docker exec -it campus-trade-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} campus_trade

# 查看Redis
docker exec -it campus-trade-redis redis-cli -a ${REDIS_PASSWORD}

# 查看RabbitMQ管理界面
# 浏览器访问 http://localhost:15672

# 查看后端日志
docker-compose logs -f --tail=100 backend

# 重新构建并启动后端
cd campus-trade-server && mvn clean package -DskipTests && cd ..
docker-compose up -d --build backend

# 清理所有容器和数据卷（危险！会删除数据）
docker-compose down -v
```

---

## 13. 安全注意事项

### 13.1 生产环境必须修改的配置

| 配置项 | 风险等级 | 说明 |
|--------|----------|------|
| `JWT_SECRET` | **严重** | 默认硬编码密钥，必须替换为随机强密钥 |
| `MYSQL_ROOT_PASSWORD` | **严重** | 默认 root123，必须替换 |
| `REDIS_PASSWORD` | **严重** | 默认为空，必须设置密码 |
| `RABBITMQ_USERNAME/PASSWORD` | **高** | 默认 guest/guest，必须替换 |
| `CORS_ALLOWED_ORIGINS` | **高** | 生产环境必须限制为实际域名 |

### 13.2 JWT密钥生成方法

```bash
# 方法1: OpenSSL
openssl rand -base64 48

# 方法2: Java
java -e 'System.out.println(java.util.UUID.randomUUID().toString().replace("-","")+java.util.UUID.randomUUID().toString().replace("-",""))'

# 方法3: Python
python3 -c "import secrets; print(secrets.token_urlsafe(48))"
```

### 13.3 安全机制清单

| 机制 | 实现位置 | 说明 |
|------|----------|------|
| XSS过滤 | `XssFilter.java` | 7种XSS正则 + 4种SQL注入正则 |
| SQL注入防御 | `XssFilter.java` | 参数/请求体/请求头全过滤 |
| 敏感词过滤 | `SensitiveWordUtil.java` | 14个敏感词库 |
| 安全响应头 | `SecurityHeaderFilter.java` | 8个安全头(X-Content-Type-Options/X-Frame-Options/X-XSS-Protection/Referrer-Policy/CSP/HSTS/Cache-Control/Pragma) |
| Token黑名单 | `JwtFilter.java` + Redis | 退出登录后Token立即失效 |
| 登录限流 | `AuthServiceImpl.java` + Redis | 5次/分钟/IP |
| 注册限流 | `AuthServiceImpl.java` + Redis | 3次/小时/IP |
| 账号锁定 | `AuthServiceImpl.java` + Redis | 5次失败锁定30分钟 |
| 密码强度 | `PasswordUtil.java` | 至少8位+3种字符类型 |
| 接口限流 | `@RateLimit` + `RateLimitAspect` | 60次/分钟/IP |
| 防重复提交 | `@RepeatSubmit` + `RepeatSubmitAspect` | 5秒窗口 |
| 文件上传校验 | `FileUploadUtil.java` | 扩展名+MIME+大小+路径遍历 |
| RBAC权限 | `@PreAuthorize` | 方法级权限控制 |
| CORS限制 | `CorsConfig.java` | 可配置allowed-origins |
| 支付通知验签 | `OrderServiceImpl.handlePayNotify()` | 支付宝RSA2签名验证 |
| 支付分布式锁 | Redis `lock:pay:notify:{orderId}` | 防止并发重复处理 |
| 支付配置加密 | `SystemConfigServiceImpl` | 私钥/公钥AES加密存储 |
| 资金流水保护 | `safeInsertFundLog()` | 写入异常不影响主流程 |
| AI Prompt Injection检测 | `AiSafetyService.checkInput()` | 中英文双语模式匹配(12种中文+多种英文)，预编译Pattern |
| AI敏感值脱敏 | `AiSafetyService.sanitizeOutput()` | 正则匹配实际敏感值，只脱敏值部分 |
| AI管理员工具过滤 | `AiToolService.getToolDefinitions()` | 按角色动态过滤工具定义 |
| AI配置修改鉴权 | `AiController.updateAiConfig()` | 仅ADMIN+可修改AI配置 |
| AI限流 | `AiRateLimiter` + Lua脚本 | 原子化INCR+EXPIRE，每用户20次/分钟 |
| AI SSE超时保护 | `SseEmitter(300s)` | Agent Loop最多3轮×60s=180s，预留缓冲 |

### 13.4 生产环境检查清单

- [ ] 所有 `.env` 中 `[必改]` 变量已修改
- [ ] JWT_SECRET 已替换为随机强密钥(≥32字符)
- [ ] MySQL/Redis/RabbitMQ 密码已设置
- [ ] CORS_ALLOWED_ORIGINS 已限制为实际域名
- [ ] Knife4j 已关闭 (`knife4j.enable=false`)
- [ ] Actuator 仅开放必要端点
- [ ] SSL/TLS 已配置(Nginx层面)
- [ ] 防火墙已配置(仅开放80/443/8080)
- [ ] 数据库端口(3306)不对外暴露
- [ ] Redis端口(6379)不对外暴露
- [ ] RabbitMQ端口(5672/15672)不对外暴露
- [ ] 日志文件定期轮转和清理
- [ ] 数据库定期备份
- [ ] AI_API_KEY 已替换为有效的DeepSeek API密钥
- [ ] AI限流配置合理（默认20次/分钟）
- [ ] 邮件服务已配置（QQ邮箱SMTP+授权码），重置密码功能可用
- [ ] 支付宝沙箱参数已配置（或确认使用模拟支付模式）

---

## 14. 测试报告

### 单元测试

| 测试类 | 测试项 | 数量 |
|--------|--------|------|
| CampusTradeApplicationTests | 应用上下文加载 | 1 |
| ResultTest | Result封装/ResultCode枚举 | 5 |
| SensitiveWordUtilTest | 敏感词检测/过滤 | 2 |
| SnowflakeIdUtilTest | 雪花算法唯一性/递增/边界 | 5 |
| PasswordUtilTest | 密码强度校验 | 4 |
| FileUploadUtilTest | 文件扩展名/MIME/大小/路径 | 5 |
| PageResultTest | 分页结果封装 | 2 |
| BusinessExceptionTest | 业务异常构造 | 3 |

### 运行测试

```bash
cd campus-trade-server
mvn test
```

---

## 15. 性能优化报告

### 数据库层

| 优化项 | 实现方式 |
|--------|----------|
| 索引优化 | 26张表, 30+索引, 唯一索引+联合索引 |
| 逻辑删除 | deleted字段，避免物理删除开销 |
| 乐观锁 | version字段，防止并发冲突 |
| 分页查询 | LIMIT + offset |
| 批量查询 | `selectByIds` 替代循环单查(N+1修复) |
| N+1查询优化 | GoodsMapper JOIN查询(selectListVO/selectHotGoodsVO/selectRecommendGoodsVO) |
| N+1查询优化 | GoodsFavoriteMapper JOIN查询(selectFavoriteGoodsVOByUserId) |
| N+1查询优化 | ChatMessageMapper selectUnreadCountGrouped批量查询(AI助手最近会话) |
| 软删除表无UNIQUE KEY | INSERT失败时catch+restore，避免唯一约束冲突 |

### 缓存层

| 优化项 | 实现方式 |
|--------|----------|
| 商品详情缓存 | 1800s TTL + NULL值缓存(60s) |
| 热门/推荐商品缓存 | 600s TTL |
| 未读通知计数缓存 | 300s TTL |
| Token缓存 | 7200s TTL |
| 防重复提交 | 5s TTL |
| 接口限流 | 60s TTL |
| 缓存穿透防护 | NULL值缓存 |
| 缓存击穿防护 | Redis互斥锁(10s) |

### MQ层

| 优化项 | 实现方式 |
|--------|----------|
| 聊天消息异步持久化 | 先写Redis，MQ异步写MySQL |
| 操作日志异步记录 | AOP切面 + MQ |
| 通知异步发送 | MQ解耦 |
| 幂等消费 | Redis SET NX去重 |
| 死信兜底 | 失败消息进入死信队列 |

### 接口层

| 优化项 | 实现方式 |
|--------|----------|
| 登录限流 | Redis实现, 5次/分钟/IP |
| 防重复提交 | Redis SET NX, 5s窗口 |
| AOP操作日志 | 自动拦截Controller |

### 安全层

| 优化项 | 实现方式 |
|--------|----------|
| XSS过滤 | 正则匹配7种XSS模式 |
| SQL注入防御 | 正则匹配4种注入模式 |
| 敏感词过滤 | 14个敏感词库 |
| Token黑名单 | 退出/踢出立即失效 |

### AI助手层

| 优化项 | 实现方式 |
|--------|----------|
| Function Calling工具缓存 | 静态缓存+双检锁，避免重复构建工具定义 |
| N+1查询优化 | selectUnreadCountGrouped批量查询最近会话未读数 |
| 限流原子化 | Lua脚本INCR+EXPIRE原子执行，避免竞态条件 |
| 会话压缩原子化 | Redis pipeline del+rpush+expire原子执行 |
| API配置持久化 | Redis持久化API Key/Model/BaseURL，重启不丢失 |
| SSE超时优化 | 300s超时匹配Agent Loop最大3轮×60s |
| 流式输出优化 | 非Agent模式直接流式，Agent模式模拟流式(3字符+30ms延迟) |
| 工具定义静态缓存 | 双检锁缓存工具定义JSON，避免每次请求重建 |
| API重试 | chatWithTools加2次重试，与chat一致 |

### 日志层

| 优化项 | 实现方式 |
|--------|----------|
| Log4j2异步日志 | AsyncLogger |
| 5文件分类 | info/error/audit/security/sql |
| traceId追踪 | 全链路TraceId |

---

## 16. 常见问题排查

### 16.1 后端启动失败

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 连接MySQL失败 | MySQL未启动/密码错误 | 检查MySQL容器状态和密码配置 |
| 连接Redis失败 | Redis未启动/密码不匹配 | 检查Redis容器和密码 |
| 连接RabbitMQ失败 | MQ未启动/用户名密码错误 | 检查MQ容器和凭据 |
| JWT解析失败 | 密钥不匹配 | 确认JWT_SECRET一致 |
| 端口被占用 | 8080端口冲突 | 修改SERVER_PORT或释放端口 |
| Springfox NPE | Spring Boot 2.6+路径匹配策略 | 确认 `spring.mvc.pathmatch.matching-strategy=ant_path_matcher` + `SpringfoxBeanPostProcessor` 兼容类 |
| MySQL中文乱码 | 字符集配置缺失 | 确认 `--character-set-client-handshake=FALSE --init-connect='SET NAMES utf8mb4'` |
| 时间差8小时 | MySQL时区默认UTC | 确认 `--default-time-zone='+08:00'` + HikariCP `connection-init-sql` |

### 16.2 前端问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| API请求404 | Nginx代理配置错误 | 检查nginx.conf中proxy_pass |
| 登录后Token丢失 | localStorage被清空 | 检查浏览器隐私设置 |
| CORS报错(403) | Nginx未清空Origin头 | 确认 `proxy_set_header Origin ""` |
| 图片无法加载 | Nginx缺少/uploads/代理 | 确认nginx.conf中 `/uploads/` location |
| 页面空白 | 前端未构建 | 执行 `npm run build` |
| WebSocket连接失败 | Nginx缺少/ws代理 | 确认nginx.conf中 `/ws` location + Upgrade头 |
| el-tooltip定位偏移 | backdrop-filter创建新堆叠上下文 | 移除 `.el-card` 上的 `backdrop-filter: blur()` |

### 16.3 Docker问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 容器启动后立即退出 | 健康检查失败/配置错误 | `docker-compose logs <service>` |
| MySQL初始化失败 | init.sql执行错误 | 已移除init.sql，DataInitializer自动建表 |
| 后端无法连接MySQL | 网络未就绪 | 确认depends_on和healthcheck |
| 上传文件丢失 | 挂载路径不正确 | 检查volumes配置 |
| 健康检查超时 | 后端启动慢 | 增大 `start_period`（默认60s） |
| Redis空密码报错 | `redis-cli -a ""` 不支持 | 设置实际密码或使用 `redis-cli ping` |

### 16.4 支付问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 支付降级为模拟支付 | 未配置支付宝沙箱参数 | 管理端"系统配置"页面填写支付宝参数 |
| 支付通知不触发 | 外网无法访问/pay/notify | 配置内网穿透或使用支付宝沙箱工具手动触发 |
| 退款失败 | 卖家未配置收款信息 | 卖家需在"收款管理"页面配置支付宝账号 |
| 资金流水缺失 | t_fund_log表异常 | safeInsertFundLog()已保护，检查后端日志 |

### 16.5 邮件服务问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 重置密码提示"邮件服务未配置" | 未在管理端配置邮件服务 | 管理端"系统配置"→"邮件服务配置"填写QQ邮箱和授权码 |
| 验证码邮件未收到 | 授权码错误/邮箱地址错误 | 检查QQ邮箱POP3/SMTP服务是否开启，授权码是否正确 |
| 邮件发送超时 | 网络问题/SMTP端口错误 | 检查端口(465 SSL/587 TLS)，确认服务器可访问smtp.qq.com |
| 用户无邮箱无法重置密码 | 注册时未绑定邮箱 | 建议用户联系管理员协助重置密码 |

### 16.6 商品审核问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 商品提交审核后一直待审核 | AI服务不可用/MQ消息积压 | 检查AI服务状态和RabbitMQ队列 |
| AI审核结果不准确 | AI模型判断误差 | 管理员可在后台"商品审核"页面复审改判 |
| 编辑商品后未重新审核 | 预期行为：编辑已审核商品自动重置为待审核 | 确认商品状态已变为PENDING |

### 16.7 日志排查

```bash
# 后端日志
docker-compose logs -f --tail=200 backend

# MySQL慢查询日志
docker exec -it campus-trade-mysql mysql -uroot -p -e "SHOW VARIABLES LIKE 'slow_query%'"

# Redis连接数
docker exec -it campus-trade-redis redis-cli -a ${REDIS_PASSWORD} info clients

# RabbitMQ队列状态
# 访问 http://localhost:15672 → Queues Tab
```

### 16.8 AI助手问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| AI回复为空 | API Key无效/过期 | 管理端"系统配置"页面修改AI API Key |
| AI回复缓慢 | 网络延迟/模型负载 | 检查网络连通性，稍后重试 |
| SSE连接断开 | 超时(>300s)/网络中断 | 前端自动重试或手动点击重试按钮 |
| 工具调用失败 | 业务接口异常 | 查看后端日志中工具执行错误 |
| 逐字输出丢失 | 前端Markdown渲染问题 | 确认流式过程中用纯文本显示，结束后才渲染Markdown |
| 复制功能失效 | HTTP环境clipboard不可用 | 已降级为document.execCommand('copy') |
| 普通用户看到管理员工具 | 工具过滤未生效 | 确认SecurityUtil.isAdmin()正确判断角色 |
| AI配置修改后重启丢失 | Redis持久化未生效 | 检查Redis连接，确认ai:config:* Key存在 |
| 限流误触发 | Lua脚本计数异常 | 检查ai:rate:{userId} Key，必要时手动删除 |
| Prompt Injection误判 | 安全检测过于严格 | 查看AiSafetyService日志，调整检测模式 |

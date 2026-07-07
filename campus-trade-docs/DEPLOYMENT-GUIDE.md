# CampusTrade 完整部署与配置文档

> 按照 `18-delivery-protocol.md` 输出完整交付文档。本文档涵盖项目结构、数据库、API、缓存、消息队列、权限、日志、部署、配置、安全注意事项等全部内容。

---

## 目录

1. [完整项目树](#1-完整项目树)
2. [完整数据库SQL](#2-完整数据库sql)
3. [完整API文档](#3-完整api文档)
4. [Redis Key文档](#4-redis-key文档)
5. [MQ文档](#5-mq文档)
6. [权限文档](#6-权限文档)
7. [日志文档](#7-日志文档)
8. [环境配置详解](#8-环境配置详解)
9. [Docker部署文档](#9-docker部署文档)
10. [安全注意事项](#10-安全注意事项)
11. [测试报告](#11-测试报告)
12. [性能优化报告](#12-性能优化报告)
13. [常见问题排查](#13-常见问题排查)

---

## 1. 完整项目树

```
CampusTrade/
├── .env.example                           # 环境变量模板
├── docker-compose.yml                     # Docker Compose 编排
│
├── campus-trade-docs/                     # 规范文档层
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
│       │   ├── config/                    # 配置类(7个)
│       │   │   ├── CorsConfig.java        # 跨域配置(可配置allowed-origins)
│       │   │   ├── FilterConfig.java      # XSS/安全头过滤器注册
│       │   │   ├── Knife4jConfig.java     # API文档配置
│       │   │   ├── RabbitMQConfig.java    # MQ配置(5交换机+5队列+死信)
│       │   │   ├── RedisConfig.java       # Redis序列化配置
│       │   │   ├── SecurityConfig.java    # Spring Security配置
│       │   │   └── WebMvcConfig.java      # 静态资源映射(上传文件)
│       │   ├── constant/                  # 常量类(4个)
│       │   │   ├── AccountLockConstant.java  # 账号锁定常量
│       │   │   ├── MQConstant.java        # MQ交换机/队列/路由键
│       │   │   ├── RedisConstant.java     # Redis Key前缀+TTL
│       │   │   └── SecurityConstant.java  # 角色/白名单/限流
│       │   ├── controller/                # 10个Controller
│       │   ├── dto/                       # 13个DTO
│       │   ├── entity/                    # 15个Entity
│       │   ├── enum_/                     # 6个枚举
│       │   ├── exception/                 # 全局异常处理(10种异常)
│       │   ├── mapper/                    # 15个Mapper接口
│       │   ├── mq/                        # 6个Consumer(含死信)
│       │   ├── security/                  # JWT/认证/过滤器
│       │   ├── service/                   # 10个Service接口
│       │   ├── service/impl/              # 10个ServiceImpl
│       │   ├── util/                      # 工具类(8个)
│       │   └── vo/                        # 11个VO
│       ├── main/resources/
│       │   ├── application.yml            # 开发环境配置
│       │   ├── application-prod.yml       # 生产环境配置(环境变量注入)
│       │   ├── log4j2.xml                 # 5文件日志配置
│       │   ├── mapper/                    # 15个Mapper XML
│       │   └── sql/init.sql               # 完整建表+初始化数据
│       └── test/java/com/campustrade/     # 8个单元测试类
│
├── campus-trade-user/                     # 用户端前端 Vue3
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/                           # 9个API模块(auth/user/goods/order/chat/report/notification/category/file)
│       ├── layouts/MainLayout.vue
│       ├── pages/                         # 11个页面
│       ├── router/index.ts
│       ├── stores/user.ts                 # Pinia状态管理
│       ├── styles/index.scss
│       └── utils/request.ts               # Axios封装(含Token刷新)
│
├── campus-trade-admin/                    # 管理端前端 Vue3
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/admin.ts
│       ├── layouts/AdminLayout.vue
│       ├── pages/                         # 7个页面
│       ├── router/index.ts
│       ├── stores/admin.ts                # Pinia状态管理
│       └── utils/request.ts               # Axios封装(含Token刷新)
│
└── nginx/
    ├── user.conf                          # 用户端Nginx配置
    └── admin.conf                         # 管理端Nginx配置
```

---

## 2. 完整数据库SQL

数据库文件: `campus-trade-server/src/main/resources/sql/init.sql`

### 基本信息

| 项 | 值 |
|----|-----|
| 数据库名 | `campus_trade` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_general_ci` |
| 引擎 | InnoDB |

### 表清单(15张)

| 表名 | 说明 | 公共字段 |
|------|------|----------|
| t_user | 用户表 | id, create_time, update_time, deleted, version |
| t_role | 角色表 | 同上 |
| t_permission | 权限表 | 同上 |
| t_user_role | 用户角色关联表 | 同上 |
| t_role_permission | 角色权限关联表 | 同上 |
| t_goods | 商品表 | 同上 |
| t_goods_category | 商品分类表 | 同上 |
| t_goods_favorite | 商品收藏表 | 同上 |
| t_order | 订单表 | 同上 |
| t_order_item | 订单明细表 | 同上 |
| t_chat_message | 聊天消息表 | 同上 |
| t_report | 举报表 | 同上 |
| t_notification | 通知表 | 同上 |
| t_operation_log | 操作日志表 | 同上 |
| t_security_log | 安全日志表 | 同上 |

### 索引规范

**唯一索引**: username, phone, email, role_code, permission_code, order_no

**联合索引**: (user_id, status), (goods_id, status), (sender_id, receiver_id)

### 初始化数据

- 3个角色: ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_USER
- 11个权限: goods:create/update/delete/audit, user:ban, report:review, log:view 等
- 6个商品分类: 数码电子, 书籍教材, 生活用品, 服装鞋帽, 运动户外, 其他

---

## 3. 完整API文档

启动后访问: `http://localhost:8080/doc.html` (Knife4j，仅开发环境启用)

### API清单

| 模块 | 方法 | 路径 | 说明 | 鉴权 | 限流 |
|------|------|------|------|------|------|
| **认证** | POST | /api/auth/register | 注册 | 否 | @RateLimit |
| | POST | /api/auth/login | 登录 | 否 | @RateLimit |
| | POST | /api/auth/logout | 退出 | 是 | - |
| | POST | /api/auth/refresh | 刷新Token | 否 | - |
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
| | PUT | /api/goods/{id}/submit | 提交审核 | 是 | - |
| | PUT | /api/goods/{id}/online | 上架 | 是 | - |
| | PUT | /api/goods/{id}/offline | 下架 | 是 | - |
| | POST | /api/goods/{id}/favorite | 收藏 | 是 | - |
| | DELETE | /api/goods/{id}/favorite | 取消收藏 | 是 | - |
| **分类** | GET | /api/goods-category | 分类列表 | 否 | - |
| **订单** | POST | /api/order | 创建订单 | 是 | - |
| | PUT | /api/order/{id}/cancel | 取消订单 | 是 | - |
| | PUT | /api/order/{id}/pay | 支付 | 是 | - |
| | PUT | /api/order/{id}/ship | 发货 | 是 | - |
| | PUT | /api/order/{id}/finish | 确认收货 | 是 | - |
| | PUT | /api/order/{id}/refund | 退款 | 是 | - |
| | GET | /api/order/{id} | 订单详情 | 是 | - |
| | GET | /api/order/buyer | 买家订单 | 是 | - |
| | GET | /api/order/seller | 卖家订单 | 是 | - |
| **聊天** | POST | /api/chat | 发送消息 | 是 | - |
| | GET | /api/chat/history/{targetUserId} | 聊天记录 | 是 | - |
| | GET | /api/chat/recent | 最近会话 | 是 | - |
| | GET | /api/chat/unread/{senderId} | 未读数量 | 是 | - |
| | PUT | /api/chat/read/{senderId} | 标记已读 | 是 | - |
| **举报** | POST | /api/report | 提交举报 | 是 | - |
| | GET | /api/report/mine | 我的举报 | 是 | - |
| | PUT | /api/report/{id}/handle | 处理举报 | ADMIN+ | - |
| **通知** | GET | /api/notification | 通知列表 | 是 | - |
| | GET | /api/notification/unread-count | 未读数量 | 是 | - |
| | PUT | /api/notification/{id}/read | 标记已读 | 是 | - |
| | PUT | /api/notification/read-all | 全部已读 | 是 | - |
| | DELETE | /api/notification/{id} | 删除通知 | 是 | - |
| **文件** | POST | /api/file/upload | 上传图片 | 是 | @RateLimit |
| | DELETE | /api/file/delete | 删除图片 | 是 | - |
| **管理** | GET | /api/admin/dashboard/stats | 仪表盘统计 | ADMIN+ | - |
| | GET | /api/admin/user | 用户列表 | ADMIN+ | - |
| | PUT | /api/admin/user/{id}/ban | 封禁用户 | ADMIN+ | - |
| | PUT | /api/admin/user/{id}/unban | 解封用户 | ADMIN+ | - |
| | GET | /api/admin/goods | 商品审核列表 | ADMIN+ | - |
| | PUT | /api/admin/goods/{id}/audit | 审核商品 | ADMIN+ | - |
| | GET | /api/admin/order | 订单管理列表 | ADMIN+ | - |
| | GET | /api/admin/report | 举报管理列表 | ADMIN+ | - |
| | PUT | /api/admin/report/{id}/resolve | 举报通过 | ADMIN+ | - |
| | PUT | /api/admin/report/{id}/dismiss | 举报驳回 | ADMIN+ | - |
| | GET | /api/admin/log/operation | 操作日志 | ADMIN+ | - |
| | GET | /api/admin/log/security | 安全日志 | ADMIN+ | - |

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

## 4. Redis Key文档

### Key命名规范

| Key | 说明 | TTL | 代码常量 |
|-----|------|-----|----------|
| `token:user:{id}` | 用户AccessToken | 7200s | `RedisConstant.TOKEN_PREFIX` |
| `refresh:user:{id}` | 刷新Token | 604800s | `RedisConstant.REFRESH_PREFIX` |
| `captcha:{uuid}` | 验证码 | 300s | `RedisConstant.CAPTCHA_PREFIX` |
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

### 缓存防护

| 防护类型 | 实现方式 | 应用场景 |
|----------|----------|----------|
| 缓存穿透 | NULL值缓存(60s) | 商品详情查询 |
| 缓存击穿 | Redis互斥锁(10s) | 商品详情/热门/推荐 |
| 缓存雪崩 | 不同Key的TTL添加随机偏移 | 全局 |

---

## 5. MQ文档

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

## 6. 权限文档

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
| /api/auth/captcha | 验证码 |
| GET /api/goods/** | 商品浏览 |
| GET /api/goods-category/** | 分类浏览 |
| /uploads/** | 上传文件访问 |
| /doc.html, /webjars/**, /swagger-resources/** | API文档(仅开发) |

### 账号安全

| 机制 | 配置 |
|------|------|
| 登录失败锁定 | 5次失败后锁定30分钟 |
| 登录限流 | 5次/分钟/IP |
| 注册限流 | 3次/小时/IP |
| 密码强度 | 至少8位，含大写/小写/数字/特殊字符中至少3种 |
| Token黑名单 | 退出登录后AccessToken立即失效 |

---

## 7. 日志文档

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
| 登录失败 | LOGIN_FAIL |
| 权限拒绝 | ACCESS_DENIED |
| Token失效 | TOKEN_EXPIRED |
| 频率限制 | RATE_LIMIT |
| 恶意输入 | MALICIOUS_INPUT |

---

## 8. 环境配置详解

### 8.1 配置文件结构

| 文件 | 环境 | 说明 |
|------|------|------|
| `application.yml` | 开发(dev) | 默认配置，敏感信息可明文(仅本地) |
| `application-prod.yml` | 生产(prod) | 所有敏感信息通过环境变量注入 |
| `.env.example` | Docker | 环境变量模板 |
| `.env` | Docker | 实际环境变量(不提交到Git) |

### 8.2 环境变量清单

#### MySQL

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| `MYSQL_URL` | jdbc:mysql://localhost:3306/campus_trade?... | 数据库连接URL |
| `MYSQL_USERNAME` | root | 数据库用户名 |
| `MYSQL_ROOT_PASSWORD` | - | **[必改]** 数据库密码 |

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

### 8.3 开发环境配置

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

### 8.4 生产环境配置

生产环境使用 `application-prod.yml`，所有敏感信息通过环境变量注入：

```yaml
# 数据库 - 环境变量 + SSL
spring.datasource.url: ${MYSQL_URL}
spring.datasource.password: ${MYSQL_PASSWORD}

# Redis - 环境变量 + 密码
spring.redis.password: ${REDIS_PASSWORD}

# RabbitMQ - 环境变量
spring.rabbitmq.password: ${RABBITMQ_PASSWORD}

# JWT - 环境变量
jwt.secret: ${JWT_SECRET}

# Knife4j - 生产环境关闭
knife4j.enable: false

# 优雅停机
server.shutdown: graceful
spring.lifecycle.timeout-per-shutdown-phase: 30s

# Actuator - 仅开放必要端点
management.endpoints.web.exposure.include: health,info,metrics
```

### 8.5 HikariCP 连接池配置对比

| 参数 | 开发 | 生产 |
|------|------|------|
| minimum-idle | 5 | 10 |
| maximum-pool-size | 20 | 50 |
| idle-timeout | 600000 | 600000 |
| max-lifetime | 1800000 | 1800000 |
| leak-detection-threshold | - | 60000 |

### 8.6 Redis Lettuce 连接池配置对比

| 参数 | 开发 | 生产 |
|------|------|------|
| max-active | 20 | 50 |
| max-idle | 10 | 20 |
| min-idle | 5 | 10 |

---

## 9. Docker部署文档

### 9.1 环境要求

| 依赖 | 最低版本 | 推荐版本 |
|------|----------|----------|
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |
| Maven | 3.8+ | 3.9+ |
| Node.js | 16+ | 18+ |
| JDK | 11 | 11 |

### 9.2 一键部署

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

### 9.3 服务端口

| 服务 | 容器内端口 | 宿主机端口 | 说明 |
|------|------------|------------|------|
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| RabbitMQ | 5672 | 5672 | AMQP协议 |
| RabbitMQ Management | 15672 | 15672 | 管理界面 |
| Backend | 8080 | 8080 | 后端API |
| Frontend-User | 80 | 80 | 用户端 |
| Frontend-Admin | 80 | 81 | 管理端 |

### 9.4 持久化挂载

| 容器路径 | 宿主机路径 | 说明 |
|----------|------------|------|
| /var/lib/mysql | /app/mysql | 数据库数据 |
| /app/logs | /app/logs | 应用日志 |
| /data/uploads | /app/upload | 上传文件 |

### 9.5 服务依赖与启动顺序

```
MySQL (健康检查通过)
  └→ RabbitMQ (健康检查通过)
       └→ Redis (健康检查通过)
            └→ Backend (健康检查通过)
                 ├→ Frontend-User
                 └→ Frontend-Admin
```

### 9.6 健康检查

| 服务 | 检查方式 | 间隔 | 超时 | 重试 |
|------|----------|------|------|------|
| MySQL | `mysqladmin ping` | 10s | 5s | 5 |
| Redis | `redis-cli ping` | 10s | 5s | 5 |
| RabbitMQ | `rabbitmq-diagnostics check_running` | 30s | 10s | 5 |
| Backend | `curl /actuator/health` | 30s | 5s | 3 |

### 9.7 常用运维命令

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

## 10. 安全注意事项

### 10.1 生产环境必须修改的配置

| 配置项 | 风险等级 | 说明 |
|--------|----------|------|
| `JWT_SECRET` | **严重** | 默认硬编码密钥，必须替换为随机强密钥 |
| `MYSQL_ROOT_PASSWORD` | **严重** | 默认 root123，必须替换 |
| `REDIS_PASSWORD` | **严重** | 默认为空，必须设置密码 |
| `RABBITMQ_USERNAME/PASSWORD` | **高** | 默认 guest/guest，必须替换 |
| `CORS_ALLOWED_ORIGINS` | **高** | 生产环境必须限制为实际域名 |

### 10.2 JWT密钥生成方法

```bash
# 方法1: OpenSSL
openssl rand -base64 48

# 方法2: Java
java -e 'System.out.println(java.util.UUID.randomUUID().toString().replace("-","")+java.util.UUID.randomUUID().toString().replace("-",""))'

# 方法3: Python
python3 -c "import secrets; print(secrets.token_urlsafe(48))"
```

### 10.3 安全机制清单

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

### 10.4 生产环境检查清单

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

---

## 11. 测试报告

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

## 12. 性能优化报告

### 数据库层

| 优化项 | 实现方式 |
|--------|----------|
| 索引优化 | 15张表, 30+索引, 唯一索引+联合索引 |
| 逻辑删除 | deleted字段，避免物理删除开销 |
| 乐观锁 | version字段，防止并发冲突 |
| 分页查询 | LIMIT + offset |
| 批量查询 | `selectByIds` 替代循环单查(N+1修复) |

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

### 日志层

| 优化项 | 实现方式 |
|--------|----------|
| Log4j2异步日志 | AsyncLogger |
| 5文件分类 | info/error/audit/security/sql |
| traceId追踪 | 全链路TraceId |

---

## 13. 常见问题排查

### 13.1 后端启动失败

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 连接MySQL失败 | MySQL未启动/密码错误 | 检查MySQL容器状态和密码配置 |
| 连接Redis失败 | Redis未启动/密码不匹配 | 检查Redis容器和密码 |
| 连接RabbitMQ失败 | MQ未启动/用户名密码错误 | 检查MQ容器和凭据 |
| JWT解析失败 | 密钥不匹配 | 确认JWT_SECRET一致 |
| 端口被占用 | 8080端口冲突 | 修改SERVER_PORT或释放端口 |

### 13.2 前端问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| API请求404 | Nginx代理配置错误 | 检查nginx.conf中proxy_pass |
| 登录后Token丢失 | localStorage被清空 | 检查浏览器隐私设置 |
| CORS报错 | 后端CORS配置不匹配 | 检查CORS_ALLOWED_ORIGINS |
| 页面空白 | 前端未构建 | 执行 `npm run build` |

### 13.3 Docker问题

| 现象 | 可能原因 | 解决方案 |
|------|----------|----------|
| 容器启动后立即退出 | 健康检查失败/配置错误 | `docker-compose logs <service>` |
| MySQL初始化失败 | init.sql执行错误 | 检查SQL语法，查看MySQL日志 |
| 后端无法连接MySQL | 网络未就绪 | 确认depends_on和healthcheck |
| 上传文件丢失 | 挂载路径不正确 | 检查volumes配置 |

### 13.4 日志排查

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

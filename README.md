# CampusTrade - 校园贸易平台

企业级前后端分离校园贸易平台，基于 Spring Boot 2.7 + Vue 3 + TypeScript 构建，支持支付宝沙箱担保交易。

## 技术栈

### 后端
- Spring Boot 2.7 + Spring Security + JWT + RBAC
- MyBatis + MySQL 8.0
- Redis (缓存/限流/Token黑名单/幂等/分布式锁)
- RabbitMQ (异步消息/死信队列/手动ACK)
- 支付宝沙箱 SDK (担保交易/退款)
- Log4j2 (五文件日志/traceId全链路追踪)
- Knife4j (API文档)

### 前端
- Vue 3 + TypeScript + Pinia
- Element Plus
- STOMP WebSocket (实时聊天)
- Vite

### 部署
- Docker Compose (MySQL/Redis/RabbitMQ/Nginx/后端)
- Nginx (反向代理/安全头/gzip)

## 项目结构

```
CampusTrade/
├── campus-trade-server/          # 后端 Spring Boot
│   └── src/main/java/com/campustrade/
│       ├── controller/           # REST Controller
│       ├── service/impl/         # 业务逻辑
│       ├── mapper/               # MyBatis Mapper
│       ├── entity/               # 实体类
│       ├── dto/                  # 请求DTO
│       ├── vo/                   # 响应VO
│       ├── config/               # 配置类(安全/支付宝/Redis等)
│       ├── security/             # JWT/XSS/安全头
│       ├── mq/                   # RabbitMQ消费者
│       ├── aspect/               # AOP操作日志切面
│       └── util/                 # 工具类
├── campus-trade-user/            # 用户端 Vue3
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

1. 创建数据库：
```sql
CREATE DATABASE campus_trade DEFAULT CHARACTER SET utf8mb4;
```

2. 启动后端（DataInitializer 自动建表和初始化数据）：
```bash
cd campus-trade-server
# 修改 application.yml 中的数据库/Redis连接信息
mvn spring-boot:run
```

3. 启动用户端前端：
```bash
cd campus-trade-user
npm install && npm run dev
```

4. 启动管理端前端：
```bash
cd campus-trade-admin
npm install && npm run dev
```

### Docker 部署

```bash
# 构建前端
cd campus-trade-user && npm install && npm run build && cd ..
cd campus-trade-admin && npm install && npm run build && cd ..

# 构建后端
cd campus-trade-server && mvn package -DskipTests && cd ..

# 启动所有服务
docker-compose up -d --build
```

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | admin123 |
| 普通用户 | user | user123 |

管理端访问：`http://<host>:81`
用户端访问：`http://<host>`

## 核心功能

### 用户端
- 用户注册/登录/JWT双Token认证/实名认证
- 商品发布/编辑/审核/上下架/收藏/搜索
- 购物车/订单创建/支付/发货/收货/退款/评价
- 支付宝沙箱担保交易（平台担保→确认收货→结算给卖家）
- 卖家收款配置管理
- STOMP实时聊天/消息撤回
- 收货地址管理（省市区级联）
- 关注/黑名单/通知/系统公告
- 暗色模式/响应式布局

### 管理端
- 仪表盘（数据统计/图表）
- 用户管理（封禁/解封/导出CSV）
- 商品审核/分类管理
- 订单管理（退款审批/导出CSV）
- 举报审核
- 横幅管理/公告管理
- 系统配置（支付宝沙箱密钥配置）
- 资金流水查看
- 操作日志/安全日志

### 支付系统
- **担保交易模式**：买家支付 → 平台担保冻结 → 买家确认收货 → 平台结算给卖家
- 支付宝沙箱支付（管理端配置密钥，AES加密存储）
- 未配置时自动降级为模拟支付
- 退款对接支付宝退款API
- 资金流水全链路记录（PAY/FREEZE/SETTLE/REFUND）
- 订单超时5分钟自动取消（Redis Key过期+分布式锁+定时任务兜底）

## 安全特性

- BCrypt密码加密 + 密码强度校验
- JWT accessToken(2h) + refreshToken(7d) + Token黑名单
- RBAC三角色权限(ROLE_USER/ROLE_ADMIN/ROLE_SUPER_ADMIN)
- 账号锁定(5次失败锁定30分钟)
- 接口限流 + 防重复提交
- XSS过滤 + SQL注入防御 + 敏感词过滤
- 安全响应头(CSP/X-Frame-Options/HSTS等)
- CORS可配置白名单
- 操作日志 + 安全日志全记录

## API文档

启动后端后访问：`http://<host>:8080/doc.html`

## License

MIT

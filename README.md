# CampusTrade - 校园二手交易平台

企业级前后端分离校园二手交易平台，基于 Spring Boot + Vue3 构建。

## 技术栈

### 后端
- Spring Boot 2.7 + Spring Security + JWT + RBAC
- MyBatis + MySQL 8.0
- Redis (缓存/限流/Token黑名单/幂等)
- RabbitMQ (异步消息/死信队列/手动ACK)
- Log4j2 (五文件日志/traceId全链路追踪)
- Knife4j (API文档)

### 前端
- Vue 3 + TypeScript + Pinia
- Element Plus
- Vite

### 部署
- Docker Compose (MySQL/Redis/RabbitMQ/Nginx/后端)
- Nginx (反向代理/安全头/gzip)

## 项目结构

```
CampusTrade/
├── campus-trade-server/          # 后端 Spring Boot
│   └── src/main/java/com/campustrade/
│       ├── controller/           # 10个Controller
│       ├── service/impl/         # 10个ServiceImpl
│       ├── mapper/               # 15个Mapper
│       ├── entity/               # 15个实体
│       ├── dto/                  # 13个DTO
│       ├── vo/                   # 11个VO
│       ├── config/               # 配置类
│       ├── security/             # JWT/XSS/安全头
│       ├── mq/                   # 6个MQ消费者
│       ├── aspect/               # AOP切面
│       ├── constant/             # 常量
│       ├── enum_/                # 枚举
│       ├── exception/            # 全局异常处理
│       └── util/                 # 工具类
├── campus-trade-user/            # 用户端 Vue3
├── campus-trade-admin/           # 管理端 Vue3
├── campus-trade-docs/            # 规范文档
├── nginx/                        # Nginx配置
├── docker-compose.yml
├── .env.example
└── DEPLOYMENT-GUIDE.md
```

## 快速开始

### 环境要求
- JDK 11+
- Node.js 16+
- MySQL 8.0+
- Redis 6+
- RabbitMQ 3.8+

### 本地开发

1. 创建数据库并执行初始化脚本：
```sql
CREATE DATABASE campus_trade DEFAULT CHARACTER SET utf8mb4;
```
执行 `campus-trade-server/src/main/resources/sql/init.sql`

2. 修改 `application.yml` 中的数据库/Redis/RabbitMQ连接信息

3. 启动后端：
```bash
cd campus-trade-server
mvn spring-boot:run
```

4. 启动用户端前端：
```bash
cd campus-trade-user
npm install
npm run dev
```

5. 启动管理端前端：
```bash
cd campus-trade-admin
npm install
npm run dev
```

### Docker 部署

```bash
cp .env.example .env
# 编辑 .env 填入实际配置
docker-compose up -d
```

详见 [DEPLOYMENT-GUIDE.md](campus-trade-docs/DEPLOYMENT-GUIDE.md)

## 核心功能

- 用户注册/登录/JWT双Token认证
- 商品发布/审核/上下架/收藏
- 订单创建/支付/发货/收货/退款
- 实时聊天
- 举报与审核
- 管理后台(用户管理/商品审核/订单管理/举报处理/日志查看)
- 文件上传
- 消息通知

## 安全特性

- BCrypt密码加密 + 密码强度校验
- JWT accessToken(2h) + refreshToken(7d) + Token黑名单
- RBAC三角色权限(ROLE_USER/ROLE_ADMIN/ROLE_SUPER_ADMIN)
- 账号锁定(5次失败锁定30分钟)
- 接口限流 + 防重复提交
- XSS过滤 + SQL注入防御 + 敏感词过滤
- 安全响应头(CSP/X-Frame-Options/HSTS等)
- CORS可配置白名单

## License

MIT
# CampusTrade 部署指南

## 目录

- [环境要求](#环境要求)
- [快速部署（Docker Compose 一键部署）](#快速部署)
- [配置详解](#配置详解)
- [支付宝沙箱配置](#支付宝沙箱配置)
- [本地开发](#本地开发)
- [常见问题](#常见问题)
- [目录与端口说明](#目录与端口说明)

---

## 环境要求

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| 操作系统 | Linux (Ubuntu 20.04+) | 推荐使用 Ubuntu/CentOS |
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 容器编排 |
| 内存 | 4GB+ | MySQL+Redis+RabbitMQ+后端共需约3GB |
| 磁盘 | 20GB+ | 数据库+日志+上传文件 |

---

## 快速部署

### 1. 克隆代码

```bash
git clone https://github.com/SuH-Love/CampusTrade.git
cd CampusTrade
```

### 2. 配置环境变量

```bash
cp .env.example .env
vim .env
```

**必须修改的变量**：

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root密码 | `MyStr0ngP@ssw0rd!` |
| `REDIS_PASSWORD` | Redis密码 | `R3disS3cure!` |
| `RABBITMQ_USERNAME` | RabbitMQ用户名 | `campustrade` |
| `RABBITMQ_PASSWORD` | RabbitMQ密码 | `R4bbitMQ!` |
| `JWT_SECRET` | JWT签名密钥 | `openssl rand -base64 48` 生成 |
| `ADMIN_PASSWORD` | 管理员初始密码 | 自定义强密码 |
| `CORS_ALLOWED_ORIGINS` | 允许的前端域名 | `http://your-server-ip,http://your-server-ip:81` |
| `CORS_ORIGINS` | WebSocket允许的域名 | `http://your-server-ip:*` |

### 3. 构建前端

```bash
cd campus-trade-user && npm install && npm run build && cd ..
cd campus-trade-admin && npm install && npm run build && cd ..
```

### 4. 构建后端

```bash
cd campus-trade-server && mvn package -DskipTests && cd ..
```

### 5. 启动所有服务

```bash
docker-compose up -d --build
```

### 6. 验证部署

```bash
# 查看所有容器状态
docker-compose ps

# 查看后端日志
docker logs campus-trade-backend -f

# 检查后端健康状态
curl http://localhost:8080/actuator/health
```

### 7. 访问应用

| 服务 | 地址 |
|------|------|
| 用户端 | `http://<服务器IP>` |
| 管理端 | `http://<服务器IP>:81` |
| API文档 | `http://<服务器IP>:8080/doc.html` |
| RabbitMQ管理 | `http://<服务器IP>:15672` |

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 超级管理员 | admin | admin123（或 .env 中 ADMIN_PASSWORD） |
| 普通用户 | user | user123 |

---

## 配置详解

### .env 环境变量完整说明

#### MySQL 配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | `root123` | **必改** MySQL root密码 |
| `MYSQL_DATABASE` | `campus_trade` | 数据库名 |
| `MYSQL_USERNAME` | `root` | 数据库用户名 |

#### Redis 配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `REDIS_PASSWORD` | `redis123` | **必改** Redis密码 |
| `REDIS_PORT` | `6379` | Redis端口 |

#### RabbitMQ 配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `RABBITMQ_USERNAME` | `guest` | **必改** RabbitMQ用户名 |
| `RABBITMQ_PASSWORD` | `guest` | **必改** RabbitMQ密码 |

#### JWT 配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `JWT_SECRET` | 内置默认值 | **必改** JWT签名密钥，建议64位随机字符串 |
| `JWT_ACCESS_EXPIRATION` | `7200000` | AccessToken过期时间(毫秒)，默认2小时 |
| `JWT_REFRESH_EXPIRATION` | `604800000` | RefreshToken过期时间(毫秒)，默认7天 |

#### CORS 跨域配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `CORS_ALLOWED_ORIGINS` | `http://localhost,...` | 允许的前端来源域名，逗号分隔 |
| `CORS_ORIGINS` | `http://localhost:*,...` | WebSocket允许的来源域名 |

**重要**：部署到服务器后，必须将服务器IP加入CORS白名单：
```
CORS_ALLOWED_ORIGINS=http://61.139.2.140,http://61.139.2.140:81
CORS_ORIGINS=http://61.139.2.140:*
```

#### 应用配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SPRING_PROFILES_ACTIVE` | `prod` | Spring配置环境(prod/dev) |
| `JAVA_OPTS` | `-Xms512m -Xmx1024m -XX:+UseG1GC` | JVM参数 |
| `SERVER_PORT` | `8080` | 后端服务端口 |
| `ADMIN_PASSWORD` | `admin123` | 管理员初始密码 |

#### Nginx/前端端口

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `FRONTEND_USER_PORT` | `80` | 用户端端口 |
| `FRONTEND_ADMIN_PORT` | `81` | 管理端端口 |

---

## 支付宝沙箱配置

系统支持支付宝沙箱担保交易，**无需修改代码或配置文件**，通过管理端后台设置即可。

### 配置步骤

1. 登录 [支付宝开放平台沙箱](https://open.alipay.com/develop/sandbox/app)
2. 获取沙箱应用的 `APPID`、`应用私钥`、`支付宝公钥`
3. 登录管理端 `http://<服务器IP>:81`
4. 进入 **系统配置** 页面
5. 填写以下6项配置：

| 配置项 | 说明 |
|--------|------|
| alipay.app_id | 沙箱应用APPID |
| alipay.private_key | 应用私钥（RSA2） |
| alipay.alipay_public_key | 支付宝公钥 |
| alipay.gateway | 默认 `https://openapi-sandbox.dl.alipaydev.com/gateway.do` |
| alipay.notify_url | 异步通知URL，格式 `http://<服务器IP>/api/order/pay/notify` |
| alipay.return_url | 同步跳转URL，格式 `http://<服务器IP>/order/` |

### 安全说明

- 私钥和公钥在数据库中 **AES加密存储**
- 管理端显示为掩码 `******`
- 未配置时自动降级为**模拟支付**（无需支付宝即可测试完整流程）

### 担保交易流程

```
买家下单 → 买家支付(支付宝/模拟) → 平台担保冻结
→ 卖家发货 → 买家确认收货 → 平台结算给卖家
→ (如需退款) 卖家同意 → 支付宝退款API → 资金退回买家
```

---

## 本地开发

### 1. 启动基础设施

```bash
# 仅启动 MySQL + Redis + RabbitMQ
docker-compose up -d mysql redis rabbitmq
```

### 2. 启动后端

```bash
cd campus-trade-server
# 修改 application.yml 中的连接信息为 localhost
mvn spring-boot:run
```

### 3. 启动前端

```bash
# 用户端
cd campus-trade-user && npm install && npm run dev

# 管理端
cd campus-trade-admin && npm install && npm run dev
```

### 4. 访问

| 服务 | 地址 |
|------|------|
| 用户端 | http://localhost:5173 |
| 管理端 | http://localhost:5174 |
| API文档 | http://localhost:8080/doc.html |

---

## 常见问题

### Q: 后端启动报 `Unknown column 'xxx' in 'field list'`

DataInitializer 会在启动时自动添加缺失的列。如果仍然报错，手动执行：

```bash
docker exec -i campus-trade-mysql mysql -uroot -p<密码> campus_trade <<'EOF'
ALTER TABLE t_order ADD COLUMN trade_no VARCHAR(64) DEFAULT NULL;
ALTER TABLE t_order ADD COLUMN pre_refund_status VARCHAR(20) DEFAULT NULL;
ALTER TABLE t_order ADD COLUMN seller_payment_config_id BIGINT DEFAULT NULL;
ALTER TABLE t_order ADD COLUMN tracking_no VARCHAR(64) DEFAULT NULL;
ALTER TABLE t_goods ADD COLUMN `condition` VARCHAR(20) DEFAULT NULL;
ALTER TABLE t_goods ADD COLUMN stock INT DEFAULT 1;
ALTER TABLE t_order_item ADD COLUMN quantity INT DEFAULT 1;
ALTER TABLE t_order ADD COLUMN delivery_method TINYINT DEFAULT 1;
ALTER TABLE t_order ADD COLUMN address VARCHAR(500) DEFAULT NULL;
EOF
```

### Q: 前端请求后端 403 CORS 错误

确保 `.env` 中 `CORS_ALLOWED_ORIGINS` 包含前端实际访问地址：
```
CORS_ALLOWED_ORIGINS=http://your-ip,http://your-ip:81
```
修改后重启后端：`docker-compose restart backend`

### Q: MySQL 中文乱码

docker-compose.yml 中已配置 `--character-set-server=utf8mb4 --character-set-client-handshake=FALSE --init-connect='SET NAMES utf8mb4'`，如果仍有乱码，检查连接字符串是否包含 `characterEncoding=UTF-8`。

### Q: WebSocket 连接失败

1. 确保 Nginx 配置中 `/ws` location 正确代理到后端
2. 确保 `CORS_ORIGINS` 包含前端域名
3. 前端使用 STOMP 协议连接 `ws://<host>/ws`

### Q: 支付宝沙箱支付不生效

1. 确认管理端 **系统配置** 中6项支付宝配置已填写
2. 确认 `alipay.notify_url` 可从外网访问
3. 未配置时系统自动使用模拟支付，功能等价

### Q: 图片上传后无法显示

确保 Nginx 配置了 `/uploads/` 代理：
```nginx
location /uploads/ {
    proxy_pass http://backend:8080/uploads/;
}
```

### Q: 重新部署后数据丢失

MySQL 数据持久化在 `/app/mysql`，上传文件在 `/app/upload`。只要这两个目录存在，数据不会丢失。

### Q: 如何查看日志

```bash
# 后端日志
docker logs campus-trade-backend -f --tail 200

# MySQL日志
docker logs campus-trade-mysql -f --tail 100

# 所有服务状态
docker-compose ps
```

### Q: 如何完全重置数据

```bash
docker-compose down
rm -rf /app/mysql /app/upload /app/logs
docker-compose up -d --build
```

---

## 目录与端口说明

### Docker 容器

| 容器名 | 镜像 | 端口映射 | 说明 |
|--------|------|---------|------|
| campus-trade-mysql | mysql:8.0 | 3306:3306 | 数据库 |
| campus-trade-redis | redis:7-alpine | 6379:6379 | 缓存/限流/锁 |
| campus-trade-rabbitmq | rabbitmq:3-management-alpine | 5672:5672, 15672:15672 | 消息队列 |
| campus-trade-backend | 自建(openjdk:11) | 8080:8080 | Spring Boot后端 |
| campus-trade-frontend-user | nginx:alpine | 80:80 | 用户端Nginx |
| campus-trade-frontend-admin | nginx:alpine | 81:80 | 管理端Nginx |

### 持久化目录

| 宿主机路径 | 容器路径 | 说明 |
|-----------|---------|------|
| `/app/mysql` | `/var/lib/mysql` | MySQL数据文件 |
| `/app/upload` | `/data/uploads` | 上传文件 |
| `/app/logs` | `/app/logs` | 后端日志 |

### 数据库表

| 表名 | 说明 |
|------|------|
| t_user | 用户表 |
| t_role / t_permission / t_role_permission | RBAC权限表 |
| t_user_role | 用户角色关联 |
| t_goods / t_goods_category | 商品/分类 |
| t_order / t_order_item | 订单/订单项 |
| t_payment_config | 卖家收款配置 |
| t_fund_log | 资金流水(PAY/FREEZE/SETTLE/REFUND) |
| t_system_config | 系统配置(支付宝密钥等) |
| t_seller_rating | 卖家评价 |
| t_cart | 购物车 |
| t_delivery_address | 收货地址 |
| t_user_follow | 用户关注 |
| t_user_blacklist | 黑名单 |
| t_notification / t_notification_preference | 通知/通知偏好 |
| t_banner / t_announcement | 横幅/公告 |
| t_report | 举报 |
| t_chat_message | 聊天消息 |
| t_operation_log / t_security_log | 操作日志/安全日志 |

### Nginx 代理路径

| 路径 | 代理目标 | 说明 |
|------|---------|------|
| `/` | 静态文件 | Vue SPA |
| `/api/` | `http://backend:8080/api/` | REST API |
| `/ws` | `http://backend:8080/ws` | STOMP WebSocket |
| `/uploads/` | `http://backend:8080/uploads/` | 上传文件 |

---

## 更新部署

```bash
cd CampusTrade
git pull

# 重新构建前端
cd campus-trade-user && npm install && npm run build && cd ..
cd campus-trade-admin && npm install && npm run build && cd ..

# 重新构建后端
cd campus-trade-server && mvn package -DskipTests && cd ..

# 重建并启动（数据库数据不会丢失）
docker-compose up -d --build
```
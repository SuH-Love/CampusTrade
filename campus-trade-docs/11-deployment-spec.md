# 部署规范

## 部署方式

Docker Compose 一键部署

## 服务清单

| 服务 | 说明 |
|------|------|
| mysql | 数据库 |
| redis | 缓存 |
| rabbitmq | 消息队列 |
| backend | 后端服务 |
| frontend-user | 用户端前端 |
| frontend-admin | 管理端前端 |

## 挂载目录

| 容器路径 | 宿主机路径 | 说明 |
|----------|------------|------|
| /var/lib/mysql | /app/mysql | 数据库持久化 |
| /app/logs | /app/logs | 日志持久化 |
| /data/uploads | /app/upload | 上传文件持久化 |

## 配置要求

- 支持一键启动
- 支持日志持久化
- 支持数据库持久化
- 支持上传文件持久化
- 健康检查（MySQL/Redis/RabbitMQ/Backend）
- 依赖顺序（MySQL → RabbitMQ → Redis → Backend → Frontend）

## 文件清单

- Dockerfile
- docker-compose.yml
- nginx/user.conf（用户端Nginx配置）
- nginx/admin.conf（管理端Nginx配置）
- .env.example（环境变量模板）

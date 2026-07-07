# 部署模块 Prompt

生成部署模块。

## 必须生成

- Dockerfile
- docker-compose.yml
- nginx.conf

## 服务

| 服务 | 说明 |
|------|------|
| mysql | 数据库 |
| redis | 缓存 |
| rabbitmq | 消息队列 |
| backend | 后端服务 |
| frontend-user | 用户端前端 |
| frontend-admin | 管理端前端 |

## 挂载

| 挂载路径 | 说明 |
|----------|------|
| /app/logs | 日志持久化 |
| /app/upload | 上传文件持久化 |
| /app/mysql | 数据库持久化 |

## 要求

- 支持一键启动
- 支持日志持久化
- 支持数据库持久化
- 支持上传文件持久化
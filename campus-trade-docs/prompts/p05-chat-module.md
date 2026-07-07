# 聊天模块 Prompt

生成聊天模块。

## 功能

- 发送消息
- 历史记录
- 未读统计
- 已读回执

## 缓存

- chat:recent:{userId}

## MQ

- chat.save.queue

## 要求

- 消息先写Redis
- 异步持久化MySQL
- 支持分页查询
- 支持最近会话列表
- 必须记录日志
# 举报模块 Prompt

生成举报模块。

## 举报对象

- 商品
- 用户
- 聊天

## 状态

- PENDING
- PROCESSING
- FINISHED

## 功能

- 提交举报
- 查看举报状态
- 管理员处理举报

## MQ

- audit.report.queue

## 必须实现

- 异步审核流转
- 日志记录
- 通知发送
- 管理员操作审计
# 通知模块 Prompt

生成通知模块。

## 功能

- 系统通知
- 业务通知
- 已读/未读
- 全部已读
- 删除通知
- 分页查询

## 通知类型

- SYSTEM：系统通知
- ORDER：订单通知
- GOODS：商品通知
- REPORT：举报通知
- CHAT：聊天通知

## 缓存

- notify:user:{id}

## MQ

- notify.send.queue

## 要求

- 异步发送通知
- 未读计数缓存
- 支持分页查询
- 必须记录日志
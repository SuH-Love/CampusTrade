# 订单模块 Prompt

生成订单模块。

## 功能

- 创建订单
- 取消订单
- 支付模拟
- 确认收货
- 退款申请

## 状态

- PENDING_PAY
- PAID
- SHIPPING
- FINISHED
- CANCELLED
- REFUND

## 必须实现

- 下单事务
- Redis防重复提交
- RabbitMQ异步通知
- 订单日志记录
- 库存扣减预留设计
- 支付超时预留设计
- 生成完整SQL
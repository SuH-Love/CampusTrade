# MQ规范

## 消息中间件

RabbitMQ

## 交换机

| 交换机 | 类型 | 说明 |
|--------|------|------|
| campus.order.direct | Direct | 订单交换机 |
| campus.chat.direct | Direct | 聊天交换机 |
| campus.audit.direct | Direct | 审核交换机 |
| campus.notify.direct | Direct | 通知交换机 |
| campus.log.direct | Direct | 日志交换机 |
| campus.dlx.direct | Direct | 死信交换机 |

## 队列

| 队列 | 绑定交换机 | RoutingKey | 死信交换机 | 说明 |
|------|-----------|------------|------------|------|
| order.create.queue | campus.order.direct | order.create | campus.dlx.direct | 订单创建队列 |
| chat.save.queue | campus.chat.direct | chat.save | campus.dlx.direct | 聊天保存队列 |
| audit.report.queue | campus.audit.direct | audit.report | campus.dlx.direct | 举报审核队列 |
| notify.send.queue | campus.notify.direct | notify.send | campus.dlx.direct | 通知发送队列 |
| log.record.queue | campus.log.direct | log.record | campus.dlx.direct | 日志记录队列 |
| campus.dlx.queue | campus.dlx.direct | campus.dlx | - | 死信队列 |

## RoutingKey

| RoutingKey | 说明 |
|------------|------|
| order.create | 订单创建 |
| chat.save | 聊天保存 |
| audit.report | 举报审核 |
| notify.send | 通知发送 |
| log.record | 日志记录 |
| campus.dlx | 死信路由 |

## 必须实现

- 消息确认机制（手动ACK）
- 失败重试（Spring AMQP retry: 3次, 间隔1s）
- 死信队列（basicNack requeue=false → 死信交换机 → 死信队列）
- 幂等消费（Redis SET NX + 24h TTL去重标记）
- 日志记录

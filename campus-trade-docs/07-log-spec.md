# 日志规范

## 日志框架

Log4j2（异步日志 AsyncLogger）

## 日志文件

| 文件 | 说明 | Logger名称 |
|------|------|------------|
| logs/info.log | 信息日志 | Root Logger |
| logs/error.log | 错误日志 | Root Logger (level=ERROR) |
| logs/audit.log | 审计日志 | AuditLogger |
| logs/security.log | 安全日志 | SecurityLogger |
| logs/sql.log | SQL日志 | com.campustrade.mapper (DEBUG) |

## 保留策略

- 单文件最大: 100MB
- 保留文件数: 30个
- 滚动策略: SizeBasedTriggeringPolicy + DefaultRolloverStrategy

## 日志格式

文件日志格式：
```
时间 traceId 级别 类名 - 消息
```
Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] [%X{traceId}] %-5level %logger{36} - %msg%n`

操作日志（通过AOP切面写入数据库）包含：
- 时间
- traceId
- 用户ID
- 用户名
- 模块
- 操作
- 请求方法
- 请求URL
- IP
- 耗时
- 状态
- 异常信息

安全日志（写入数据库）包含：
- 时间
- 用户ID
- 用户名
- 事件类型（LOGIN_FAIL/ACCESS_DENIED/TOKEN_EXPIRED/RATE_LIMIT/MALICIOUS_INPUT）
- IP
- 详情

## 必须记录的操作

- 用户登录
- 商品发布
- 商品审核
- 订单创建
- 订单取消
- 举报提交
- 管理员处理
- 权限变更

## traceId全链路追踪

通过 `TraceIdUtil` + MDC 实现，所有日志文件和操作日志均携带traceId

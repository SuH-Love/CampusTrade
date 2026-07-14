# 安全规范

> 本文档非常关键，必须严格遵守。

## 认证

- 方式：JWT
- accessToken：2小时
- refreshToken：7天

## 密码

- 加密方式：BCrypt
- 强度要求：至少8位，含大写/小写/数字/特殊字符中至少3种

## 权限模型

必须实现 Spring Security RBAC

### 角色

- ROLE_USER
- ROLE_ADMIN
- ROLE_SUPER_ADMIN

### 权限（11个）

- goods:manage
- goods:create
- goods:update
- goods:delete
- goods:audit
- user:manage
- user:ban
- report:manage
- report:review
- log:manage
- log:view

## 登录限流

- 实现：Redis
- 限制：5次/分钟/IP

## 注册限流

- 实现：Redis
- 限制：3次/小时/IP

## 账号锁定

- 实现：Redis
- 5次登录失败后锁定30分钟
- Key：`login:fail:{username}`

## Token黑名单

- Key：`blacklist:token:{token}`

## 防重复提交

- Key：`repeat:{userId}:{apiMd5}`

## 安全白名单

以下路径无需认证：
- /api/auth/login
- /api/auth/register
- /api/auth/refresh
- /api/auth/captcha
- GET /api/goods/**
- GET /api/goods-category/**
- /uploads/**
- /doc.html, /webjars/**, /swagger-resources/**（仅开发环境）

## 必须实现

- XSS过滤器（7种XSS正则 + 4种SQL注入正则）
- SQL注入防御
- 敏感词过滤
- 审计日志
- 安全日志
- 安全响应头（8个：X-Content-Type-Options/X-Frame-Options/X-XSS-Protection/Referrer-Policy/CSP/HSTS/Cache-Control/Pragma）
- 文件上传校验（扩展名+MIME+大小+路径遍历）
- CORS可配置白名单

## 安全日志记录

- 登录失败（LOGIN_FAIL）
- 权限拒绝（ACCESS_DENIED）
- Token失效（TOKEN_EXPIRED）
- 频繁请求（RATE_LIMIT）
- 恶意输入（MALICIOUS_INPUT）

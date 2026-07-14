# 认证授权模块 Prompt

生成认证授权模块。

## 实现

Spring Security + JWT + RBAC

## 功能

- 认证过滤器
- 权限过滤器
- Token解析
- Token续签
- Token黑名单校验
- 权限校验注解

## 角色

- ROLE_USER
- ROLE_ADMIN
- ROLE_SUPER_ADMIN

## 权限

- goods:create
- goods:update
- goods:audit
- report:handle
- log:view

## 生成

- SecurityConfig
- JwtFilter
- JwtUtil
- UserDetailsService
- PermissionEvaluator
- AccessDeniedHandler
- AuthenticationEntryPoint
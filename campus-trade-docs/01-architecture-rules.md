# 架构规则

> 本文档是 AI 最重要的约束，必须严格遵守。

## 架构模式

MVC

## 目录结构

```
com.campustrade
├── controller
├── service
├── service.impl
├── mapper
├── entity
├── dto
├── vo
├── config
├── common
├── exception
├── security
├── mq
├── constant
├── enum_
├── base
├── aspect
├── util
```

## 职责定义

### Controller

- 接收请求
- 参数校验
- 返回结果

**禁止：**

- 写业务逻辑
- 写SQL

### Service

- 核心业务逻辑
- 事务控制

### Mapper

- SQL映射

### DTO

- 入参

### VO

- 出参

### Entity

- 数据库映射

**禁止：** Entity直接返回前端

## 规范

### 所有Controller

路径必须：

```
/api/{module}
```

示例：

```
/api/user
/api/goods
/api/order
```

### 必须统一

- 统一异常处理
- 统一返回结构
- 统一日志记录
- 统一鉴权
- 统一限流
- 统一幂等
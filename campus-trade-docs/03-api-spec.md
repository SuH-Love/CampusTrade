# API规范

## RESTful约定

| 方法 | 用途 |
|------|------|
| GET | 查询 |
| POST | 创建 |
| PUT | 修改 |
| DELETE | 删除 |

## 统一返回结构

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## 统一分页请求

```json
{
  "pageNum": 1,
  "pageSize": 10
}
```

## 统一分页返回

```json
{
  "list": [],
  "total": 100
}
```

## 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 405 | 请求方法不支持 |
| 415 | 不支持的媒体类型 |
| 423 | 账号已锁定 |
| 429 | 频率过高 |
| 500 | 系统异常 |

## 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名已存在 |
| 1002 | 手机号已注册 |
| 1003 | 邮箱已注册 |
| 1004 | 用户名或密码错误 |
| 1005 | Token已过期 |
| 1006 | Token无效 |
| 1007 | 账号已被禁用 |
| 1008 | 原密码错误 |
| 2001 | 商品不存在 |
| 2002 | 非商品所有者 |
| 2003 | 商品状态异常 |
| 2004 | 已收藏该商品 |
| 3001 | 订单不存在 |
| 3002 | 订单状态异常 |
| 3003 | 非订单所有者 |
| 3004 | 请勿重复提交 |
| 4001 | 举报不存在 |
| 4002 | 已举报过 |
| 5001 | 消息不存在 |
| 6001 | 请求过于频繁 |
| 6002 | 请勿重复提交 |
| 6003 | 数据已被修改，请刷新后重试 |

## 接口命名

### 正确

```
GET  /api/goods
POST /api/goods
PUT  /api/goods/{id}
```

### 错误

```
POST /api/getGoods
POST /api/updateGoods
```

## API端点清单

### 认证模块 /api/auth

| 方法 | 路径 | 说明 | 鉴权 | 限流 |
|------|------|------|------|------|
| POST | /api/auth/register | 注册 | 否 | @RateLimit + @RepeatSubmit |
| POST | /api/auth/login | 登录 | 否 | @RateLimit |
| POST | /api/auth/logout | 退出 | 是 | - |
| POST | /api/auth/refresh | 刷新Token | 否 | - |

### 用户模块 /api/user

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/user/info | 获取个人信息 | 是 |
| PUT | /api/user/info | 修改个人信息 | 是 |
| PUT | /api/user/password | 修改密码 | 是 |
| POST | /api/user/verify | 实名认证 | 是 |
| POST | /api/user/avatar | 上传头像 | 是 |

### 商品模块 /api/goods

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /api/goods | 发布商品 | 是 |
| PUT | /api/goods/{id} | 修改商品 | 是 |
| DELETE | /api/goods/{id} | 删除商品 | 是 |
| GET | /api/goods/{id} | 商品详情 | 否 |
| GET | /api/goods | 商品列表 | 否 |
| GET | /api/goods/hot | 热门商品 | 否 |
| GET | /api/goods/recommend | 推荐商品 | 否 |
| PUT | /api/goods/{id}/submit | 提交审核 | 是 |
| PUT | /api/goods/{id}/online | 上架 | 是 |
| PUT | /api/goods/{id}/offline | 下架 | 是 |
| POST | /api/goods/{id}/favorite | 收藏 | 是 |
| DELETE | /api/goods/{id}/favorite | 取消收藏 | 是 |

### 分类模块 /api/goods-category

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/goods-category | 分类列表 | 否 |

### 订单模块 /api/order

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /api/order | 创建订单 | 是 |
| PUT | /api/order/{id}/cancel | 取消订单 | 是 |
| PUT | /api/order/{id}/pay | 支付 | 是 |
| PUT | /api/order/{id}/ship | 发货 | 是 |
| PUT | /api/order/{id}/finish | 确认收货 | 是 |
| PUT | /api/order/{id}/refund | 退款 | 是 |
| GET | /api/order/{id} | 订单详情 | 是 |
| GET | /api/order/buyer | 买家订单 | 是 |
| GET | /api/order/seller | 卖家订单 | 是 |

### 聊天模块 /api/chat

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /api/chat | 发送消息 | 是 |
| GET | /api/chat/history/{targetUserId} | 聊天记录 | 是 |
| GET | /api/chat/recent | 最近会话 | 是 |
| GET | /api/chat/unread/{senderId} | 未读数量 | 是 |
| PUT | /api/chat/read/{senderId} | 标记已读 | 是 |

### 举报模块 /api/report

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /api/report | 提交举报 | 是 |
| GET | /api/report/mine | 我的举报 | 是 |
| PUT | /api/report/{id}/handle | 处理举报 | ADMIN+ |

### 通知模块 /api/notification

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/notification | 通知列表 | 是 |
| GET | /api/notification/unread-count | 未读数量 | 是 |
| PUT | /api/notification/{id}/read | 标记已读 | 是 |
| PUT | /api/notification/read-all | 全部已读 | 是 |
| DELETE | /api/notification/{id} | 删除通知 | 是 |

### 文件模块 /api/file

| 方法 | 路径 | 说明 | 鉴权 | 限流 |
|------|------|------|------|------|
| POST | /api/file/upload | 上传图片 | 是 | @RateLimit |
| DELETE | /api/file/delete | 删除图片 | 是 | - |

### 管理模块 /api/admin

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/admin/dashboard/stats | 仪表盘统计 | ADMIN+ |
| GET | /api/admin/user | 用户列表 | user:ban |
| PUT | /api/admin/user/{id}/ban | 封禁用户 | user:ban |
| PUT | /api/admin/user/{id}/unban | 解封用户 | user:ban |
| GET | /api/admin/goods | 商品审核列表 | goods:audit |
| PUT | /api/admin/goods/{id}/audit | 审核商品 | goods:audit |
| GET | /api/admin/order | 订单管理列表 | ADMIN+ |
| GET | /api/admin/report | 举报管理列表 | report:review |
| PUT | /api/admin/report/{id}/resolve | 举报通过 | report:review |
| PUT | /api/admin/report/{id}/dismiss | 举报驳回 | report:review |
| GET | /api/admin/log/operation | 操作日志 | log:view |
| GET | /api/admin/log/security | 安全日志 | log:view |

## 接口文档

必须生成 Knife4j 接口文档

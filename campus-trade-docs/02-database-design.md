# 数据库设计

> 本文档是 AI 最容易乱写的地方，必须锁死。

## 数据库

- 数据库名：`campus_trade`
- 字符集：`utf8mb4`

## 公共字段

所有表必须包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PRIMARY KEY AUTO_INCREMENT | 主键 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT DEFAULT 0 | 逻辑删除 |
| version | INT DEFAULT 0 | 乐观锁版本号 |

## 表清单

- t_user
- t_role
- t_permission
- t_user_role
- t_role_permission
- t_goods
- t_goods_category
- t_goods_favorite
- t_order
- t_order_item
- t_chat_message
- t_report
- t_notification
- t_operation_log
- t_security_log

## 索引规范

### 唯一索引

- username
- phone
- email
- role_code
- permission_code
- order_no
- uk_user_role(user_id, role_id)
- uk_role_permission(role_id, permission_id)
- uk_user_goods(user_id, goods_id)

### 普通索引

- status
- create_time

### 联合索引

- idx_user_status(user_id, status)
- idx_goods_status(id, status)
- idx_sender_receiver(sender_id, receiver_id)
- idx_buyer_status(buyer_id, status)
- idx_seller_status(seller_id, status)
- idx_user_read(user_id, is_read)
- idx_target(target_type, target_id)

## 权限码（11个）

| 权限 | 编码 | 类型 |
|------|------|------|
| 商品管理 | goods:manage | 菜单 |
| 商品创建 | goods:create | 按钮 |
| 商品修改 | goods:update | 按钮 |
| 商品删除 | goods:delete | 按钮 |
| 商品审核 | goods:audit | 按钮 |
| 用户管理 | user:manage | 菜单 |
| 用户封禁 | user:ban | 按钮 |
| 举报管理 | report:manage | 菜单 |
| 举报审核 | report:review | 按钮 |
| 日志管理 | log:manage | 菜单 |
| 日志查看 | log:view | 按钮 |

## 初始化数据

- 3个角色: ROLE_SUPER_ADMIN, ROLE_ADMIN, ROLE_USER
- 11个权限: 见上方权限码表
- 6个商品分类: 数码电子, 书籍教材, 生活用品, 服装鞋帽, 运动户外, 其他

## 约束

- 所有高频查询字段必须建索引
- 所有删除必须逻辑删除

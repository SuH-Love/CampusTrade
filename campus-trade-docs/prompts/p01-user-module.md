# 用户模块 Prompt

现在生成用户模块。

## 严格遵守

- 00-project-charter.md
- 01-architecture-rules.md
- 02-database-design.md
- 03-api-spec.md
- 04-security-spec.md
- 05-cache-spec.md
- 07-log-spec.md

## 生成顺序

1. 模块说明
2. 表结构SQL
3. Entity
4. DTO
5. VO
6. Mapper
7. Mapper XML
8. Service
9. ServiceImpl
10. Controller
11. 单元测试

## 功能

- 注册
- 登录
- 退出
- 刷新Token
- 实名认证
- 修改资料
- 修改密码
- 上传头像
- 查询个人信息

## 要求

- 密码必须BCrypt
- 登录必须JWT
- Token必须Redis缓存
- 退出必须加入黑名单
- 记录操作日志
- 记录安全日志
- 接口必须符合RESTful
- 禁止跳步
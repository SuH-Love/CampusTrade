# 商品模块 Prompt

生成商品模块。

## 功能

- 发布商品
- 修改商品
- 删除商品
- 商品详情
- 分页
- 搜索
- 分类筛选
- 价格筛选
- 商品审核
- 上下架

## 状态

- DRAFT
- PENDING
- APPROVED
- REJECTED
- ONLINE
- OFFLINE
- SOLD

## 缓存

- goods:detail:{id}
- goods:hot
- goods:recommend

## 要求

- 热门商品缓存
- 详情缓存
- 搜索支持分页
- 审核必须记录日志
- 上下架必须记录日志
- 必须使用DTO和VO
- 生成完整SQL和索引
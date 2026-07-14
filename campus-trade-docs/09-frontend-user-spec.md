# 前端用户端规范

## 技术栈

- Vue3
- TypeScript
- Pinia
- ElementPlus
- Vite

## 目录结构

```
src/
├── api/
├── assets/
├── components/
├── composables/
├── layouts/
├── pages/
├── router/
├── stores/
├── styles/
├── types/
├── utils/
└── App.vue
```

## 页面模块

- 首页
- 商品列表
- 商品详情
- 商品发布
- 个人中心
- 订单管理
- 聊天
- 举报
- 通知
- 登录
- 注册

## 规范

- 组件名使用 PascalCase
- 组合式 API（setup语法糖）
- TypeScript严格模式
- 统一请求封装（axios拦截器）
- 统一错误处理
- 路由守卫鉴权
- Pinia状态管理
- 响应式布局
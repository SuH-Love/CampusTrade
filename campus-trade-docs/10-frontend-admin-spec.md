# 前端管理端规范

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

- 登录
- 仪表盘
- 用户管理
- 商品审核
- 订单管理
- 举报审核
- 日志中心

> 注：权限管理、角色管理、菜单管理、系统设置页面待后续迭代实现

## 规范

- 组件名使用 PascalCase
- 组合式 API（setup语法糖）
- TypeScript严格模式
- 统一请求封装（axios拦截器 + 401自动刷新Token）
- 统一错误处理
- 路由守卫鉴权
- Pinia状态管理

> 注：RBAC动态菜单、动态路由、按钮级权限控制待后续迭代实现

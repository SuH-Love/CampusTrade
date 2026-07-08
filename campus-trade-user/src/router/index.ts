import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/Register.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/pages/Home.vue') },
      { path: 'goods', name: 'GoodsList', component: () => import('@/pages/GoodsList.vue') },
      { path: 'goods/:id', name: 'GoodsDetail', component: () => import('@/pages/GoodsDetail.vue') },
      { path: 'goods/publish', name: 'GoodsPublish', component: () => import('@/pages/GoodsPublish.vue'), meta: { auth: true } },
      { path: 'my-goods', name: 'MyGoods', component: () => import('@/pages/MyGoods.vue'), meta: { auth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/pages/Profile.vue'), meta: { auth: true } },
      { path: 'order', name: 'Orders', component: () => import('@/pages/Orders.vue'), meta: { auth: true } },
      { path: 'chat', name: 'Chat', component: () => import('@/pages/Chat.vue'), meta: { auth: true } },
      { path: 'notification', name: 'Notification', component: () => import('@/pages/Notification.vue'), meta: { auth: true } },
      { path: 'report', name: 'Report', component: () => import('@/pages/Report.vue'), meta: { auth: true } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.auth && !userStore.token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
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
      { path: 'goods/edit/:id', name: 'GoodsEdit', component: () => import('@/pages/GoodsEdit.vue'), meta: { auth: true } },
      { path: 'my-goods', name: 'MyGoods', component: () => import('@/pages/MyGoods.vue'), meta: { auth: true } },
      { path: 'favorites', name: 'Favorites', component: () => import('@/pages/Favorites.vue'), meta: { auth: true } },
      { path: 'following', name: 'Following', component: () => import('@/pages/Following.vue'), meta: { auth: true } },
      { path: 'cart', name: 'Cart', component: () => import('@/pages/Cart.vue'), meta: { auth: true } },
      { path: 'address', name: 'AddressManage', component: () => import('@/pages/AddressManage.vue'), meta: { auth: true } },
      { path: 'profile/:id?', name: 'Profile', component: () => import('@/pages/Profile.vue'), meta: { auth: true } },
      { path: 'order', name: 'Orders', component: () => import('@/pages/Orders.vue'), meta: { auth: true } },
      { path: 'order/:id', name: 'OrderDetail', component: () => import('@/pages/OrderDetail.vue'), meta: { auth: true } },
      { path: 'chat', name: 'Chat', component: () => import('@/pages/Chat.vue'), meta: { auth: true } },
      { path: 'notification', name: 'Notification', component: () => import('@/pages/Notification.vue'), meta: { auth: true } },
      { path: 'report', name: 'Report', component: () => import('@/pages/Report.vue'), meta: { auth: true } },
      { path: 'my-reports', name: 'MyReports', component: () => import('@/pages/MyReports.vue'), meta: { auth: true } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFound.vue')
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
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const lazyLoad = (importFn: () => Promise<Record<string, unknown>>) => {
  return () => importFn().catch((error: Error) => {
    if (error.message?.includes('Failed to fetch dynamically imported module')) {
      return new Promise(resolve => setTimeout(resolve, 1500)).then(() => importFn())
    }
    throw error
  })
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: lazyLoad(() => import('@/pages/Login.vue'))
  },
  {
    path: '/register',
    name: 'Register',
    component: lazyLoad(() => import('@/pages/Register.vue'))
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: lazyLoad(() => import('@/pages/ForgotPassword.vue'))
  },
  {
    path: '/',
    component: lazyLoad(() => import('@/layouts/MainLayout.vue')),
    children: [
      { path: '', name: 'Home', component: lazyLoad(() => import('@/pages/Home.vue')) },
      { path: 'goods', name: 'GoodsList', component: lazyLoad(() => import('@/pages/GoodsList.vue')) },
      { path: 'goods/:id', name: 'GoodsDetail', component: lazyLoad(() => import('@/pages/GoodsDetail.vue')) },
      { path: 'goods/publish', name: 'GoodsPublish', component: lazyLoad(() => import('@/pages/GoodsPublish.vue')), meta: { auth: true } },
      { path: 'goods/edit/:id', name: 'GoodsEdit', component: lazyLoad(() => import('@/pages/GoodsEdit.vue')), meta: { auth: true } },
      { path: 'my-goods', name: 'MyGoods', component: lazyLoad(() => import('@/pages/MyGoods.vue')), meta: { auth: true } },
      { path: 'favorites', name: 'Favorites', component: lazyLoad(() => import('@/pages/Favorites.vue')), meta: { auth: true } },
      { path: 'following', name: 'Following', component: lazyLoad(() => import('@/pages/Following.vue')), meta: { auth: true } },
      { path: 'cart', name: 'Cart', component: lazyLoad(() => import('@/pages/Cart.vue')), meta: { auth: true } },
      { path: 'address', name: 'AddressManage', component: lazyLoad(() => import('@/pages/AddressManage.vue')), meta: { auth: true } },
      { path: 'payment-config', name: 'PaymentConfig', component: lazyLoad(() => import('@/pages/PaymentConfig.vue')), meta: { auth: true } },
      { path: 'blacklist', name: 'BlacklistManage', component: lazyLoad(() => import('@/pages/BlacklistManage.vue')), meta: { auth: true } },
      { path: 'profile/:id?', name: 'Profile', component: lazyLoad(() => import('@/pages/Profile.vue')), meta: { auth: true } },
      { path: 'order', name: 'Orders', component: lazyLoad(() => import('@/pages/Orders.vue')), meta: { auth: true } },
      { path: 'order/:id', name: 'OrderDetail', component: lazyLoad(() => import('@/pages/OrderDetail.vue')), meta: { auth: true } },
      { path: 'chat/:userId?', name: 'Chat', component: lazyLoad(() => import('@/pages/Chat.vue')), meta: { auth: true } },
      { path: 'notification', name: 'Notification', component: lazyLoad(() => import('@/pages/Notification.vue')), meta: { auth: true } },
      { path: 'report', name: 'Report', component: lazyLoad(() => import('@/pages/Report.vue')), meta: { auth: true } },
      { path: 'my-reports', name: 'MyReports', component: lazyLoad(() => import('@/pages/MyReports.vue')), meta: { auth: true } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: lazyLoad(() => import('@/pages/NotFound.vue'))
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

import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/pages/Login.vue') },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      { path: '', name: 'Dashboard', component: () => import('@/pages/Dashboard.vue') },
      { path: 'user', name: 'UserManage', component: () => import('@/pages/UserManage.vue') },
      { path: 'goods', name: 'GoodsAudit', component: () => import('@/pages/GoodsAudit.vue') },
      { path: 'order', name: 'OrderManage', component: () => import('@/pages/OrderManage.vue') },
      { path: 'report', name: 'ReportAudit', component: () => import('@/pages/ReportAudit.vue') },
      { path: 'log', name: 'LogCenter', component: () => import('@/pages/LogCenter.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, _from, next) => {
  const adminStore = useAdminStore()
  if (to.path !== '/login' && !adminStore.token) {
    next('/login')
  } else if (to.path === '/login' && adminStore.token) {
    next('/')
  } else {
    next()
  }
})

export default router

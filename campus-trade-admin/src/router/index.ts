import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const Layout = () => import('@/layouts/AdminLayout.vue')

const allRoutes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/pages/Login.vue') },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/pages/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'DataAnalysis', permission: '' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/pages/UserManage.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'user:manage' }
      },
      {
        path: 'goods',
        name: 'GoodsAudit',
        component: () => import('@/pages/GoodsAudit.vue'),
        meta: { title: '商品审核', icon: 'Goods', permission: 'goods:audit' }
      },
      {
        path: 'order',
        name: 'OrderManage',
        component: () => import('@/pages/OrderManage.vue'),
        meta: { title: '订单管理', icon: 'List', permission: 'goods:manage' }
      },
      {
        path: 'report',
        name: 'ReportAudit',
        component: () => import('@/pages/ReportAudit.vue'),
        meta: { title: '举报审核', icon: 'Warning', permission: 'report:manage' }
      },
      {
        path: 'log',
        name: 'LogCenter',
        component: () => import('@/pages/LogCenter.vue'),
        meta: { title: '日志中心', icon: 'Document', permission: 'log:manage' }
      }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes: allRoutes })

router.beforeEach(async (to, _from, next) => {
  const adminStore = useAdminStore()
  if (to.path !== '/login' && !adminStore.token) {
    next('/login')
  } else if (to.path === '/login' && adminStore.token) {
    next('/')
  } else {
    if (adminStore.token && adminStore.permissions.length === 0) {
      await adminStore.fetchAdminInfo()
    }
    if (to.meta?.permission && !adminStore.hasPermission(to.meta.permission as string)) {
      next('/')
    } else {
      next()
    }
  }
})

export default router
export { allRoutes }

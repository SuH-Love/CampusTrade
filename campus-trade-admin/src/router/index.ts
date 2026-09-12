import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const lazyLoad = (importFn: () => Promise<Record<string, unknown>>) => {
  return () => importFn().catch((error: Error) => {
    if (error.message?.includes('Failed to fetch dynamically imported module') ||
        error.message?.includes('Importing a module script failed')) {
      window.location.reload()
      return new Promise(() => {})
    }
    throw error
  })
}

const Layout = lazyLoad(() => import('@/layouts/AdminLayout.vue'))

const allRoutes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: lazyLoad(() => import('@/pages/Login.vue')) },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: lazyLoad(() => import('@/pages/Dashboard.vue')),
        meta: { title: '仪表盘', icon: 'Odometer', permission: '' }
      },
      {
        path: 'user',
        name: 'UserManage',
        component: lazyLoad(() => import('@/pages/UserManage.vue')),
        meta: { title: '用户管理', icon: 'User', permission: 'user:manage' }
      },
      {
        path: 'goods',
        name: 'GoodsAudit',
        component: lazyLoad(() => import('@/pages/GoodsAudit.vue')),
        meta: { title: '商品审核', icon: 'Goods', permission: 'goods:audit' }
      },
      {
        path: 'order',
        name: 'OrderManage',
        component: lazyLoad(() => import('@/pages/OrderManage.vue')),
        meta: { title: '订单管理', icon: 'List', permission: 'goods:manage' }
      },
      {
        path: 'report',
        name: 'ReportAudit',
        component: lazyLoad(() => import('@/pages/ReportAudit.vue')),
        meta: { title: '举报审核', icon: 'Warning', permission: 'report:manage' }
      },
      {
        path: 'banner',
        name: 'BannerManage',
        component: lazyLoad(() => import('@/pages/BannerManage.vue')),
        meta: { title: '横幅管理', icon: 'Picture', permission: 'banner:manage' }
      },
      {
        path: 'category',
        name: 'CategoryManage',
        component: lazyLoad(() => import('@/pages/CategoryManage.vue')),
        meta: { title: '分类管理', icon: 'Menu', permission: 'goods:audit' }
      },
      {
        path: 'announcement',
        name: 'AnnouncementManage',
        component: lazyLoad(() => import('@/pages/AnnouncementManage.vue')),
        meta: { title: '公告管理', icon: 'Bell', permission: 'goods:audit' }
      },
      {
        path: 'log',
        name: 'LogCenter',
        component: lazyLoad(() => import('@/pages/LogCenter.vue')),
        meta: { title: '日志中心', icon: 'Document', permission: 'log:manage' }
      },
      {
        path: 'system-config',
        name: 'SystemConfig',
        component: lazyLoad(() => import('@/pages/SystemConfig.vue')),
        meta: { title: '系统配置', icon: 'Setting', permission: 'system:config' }
      },
      {
        path: 'fund-log',
        name: 'FundLogManage',
        component: lazyLoad(() => import('@/pages/FundLogManage.vue')),
        meta: { title: '资金流水', icon: 'Coin', permission: 'fund:manage' }
      },
      {
        path: 'ai-dashboard',
        name: 'AiDashboard',
        component: lazyLoad(() => import('@/pages/AiDashboard.vue')),
        meta: { title: 'AI看板', icon: 'TrendCharts', permission: 'system:config' }
      },
      {
        path: 'faq',
        name: 'FaqManage',
        component: lazyLoad(() => import('@/pages/FaqManage.vue')),
        meta: { title: 'AI知识库', icon: 'ChatLineSquare', permission: 'system:config' }
      },
      {
        path: 'ai-knowledge',
        name: 'AiKnowledge',
        component: lazyLoad(() => import('@/pages/AiKnowledge.vue')),
        meta: { title: '平台知识', icon: 'Collection', permission: 'system:config' }
      },
      {
        path: 'ai-tools',
        name: 'AiTools',
        component: lazyLoad(() => import('@/pages/AiTools.vue')),
        meta: { title: 'AI工具', icon: 'MagicStick', permission: 'system:config' }
      },
      {
        path: 'ai-feedback',
        name: 'AiFeedback',
        component: lazyLoad(() => import('@/pages/AiFeedback.vue')),
        meta: { title: 'AI反馈', icon: 'ChatDotRound', permission: 'system:config' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
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

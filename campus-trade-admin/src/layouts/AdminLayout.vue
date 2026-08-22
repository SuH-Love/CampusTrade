<template>
  <el-container class="admin-layout">
    <el-aside v-if="!isMobile" :width="isCollapse ? '64px' : '240px'" class="sidebar">
      <div class="logo-area">
        <div class="logo-icon" v-if="!isCollapse">C</div>
        <div class="logo-icon small" v-else>C</div>
        <span class="logo-text" v-if="!isCollapse">CampusTrade</span>
      </div>
      <el-menu :default-active="activeMenu" router :collapse="isCollapse" class="sidebar-menu">
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
            <el-badge v-if="item.badge && item.badge > 0" :value="item.badge" :max="99" class="menu-badge" />
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-drawer v-if="isMobile" v-model="drawerVisible" direction="ltr" :size="'260px'" :show-close="false" :with-header="false" class="sidebar-drawer">
      <div class="logo-area">
        <div class="logo-icon">C</div>
        <span class="logo-text">CampusTrade</span>
      </div>
      <el-menu :default-active="activeMenu" router class="sidebar-menu" @select="onMenuSelect">
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
            <el-badge v-if="item.badge && item.badge > 0" :value="item.badge" :max="99" class="menu-badge" />
          </el-menu-item>
        </template>
      </el-menu>
    </el-drawer>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon v-if="isMobile" class="collapse-btn" @click="drawerVisible = true">
            <Expand />
          </el-icon>
          <el-icon v-else class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="header-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <template v-for="item in breadcrumbItems" :key="item.path">
              <el-breadcrumb-item :to="item.path !== route.path ? { path: item.path } : undefined">{{ item.title }}</el-breadcrumb-item>
            </template>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tooltip :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom">
            <el-icon class="header-action-btn" @click="toggleFullscreen">
              <FullScreen v-if="!isFullscreen" /><Aim v-else />
            </el-icon>
          </el-tooltip>
          <el-dropdown>
            <div class="admin-info">
              <el-avatar :size="32" class="admin-avatar">{{ adminStore.username?.[0]?.toUpperCase() || 'A' }}</el-avatar>
              <span>{{ adminStore.nickname || adminStore.username || '管理员' }}</span>
              <el-tag v-if="adminStore.isSuperAdmin" size="small" type="danger" class="super-tag">超管</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="changePasswordDialogVisible = true">修改密码</el-dropdown-item>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main"><router-view /></el-main>
    </el-container>

    <el-dialog v-model="changePasswordDialogVisible" title="修改密码" width="440px" destroy-on-close :close-on-click-modal="false">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="90px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changePasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="changePasswordLoading">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { getDashboardStats, getReportList, updateAdminPassword } from '@/api/admin'
import type { DashboardStats, PageQueryParams } from '@/types'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const activeMenu = computed(() => route.path)
const isCollapse = ref(false)
const isMobile = ref(false)
const drawerVisible = ref(false)
const isFullscreen = ref(false)
const stats = ref<DashboardStats | null>(null)
const reportCount = ref(0)

interface MenuItem {
  path: string
  title: string
  icon: string
  permission: string
  badge?: number
}

const allMenus: MenuItem[] = [
  { path: '/', title: '仪表盘', icon: 'DataAnalysis', permission: '' },
  { path: '/user', title: '用户管理', icon: 'User', permission: 'user:manage' },
  { path: '/goods', title: '商品审核', icon: 'Goods', permission: 'goods:audit' },
  { path: '/category', title: '分类管理', icon: 'Menu', permission: 'goods:audit' },
  { path: '/announcement', title: '公告管理', icon: 'Bell', permission: 'goods:audit' },
  { path: '/order', title: '订单管理', icon: 'List', permission: 'goods:manage' },
  { path: '/report', title: '举报审核', icon: 'Warning', permission: 'report:manage' },
  { path: '/banner', title: '横幅管理', icon: 'Picture', permission: '' },
  { path: '/log', title: '日志中心', icon: 'Document', permission: 'log:manage' },
  { path: '/system-config', title: '系统配置', icon: 'Setting', permission: '' },
  { path: '/fund-log', title: '资金流水', icon: 'Coin', permission: '' }
]

const menuItems = computed(() =>
  allMenus.filter(m => !m.permission || adminStore.hasPermission(m.permission)).map(m => {
    if (m.path === '/goods') return { ...m, badge: stats.value?.pendingAudit || 0 }
    if (m.path === '/report') {
      return { ...m, badge: reportCount.value > 0 ? reportCount.value : undefined }
    }
    return m
  })
)

const breadcrumbItems = computed(() => {
  return route.matched
    .filter(r => r.meta?.title && r.path !== '/')
    .map(r => ({ path: r.path, title: r.meta.title as string }))
})

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) drawerVisible.value = false
}

const onMenuSelect = () => {
  drawerVisible.value = false
}

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
    isFullscreen.value = true
  } else {
    document.exitFullscreen()
    isFullscreen.value = false
  }
}

const onFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement
}

const fetchStats = async () => {
  try {
    stats.value = await getDashboardStats()
  } catch { /* ignore */ }
  try {
    const [pendingRes, processingRes] = await Promise.all([
      getReportList({ pageNum: 1, pageSize: 1, status: 'PENDING' } as PageQueryParams),
      getReportList({ pageNum: 1, pageSize: 1, status: 'PROCESSING' } as PageQueryParams)
    ])
    reportCount.value = (pendingRes.total || 0) + (processingRes.total || 0)
  } catch { /* ignore */ }
}

const handleLogout = () => {
  adminStore.logout()
  router.push('/login')
}

const changePasswordDialogVisible = ref(false)
const changePasswordLoading = ref(false)
const passwordFormRef = ref<{ validate: () => Promise<void> }>()
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPassword = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码不少于6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }, { validator: validateConfirmPassword, trigger: 'blur' }]
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  await passwordFormRef.value.validate()
  changePasswordLoading.value = true
  try {
    await updateAdminPassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    changePasswordDialogVisible.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    handleLogout()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '修改失败'
    ElMessage.error(msg)
  } finally {
    changePasswordLoading.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  fetchStats()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
})
</script>

<style scoped lang="scss">
.admin-layout { min-height: 100vh; }

.sidebar {
  background: var(--admin-sidebar-bg);
  transition: width 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.sidebar-drawer {
  :deep(.el-drawer__body) {
    background: var(--admin-sidebar-bg);
    padding: 0;
  }
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  overflow: hidden;
  white-space: nowrap;
}

.logo-icon {
  width: 36px; height: 36px;
  background: linear-gradient(135deg, var(--admin-primary), var(--admin-primary-light));
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 800; font-size: 18px;
  flex-shrink: 0;
  &.small { width: 32px; height: 32px; font-size: 16px; border-radius: 8px; }
}

.logo-text {
  font-size: 18px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #c7d2fe, #e0e7ff);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}

.sidebar-menu {
  border-right: none !important;
  background: transparent !important;
  padding-top: 8px;
  .el-menu-item {
    color: var(--admin-sidebar-text) !important;
    height: 48px; line-height: 48px;
    margin: 2px 8px; border-radius: 8px;
    &.is-active {
      background: var(--admin-sidebar-active) !important;
      color: var(--admin-sidebar-active-text) !important;
    }
    &:hover { background: rgba(255,255,255,0.06) !important; }
  }
}

.menu-badge {
  :deep(.el-badge__content) {
    font-size: 10px;
    height: 16px;
    line-height: 16px;
    padding: 0 5px;
  }
}

.header {
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid var(--admin-border-light);
  padding: 0 24px;
  height: 60px;
  box-shadow: var(--admin-shadow);
}

.header-left { display: flex; align-items: center; gap: 12px; }

.collapse-btn {
  cursor: pointer; font-size: 20px; color: var(--admin-text-secondary);
  transition: var(--admin-transition);
  &:hover { color: var(--admin-primary); }
}

.header-breadcrumb {
  font-size: 14px;
  :deep(.el-breadcrumb__inner) {
    color: var(--admin-text-secondary);
    &.is-link:hover { color: var(--admin-primary); }
  }
  :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
    color: var(--admin-text);
    font-weight: 600;
  }
}

.header-right { display: flex; align-items: center; gap: 16px; }

.header-action-btn {
  cursor: pointer;
  font-size: 18px;
  color: var(--admin-text-secondary);
  transition: var(--admin-transition);
  &:hover { color: var(--admin-primary); }
}

.admin-info {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 12px; border-radius: 20px;
  transition: var(--admin-transition);
  font-size: 14px; font-weight: 500;
  &:hover { background: var(--admin-bg); }
}

.admin-avatar {
  background: var(--admin-primary);
}

.super-tag { margin-left: 4px; }

.admin-main { background: var(--admin-bg-light); padding: 24px; }

@media (max-width: 768px) {
  .header-breadcrumb { display: none; }
  .header { padding: 0 16px; }
  .admin-main { padding: 16px; }
}
</style>

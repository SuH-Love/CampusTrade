<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '240px'" class="sidebar">
      <div class="logo-area">
        <div class="logo-icon" v-if="!isCollapse">C</div>
        <div class="logo-icon small" v-else>C</div>
        <span class="logo-text" v-if="!isCollapse">CampusTrade</span>
      </div>
      <el-menu :default-active="activeMenu" router :collapse="isCollapse" class="sidebar-menu">
        <template v-for="item in menuItems" :key="item.path">
          <el-menu-item :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon><span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <span class="header-title">管理后台</span>
        </div>
        <div class="header-right">
          <el-dropdown>
            <div class="admin-info">
              <el-avatar :size="32" style="background: var(--admin-primary)">{{ adminStore.username?.[0]?.toUpperCase() || 'A' }}</el-avatar>
              <span>{{ adminStore.nickname || adminStore.username || '管理员' }}</span>
              <el-tag v-if="adminStore.isSuperAdmin" size="small" type="danger" style="margin-left: 4px">超管</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const activeMenu = computed(() => route.path)
const isCollapse = ref(false)

interface MenuItem {
  path: string
  title: string
  icon: string
  permission: string
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
  { path: '/log', title: '日志中心', icon: 'Document', permission: 'log:manage' }
]

const menuItems = computed(() =>
  allMenus.filter(m => !m.permission || adminStore.hasPermission(m.permission))
)

const handleLogout = () => {
  adminStore.logout()
  router.push('/login')
}
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

.header {
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid var(--admin-border);
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

.header-title { font-size: 16px; font-weight: 600; color: var(--admin-text); }

.header-right { display: flex; align-items: center; }

.admin-info {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer; padding: 4px 12px; border-radius: 20px;
  transition: var(--admin-transition);
  font-size: 14px; font-weight: 500;
  &:hover { background: var(--admin-bg); }
}

.admin-main { background: var(--admin-bg); padding: 24px; }
</style>

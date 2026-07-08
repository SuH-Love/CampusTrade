<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">C</div>
          <span class="logo-text">CampusTrade</span>
        </div>
        <el-menu mode="horizontal" :default-active="activeMenu" router class="nav-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/goods">商品</el-menu-item>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.token">
            <el-menu mode="horizontal" :default-active="activeMenu" router class="user-menu">
              <el-menu-item index="/my-goods">我的商品</el-menu-item>
              <el-menu-item index="/order">订单</el-menu-item>
              <el-menu-item index="/favorites">收藏</el-menu-item>
            </el-menu>
            <el-dropdown>
              <div class="user-info">
                <el-avatar :size="34" :src="userStore.userInfo?.avatar" />
                <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/chat')">
                    <el-icon><ChatDotRound /></el-icon>聊天
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/notification')">
                    <el-icon><Bell /></el-icon>通知
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/my-reports')">
                    <el-icon><Warning /></el-icon>我的举报
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round @click="$router.push('/login')">登录</el-button>
            <el-button round @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="main-content">
      <router-view :key="route.fullPath" />
    </el-main>
    <el-footer class="footer">
      <span>CampusTrade 校园二手交易平台</span>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)

const handleLogout = async () => {
  await userStore.logout()
  location.href = '/login'
}
</script>

<style scoped lang="scss">
.main-layout { min-height: 100vh; display: flex; flex-direction: column; }

.header {
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 8px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin-right: 16px;
  flex-shrink: 0;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-size: 18px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.nav-menu {
  flex-shrink: 0;
  .el-menu-item {
    font-size: 15px;
    font-weight: 500;
    height: 64px;
    line-height: 64px;
    border-bottom: 2px solid transparent !important;
    &.is-active { border-bottom-color: var(--primary) !important; color: var(--primary) !important; }
  }
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-menu {
  flex-shrink: 0;
  .el-menu-item {
    font-size: 14px;
    height: 64px;
    line-height: 64px;
    border-bottom: 2px solid transparent !important;
    &.is-active { border-bottom-color: var(--primary) !important; color: var(--primary) !important; }
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: var(--radius-xl);
  transition: var(--transition);
  &:hover { background: var(--bg-hover); }
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.main-content {
  flex: 1;
  padding: 0;
  background: var(--bg-page);
}

.footer {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  height: 48px;
  line-height: 48px;
  padding: 0;
}
</style>

<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">C</div>
          <span class="logo-text">CampusTrade</span>
        </div>
        <nav class="nav-links">
          <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">首页</router-link>
          <router-link to="/goods" class="nav-link" :class="{ active: route.path.startsWith('/goods') }">商品</router-link>
          <template v-if="userStore.token">
            <router-link to="/my-goods" class="nav-link" :class="{ active: route.path === '/my-goods' }">我的商品</router-link>
            <router-link to="/order" class="nav-link" :class="{ active: route.path.startsWith('/order') }">订单</router-link>
            <router-link to="/favorites" class="nav-link" :class="{ active: route.path === '/favorites' }">收藏</router-link>
          </template>
        </nav>
        <div class="header-right">
          <template v-if="userStore.token">
            <el-badge :value="chatUnread || undefined" :hidden="!chatUnread" :max="99" class="header-badge">
              <router-link to="/chat" class="icon-btn" title="聊天">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </router-link>
            </el-badge>
            <el-badge :value="notifyCount || undefined" :hidden="!notifyCount" :max="99" class="header-badge">
              <router-link to="/notification" class="icon-btn" title="通知">
                <el-icon :size="20"><Bell /></el-icon>
              </router-link>
            </el-badge>
            <el-dropdown>
              <div class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar" />
                <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <el-icon><User /></el-icon>个人中心
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
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUnreadCount as getNotifyUnread } from '@/api/notification'
import { useChatWs } from '@/composables/useChatWs'

const route = useRoute()
const userStore = useUserStore()
const notifyCount = ref(0)
const chatUnread = ref(0)
const { wsUnreadCount } = useChatWs()

const fetchCounts = async () => {
  if (!userStore.token) return
  try { const r = await getNotifyUnread(); notifyCount.value = typeof r === 'number' ? r : 0 } catch { /* */ }
}

watch(wsUnreadCount, (v) => { chatUnread.value = v }, { immediate: true })

const handleLogout = async () => {
  await userStore.logout()
  location.href = '/login'
}

onMounted(fetchCounts)
</script>

<style scoped lang="scss">
.main-layout { min-height: 100vh; display: flex; flex-direction: column; }

.header {
  background: var(--bg-card);
  border-bottom: 1px solid var(--border);
  padding: 0;
  height: 60px;
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
  margin-right: 24px;
  flex-shrink: 0;
}

.logo-icon {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-size: 17px;
}

.logo-text {
  font-size: 19px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 6px 16px;
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition);
  white-space: nowrap;
  &:hover { color: var(--primary); background: var(--primary-lighter); }
  &.active { color: var(--primary); background: var(--primary-lighter); font-weight: 600; }
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.header-badge { display: flex; align-items: center; }

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition);
  &:hover { color: var(--primary); background: var(--primary-lighter); }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
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

<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/')">
          <div class="logo-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          </div>
          <span class="logo-text">CampusTrade</span>
        </div>
        <nav class="nav-links">
          <router-link to="/" class="nav-link" :class="{ active: route.path === '/' }">
            <el-icon :size="16"><HomeFilled /></el-icon><span>首页</span>
          </router-link>
          <router-link to="/goods" class="nav-link" :class="{ active: route.path.startsWith('/goods') }">
            <el-icon :size="16"><Goods /></el-icon><span>商品</span>
          </router-link>
          <template v-if="userStore.token">
            <div class="nav-divider" />
            <router-link to="/my-goods" class="nav-link nav-link--user" :class="{ active: route.path === '/my-goods' }">
              <el-icon :size="16"><Box /></el-icon><span>我的商品</span>
            </router-link>
            <router-link to="/order" class="nav-link nav-link--user" :class="{ active: route.path.startsWith('/order') }">
              <el-icon :size="16"><List /></el-icon><span>订单</span>
            </router-link>
            <router-link to="/favorites" class="nav-link nav-link--user" :class="{ active: route.path === '/favorites' }">
              <el-icon :size="16"><Star /></el-icon><span>收藏</span>
            </router-link>
            <router-link to="/following" class="nav-link nav-link--user" :class="{ active: route.path === '/following' }">
              <el-icon :size="16"><UserFilled /></el-icon><span>关注</span>
            </router-link>
          </template>
        </nav>
        <div class="header-right">
          <template v-if="userStore.token">
            <el-badge :value="cartStore.cartCount || ''" :hidden="!cartStore.cartCount" class="header-badge">
              <router-link to="/cart" class="icon-btn" title="购物车">
                <el-icon :size="20"><ShoppingCart /></el-icon>
              </router-link>
            </el-badge>
            <el-badge :is-dot="!!chatUnread" class="header-badge">
              <router-link to="/chat" class="icon-btn" title="聊天">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </router-link>
            </el-badge>
            <el-badge :is-dot="!!notifyUnread" class="header-badge">
              <router-link to="/notification" class="icon-btn" title="通知">
                <el-icon :size="20"><Bell /></el-icon>
              </router-link>
            </el-badge>
            <el-dropdown>
              <div class="user-info">
                <el-avatar :size="34" :src="userStore.userInfo?.avatar || '/default-avatar.svg'" />
                <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                <el-icon :size="14"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="$router.push('/profile')">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/my-reports')">
                    <el-icon><Warning /></el-icon>我的举报
                  </el-dropdown-item>
                  <el-dropdown-item @click="$router.push('/address')">
                    <el-icon><Location /></el-icon>收货地址
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
      <div class="footer-inner">
        <div class="footer-brand">
          <div class="footer-logo">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
          </div>
          <span>CampusTrade</span>
        </div>
        <div class="footer-links">
          <router-link to="/goods">商品市场</router-link>
          <router-link to="/goods/publish">发布商品</router-link>
          <router-link to="/about">关于我们</router-link>
        </div>
        <div class="footer-copy">&copy; 2026 CampusTrade 校园二手交易平台 · 安全 · 便捷 · 值得信赖</div>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { getUnreadCount as getNotifyUnread } from '@/api/notification'
import { useChatWs } from '@/composables/useChatWs'

const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const { chatUnread, notifyUnread, onNotification } = useChatWs()

let pollTimer: ReturnType<typeof setInterval> | null = null

const fetchNotifyCount = async () => {
  if (!userStore.token) return
  try { const r = await getNotifyUnread(); if (typeof r === 'number') notifyUnread.value = r } catch { /* */ }
}

const startPolling = () => {
  fetchNotifyCount()
  cartStore.fetchCartCount()
  pollTimer = setInterval(() => { fetchNotifyCount(); cartStore.fetchCartCount() }, 60000)
}

const stopPolling = () => {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

watch(() => userStore.token, (token) => {
  if (token) startPolling(); else { stopPolling(); notifyUnread.value = 0; cartStore.cartCount = 0 }
}, { immediate: true })

const removeNotifyHandler = onNotification(() => {
  fetchNotifyCount()
})

const handleLogout = async () => {
  stopPolling()
  await userStore.logout()
  location.href = '/login'
}

onUnmounted(() => {
  stopPolling()
  removeNotifyHandler()
})
</script>

<style scoped lang="scss">
.main-layout { min-height: 100vh; display: flex; flex-direction: column; }

.header {
  background: var(--bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.5);
  padding: 0;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 12px rgba(0, 0, 0, 0.04);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  padding: 0 28px;
  gap: 8px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  margin-right: 28px;
  flex-shrink: 0;
  transition: var(--transition);
  &:hover { transform: scale(1.02); }
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: var(--primary-gradient);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.logo-text {
  font-size: 19px;
  font-weight: 800;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
  letter-spacing: -0.3px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition-fast);
  white-space: nowrap;
  &:hover { color: var(--primary); background: var(--primary-lighter); }
  &.active {
    color: var(--primary);
    background: var(--primary-lighter);
    font-weight: 600;
    box-shadow: inset 0 0 0 1px rgba(99, 102, 241, 0.15);
  }
  &.nav-link--user { color: var(--text-muted); }
  &.nav-link--user:hover { color: var(--primary); }
  &.nav-link--user.active { color: var(--primary); }
}

.nav-divider {
  width: 1px;
  height: 20px;
  background: var(--border);
  margin: 0 6px;
  flex-shrink: 0;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.header-badge { display: flex; align-items: center; }

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition-fast);
  &:hover { color: var(--primary); background: var(--primary-lighter); transform: translateY(-1px); }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 14px 4px 4px;
  border-radius: var(--radius-xl);
  transition: var(--transition-fast);
  &:hover { background: var(--primary-lighter); }
}

.user-name {
  font-size: 14px;
  font-weight: 600;
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
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  height: auto;
  padding: 0;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 28px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.footer-logo {
  width: 28px;
  height: 28px;
  background: var(--primary-gradient);
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.footer-links {
  display: flex;
  gap: 24px;
  a {
    font-size: 13px;
    color: var(--text-secondary);
    font-weight: 500;
    &:hover { color: var(--primary); }
  }
}

.footer-copy {
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}
</style>

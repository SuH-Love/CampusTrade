<template>
  <el-container class="main-layout">
    <el-header class="header">
      <div class="logo" @click="$router.push('/')">CampusTrade</div>
      <el-menu mode="horizontal" :default-active="activeMenu" router>
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
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" />
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/chat')">聊天</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/notification')">通知</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push('/login')">登录</el-button>
          <el-button @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
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
.main-layout { min-height: 100vh; }
.header {
  display: flex; align-items: center; background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); padding: 0 20px;
  .logo { font-size: 20px; font-weight: bold; color: #409eff; cursor: pointer; margin-right: 20px; white-space: nowrap; }
  .header-right {
    margin-left: auto; display: flex; align-items: center; gap: 4px;
    .user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; white-space: nowrap; }
  }
}
.user-menu { border-bottom: none; }
</style>

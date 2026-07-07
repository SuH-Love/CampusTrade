<template>
  <el-container class="admin-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" style="transition: width 0.3s">
      <div class="logo">CampusTrade Admin</div>
      <el-menu :default-active="activeMenu" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff" :collapse="isCollapse">
        <el-menu-item index="/"><el-icon><DataAnalysis /></el-icon><span>仪表盘</span></el-menu-item>
        <el-menu-item index="/user"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/goods"><el-icon><Goods /></el-icon><span>商品审核</span></el-menu-item>
        <el-menu-item index="/order"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/report"><el-icon><Warning /></el-icon><span>举报审核</span></el-menu-item>
        <el-menu-item index="/log"><el-icon><Document /></el-icon><span>日志中心</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="isCollapse = !isCollapse"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
        <span>CampusTrade 管理后台</span>
        <el-dropdown style="margin-left: auto">
          <span class="admin-info">{{ adminStore.username || '管理员' }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
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

const handleLogout = () => {
  adminStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.admin-layout { min-height: 100vh; }
.el-aside { background: #304156; transition: width 0.3s; overflow: hidden; .logo { color: #fff; text-align: center; padding: 20px; font-size: 18px; font-weight: bold; white-space: nowrap; overflow: hidden; } }
.header { display: flex; align-items: center; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
.collapse-btn { cursor: pointer; font-size: 20px; }
.admin-info { cursor: pointer; }
</style>

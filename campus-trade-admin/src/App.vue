<template><router-view /></template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const adminStore = useAdminStore()
const router = useRouter()

onMounted(() => {
  if (!adminStore.token) {
    adminStore.clearAuth()
    return
  }

  const SESSION_TIMEOUT = 2 * 60 * 60 * 1000
  let timeoutId: ReturnType<typeof setTimeout>

  const resetTimeout = () => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => {
      if (adminStore.token) {
        adminStore.clearAuth()
        router.push('/login')
        ElMessage.warning('长时间未操作，请重新登录')
      }
    }, SESSION_TIMEOUT)
  }

  document.addEventListener('click', resetTimeout, { passive: true })
  document.addEventListener('keydown', resetTimeout, { passive: true })
  resetTimeout()
})
</script>

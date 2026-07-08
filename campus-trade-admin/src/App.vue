<template><router-view /></template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAdminStore } from '@/stores/admin'

const adminStore = useAdminStore()

onMounted(() => {
  if (!adminStore.token) {
    adminStore.clearAuth()
  }
})
</script>

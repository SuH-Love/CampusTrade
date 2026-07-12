<template>
  <div class="empty-state">
    <div class="empty-icon">{{ icon }}</div>
    <div class="empty-title" v-if="title">{{ title }}</div>
    <div class="empty-desc" v-if="description">{{ description }}</div>
    <el-button v-if="actionText" type="primary" round @click="$emit('action')">{{ actionText }}</el-button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  title?: string
  description?: string
  actionText?: string
}>()

defineEmits<{
  action: []
}>()
</script>

<style scoped lang="scss">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  filter: grayscale(0.2);
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 20px;
  max-width: 300px;
}
</style>
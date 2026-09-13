<template>
  <el-dialog v-model="visible" width="400px" :show-close="true" append-to-body align-center class="verify-guide-dialog">
    <div class="verify-guide-body">
      <div class="verify-guide-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-1 6h2v2h-2V7zm0 4h2v6h-2v-6z"/></svg>
      </div>
      <h3 class="verify-guide-title">{{ title }}</h3>
      <p class="verify-guide-desc">{{ description }}</p>
      <el-button type="primary" round @click="goVerify">前往认证</el-button>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  description?: string
}>(), {
  title: '需要实名认证',
  description: '为保障校园交易安全，发布商品前请先完成实名认证'
})

const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const router = useRouter()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const goVerify = () => { visible.value = false; router.push('/profile') }
</script>

<style scoped lang="scss">
.verify-guide-body {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 16px 8px 8px; text-align: center; gap: 12px;
}
.verify-guide-icon { color: var(--warning); }
.verify-guide-title { font-size: 18px; font-weight: 700; margin: 0; }
.verify-guide-desc { color: var(--text-secondary); font-size: 14px; margin: 0; line-height: 1.6; }
:deep(.verify-guide-dialog) {
  border-radius: var(--radius-lg) !important;
  .el-dialog__header { padding: 0; }
  .el-dialog__body { padding: 24px; }
}
</style>
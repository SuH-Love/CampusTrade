<template>
  <div class="goods-publish page-bg">
    <div class="page-container">
      <el-card v-if="userStore.userInfo && userStore.userInfo.realVerified !== 1" class="verify-guide-card">
        <div class="verify-guide">
          <div class="verify-guide-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-1 6h2v2h-2V7zm0 4h2v6h-2v-6z"/></svg>
          </div>
          <h3 class="verify-guide-title">需要实名认证</h3>
          <p class="verify-guide-desc">为保障校园交易安全，发布商品前请先完成实名认证</p>
          <el-button type="primary" round @click="$router.push('/profile?tab=verify')">前往认证</el-button>
        </div>
      </el-card>
      <el-card v-else>
        <template #header>
          <div class="detail-header">
            <h3 class="m-0">发布商品</h3>
            <el-button @click="$router.back()">取消</el-button>
          </div>
        </template>
        <GoodsForm ref="goodsFormRef" :loading="submitting" @submit="handleSubmit" @cancel="handleCancel" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { createGoods } from '@/api/goods'
import { ElMessage } from 'element-plus'
import type { GoodsCreateParams } from '@/types'
import GoodsForm from '@/components/GoodsForm.vue'

const router = useRouter()
const userStore = useUserStore()
const submitting = ref(false)
const goodsFormRef = ref<{ clearDraft: () => void }>()

const handleSubmit = async (data: GoodsCreateParams) => {
  submitting.value = true
  try {
    await createGoods(data)
    ElMessage.success('发布成功，可在"我的商品"中提交审核')
    goodsFormRef.value?.clearDraft()
    router.push('/my-goods')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.goods-publish {
  min-height: calc(100vh - 60px);
}
.detail-header { display: flex; justify-content: space-between; align-items: center; }
.verify-guide-card {
  :deep(.el-card__body) { padding: 48px 32px; }
}
.verify-guide {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  text-align: center; gap: 16px;
}
.verify-guide-icon { color: var(--warning); }
.verify-guide-title { font-size: 18px; font-weight: 700; margin: 0; }
.verify-guide-desc { color: var(--text-secondary); font-size: 14px; margin: 0; line-height: 1.6; white-space: nowrap; }
</style>

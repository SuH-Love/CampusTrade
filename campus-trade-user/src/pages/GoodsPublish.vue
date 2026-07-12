<template>
  <div class="goods-publish page-bg">
    <div class="page-container">
      <el-card>
        <h2 class="section-title">发布商品</h2>
        <GoodsForm ref="goodsFormRef" :loading="submitting" @submit="handleSubmit" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createGoods } from '@/api/goods'
import { ElMessage } from 'element-plus'
import type { GoodsCreateParams } from '@/types'
import GoodsForm from '@/components/GoodsForm.vue'

const router = useRouter()
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
</script>

<style scoped lang="scss">
.goods-publish {
  min-height: calc(100vh - 60px);
}
</style>

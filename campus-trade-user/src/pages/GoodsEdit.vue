<template>
  <div class="goods-edit page-bg">
    <div class="page-container">
      <el-card>
        <template #header>
          <div class="detail-header">
            <h3 class="m-0">编辑商品</h3>
            <el-button @click="$router.back()">取消</el-button>
          </div>
        </template>
        <GoodsForm
          v-if="initialData"
          :initialData="initialData"
          :loading="submitting"
          @submit="handleSubmit"
          @cancel="handleCancel"
        />
        <el-skeleton v-else :rows="10" animated />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, updateGoods } from '@/api/goods'
import { ElMessage } from 'element-plus'
import type { GoodsCreateParams } from '@/types'
import GoodsForm from '@/components/GoodsForm.vue'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const initialData = ref<Partial<GoodsCreateParams>>()

const handleSubmit = async (data: GoodsCreateParams) => {
  submitting.value = true
  try {
    await updateGoods(Number(route.params.id), data)
    ElMessage.success('修改成功')
    router.push('/my-goods')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(async () => {
  try {
    const goods = await getGoodsDetail(Number(route.params.id))
    initialData.value = {
      title: goods.title,
      categoryId: goods.categoryId,
      description: goods.description,
      price: goods.price,
      originalPrice: goods.originalPrice,
      coverImage: goods.coverImage,
      images: goods.images,
      condition: goods.condition,
      stock: goods.stock
    }
  } catch {
    ElMessage.error('商品信息加载失败')
  }
})
</script>

<style scoped lang="scss">
.goods-edit {
  min-height: calc(100vh - 60px);
}
.detail-header { display: flex; justify-content: space-between; align-items: center; }
</style>

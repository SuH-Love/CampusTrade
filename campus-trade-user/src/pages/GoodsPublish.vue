<template>
  <div class="goods-publish">
    <el-card>
      <h2>发布商品</h2>
      <el-form :model="form" label-width="80px" @submit.prevent="handleSubmit">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类"><el-input-number v-model="form.categoryId" :min="1" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="售价"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="封面图"><el-input v-model="form.coverImage" placeholder="图片URL" /></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSubmit">发布</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { createGoods } from '@/api/goods'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = reactive({ title: '', categoryId: 1, description: '', price: 0, originalPrice: 0, coverImage: '' })

const handleSubmit = async () => {
  await createGoods(form)
  ElMessage.success('发布成功')
  router.push('/goods')
}
</script>
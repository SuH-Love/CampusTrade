<template>
  <div class="goods-edit">
    <el-card>
      <h2>编辑商品</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" @submit.prevent="handleSubmit">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="售价" prop="price"><el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="成色" prop="condition">
          <el-select v-model="form.condition" placeholder="请选择成色" style="width: 100%">
            <el-option label="九九新" value="九九新" />
            <el-option label="九五新" value="九五新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八五新" value="八五新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新" value="七成新" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSubmit" :loading="submitting">保存修改</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, updateGoods } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import type { GoodsCategory } from '@/api/category'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const categories = ref<GoodsCategory[]>([])

const form = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  description: '',
  price: 0,
  originalPrice: 0,
  condition: ''
})

const rules = {
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  condition: [{ required: true, message: '请选择成色', trigger: 'change' }]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await updateGoods(Number(route.params.id), form)
    ElMessage.success('修改成功')
    router.push('/my-goods')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const [goods, cats] = await Promise.all([getGoodsDetail(Number(route.params.id)), getCategoryList()])
  categories.value = cats || []
  form.title = goods.title
  form.categoryId = goods.categoryId
  form.description = goods.description
  form.price = goods.price
  form.originalPrice = goods.originalPrice
  form.condition = goods.condition || ''
})
</script>

<style scoped lang="scss">
.goods-edit { padding: 20px; }
</style>
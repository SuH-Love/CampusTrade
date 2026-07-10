<template>
  <div class="goods-publish">
    <el-card>
      <h2>发布商品</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" @submit.prevent="handleSubmit">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" placeholder="请输入商品标题" /></el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述商品详情" /></el-form-item>
        <el-form-item label="售价" prop="price"><el-input-number v-model="form.price" :min="0.01" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="成色" prop="condition">
          <el-select v-model="form.condition" placeholder="请选择成色" style="width: 100%">
            <el-option label="全新" value="全新" />
            <el-option label="九九新" value="九九新" />
            <el-option label="九五新" value="九五新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八五新" value="八五新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新" value="七成新" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存数量"><el-input-number v-model="form.stock" :min="1" :max="9999" style="width: 100%" /></el-form-item>
        <el-form-item label="封面图">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="handleCoverSuccess" :before-upload="beforeUpload" list-type="picture" :limit="1" :file-list="coverFileList">
            <el-button size="small">上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :on-success="handleImageSuccess" :before-upload="beforeUpload" list-type="picture-card" :limit="5" :file-list="imageFileList">
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSubmit" :loading="submitting">发布商品</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { createGoods } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import type { GoodsCategory } from '@/api/category'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { UploadResponse } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const categories = ref<GoodsCategory[]>([])

const form = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  description: '',
  price: 0,
  originalPrice: 0,
  coverImage: '',
  images: '',
  condition: '',
  stock: 1
})

const rules = {
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  condition: [{ required: true, message: '请选择成色', trigger: 'change' }]
}

const uploadUrl = '/api/file/upload'
const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

interface FileItem {
  name: string
  url: string
}

const coverFileList = ref<FileItem[]>([])
const imageFileList = ref<FileItem[]>([])
const imageUrls = ref<string[]>([])

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt5M) { ElMessage.error('图片大小不能超过5MB'); return false }
  return true
}

const handleCoverSuccess = (response: UploadResponse) => {
  if (response.code === 200) {
    form.coverImage = response.data
    coverFileList.value = [{ name: 'cover', url: response.data }]
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleImageSuccess = (response: UploadResponse) => {
  if (response.code === 200) {
    imageUrls.value.push(response.data)
    form.images = imageUrls.value.join(',')
    imageFileList.value.push({ name: response.data, url: response.data })
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (form.price <= 0) { ElMessage.error('售价必须大于0'); return }
  if (!form.categoryId) { ElMessage.error('请选择分类'); return }
  submitting.value = true
  try {
    await createGoods({ ...form, categoryId: form.categoryId })
    ElMessage.success('发布成功，可在"我的商品"中提交审核')
    router.push('/my-goods')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getCategoryList()
    categories.value = res || []
  } catch { /* ignore */ }
})
</script>

<style scoped lang="scss">
.goods-publish { padding: 20px; }
</style>

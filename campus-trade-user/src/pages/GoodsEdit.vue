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
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleCoverSuccess" accept="image/*">
            <img v-if="form.coverImage" :src="form.coverImage" style="width: 120px; height: 120px; object-fit: cover; border-radius: 8px; border: 1px solid #e2e8f0" />
            <el-button v-else size="small">上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" :show-file-list="false" :on-success="handleImageSuccess" accept="image/*" multiple>
            <el-button size="small">添加图片</el-button>
          </el-upload>
          <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px">
            <div v-for="(img, idx) in imageList" :key="idx" style="position: relative">
              <img :src="img" style="width: 80px; height: 80px; object-fit: cover; border-radius: 6px; border: 1px solid #e2e8f0" />
              <el-button type="danger" size="small" circle style="position: absolute; top: -6px; right: -6px" @click="removeImage(idx)">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleSubmit" :loading="submitting">保存修改</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, updateGoods } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import type { GoodsCategory } from '@/api/category'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref<{ validate: () => Promise<void> } | null>(null)
const submitting = ref(false)
const categories = ref<GoodsCategory[]>([])
const imageList = ref<string[]>([])

const uploadUrl = '/api/file/upload'
const uploadHeaders = computed(() => userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {})

const form = reactive({
  title: '',
  categoryId: undefined as number | undefined,
  description: '',
  price: 0,
  originalPrice: 0,
  condition: '',
  stock: 1,
  coverImage: '',
  images: ''
})

const handleCoverSuccess = (response: { data: string }) => {
  if (response.data) form.coverImage = response.data
}

const handleImageSuccess = (response: { data: string }) => {
  if (response.data) imageList.value.push(response.data)
}

const removeImage = (idx: number) => {
  imageList.value.splice(idx, 1)
}

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
    await updateGoods(Number(route.params.id), { ...form, images: imageList.value.join(',') })
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
  form.stock = goods.stock || 1
  form.coverImage = goods.coverImage || ''
  imageList.value = goods.images ? goods.images.split(',').filter(Boolean) : []
})
</script>

<style scoped lang="scss">
.goods-edit { padding: 20px; }
</style>
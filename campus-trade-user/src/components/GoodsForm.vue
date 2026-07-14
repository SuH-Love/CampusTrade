<template>
  <div class="goods-form">
    <el-steps :active="currentStep" finish-status="success" align-center class="goods-form-steps">
      <el-step title="基本信息" />
      <el-step title="图片上传" />
      <el-step title="确认预览" />
    </el-steps>

    <div class="step-content">
      <el-form
        v-show="currentStep === 0"
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        class="step-form"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入商品标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" class="full-width">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成色" prop="condition">
          <el-select v-model="form.condition" placeholder="请选择成色" class="full-width">
            <el-option v-for="opt in CONDITION_OPTIONS" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" class="full-width" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" class="full-width" />
        </el-form-item>
        <el-form-item label="库存数量">
          <el-input-number v-model="form.stock" :min="1" :max="9999" class="full-width" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请描述商品详情" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <div v-show="currentStep === 1" class="step-upload">
        <div class="upload-section">
          <div class="upload-label">封面图</div>
          <el-upload
            action="#"
            :http-request="handleCoverUpload"
            :before-upload="beforeUpload"
            :show-file-list="false"
            accept="image/*"
            class="cover-uploader"
          >
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" alt="商品图片" />
            <div v-else class="upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <span>上传封面</span>
            </div>
          </el-upload>
          <el-button v-if="form.coverImage" link type="danger" class="remove-cover-btn" @click="form.coverImage = ''">移除封面</el-button>
        </div>

        <div class="upload-section">
          <div class="upload-label">商品图片 <span class="upload-hint">（最多5张）</span></div>
          <div class="image-grid">
            <div v-for="(url, idx) in imageUrls" :key="idx" class="image-item">
              <img :src="url" class="image-thumb" alt="商品图片" />
              <button class="image-remove" @click="removeImage(idx)">
                <el-icon><Close /></el-icon>
              </button>
            </div>
            <el-upload
              v-if="imageUrls.length < 5"
              action="#"
              :http-request="handleImageUpload"
              :before-upload="beforeUpload"
              :show-file-list="false"
              accept="image/*"
              multiple
              class="image-add"
            >
              <div class="upload-placeholder upload-placeholder--small">
                <el-icon class="upload-icon"><Plus /></el-icon>
              </div>
            </el-upload>
          </div>
        </div>
      </div>

      <div v-show="currentStep === 2" class="step-preview">
        <div class="preview-card">
          <div class="preview-cover-wrap">
            <img :src="form.coverImage || '/default-cover.svg'" class="preview-cover-img" alt="商品图片" />
            <div class="preview-tags">
              <span v-if="categoryName" class="preview-tag preview-tag--category">{{ categoryName }}</span>
              <span v-if="form.condition" class="preview-tag preview-tag--condition">{{ form.condition }}</span>
            </div>
          </div>
          <div class="preview-body">
            <div class="preview-title">{{ form.title || '未填写标题' }}</div>
            <div class="preview-desc">{{ form.description || '未填写描述' }}</div>
            <div class="preview-price-row">
              <span class="price-text">¥{{ form.price }}</span>
              <span v-if="form.originalPrice && form.originalPrice > form.price" class="original-price">¥{{ form.originalPrice }}</span>
            </div>
            <div class="preview-stock">库存: {{ form.stock }}</div>
            <div v-if="imageUrls.length" class="preview-images">
              <img v-for="url in imageUrls" :key="url" :src="url" class="preview-img-thumb" alt="商品图片" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="step-actions">

      <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
      <div class="step-actions-right">
        <span v-if="draftSaved" class="draft-saved-hint">已自动保存</span>
        <el-button v-if="currentStep < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentStep === 2" type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认发布' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Plus, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
type UploadRequestOptions = { file: File; onProgress: (e: { percent: number }) => void; onSuccess: (response: unknown) => void; onError: (e: unknown) => void }
import { getCategoryList } from '@/api/category'
import type { GoodsCategory } from '@/api/category'
import { uploadImage } from '@/api/file'
import type { GoodsCreateParams } from '@/types'

const CONDITION_OPTIONS = ['全新', '九九新', '九五新', '九成新', '八五新', '八成新', '七成新'] as const
const DRAFT_KEY = 'goods-draft-publish'
const DRAFT_INTERVAL = 30000

interface GoodsFormProps {
  initialData?: Partial<GoodsCreateParams>
  loading?: boolean
}

const props = withDefaults(defineProps<GoodsFormProps>(), {
  loading: false
})

const emit = defineEmits<{
  submit: [data: GoodsCreateParams]
  cancel: []
}>()

const formRef = ref<FormInstance>()
const currentStep = ref(0)
const categories = ref<GoodsCategory[]>([])
const imageUrls = ref<string[]>(
  props.initialData?.images
    ? props.initialData.images.split(',').filter(Boolean)
    : []
)

const isEdit = computed(() => !!props.initialData)

const form = reactive({
  title: props.initialData?.title ?? '',
  categoryId: props.initialData?.categoryId as number | undefined,
  description: props.initialData?.description ?? '',
  price: props.initialData?.price ?? 0,
  originalPrice: props.initialData?.originalPrice ?? 0,
  coverImage: props.initialData?.coverImage ?? '',
  condition: props.initialData?.condition ?? '',
  stock: props.initialData?.stock ?? 1
})

const rules = {
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  condition: [{ required: true, message: '请选择成色', trigger: 'change' }]
}

const categoryName = computed(() => {
  const cat = categories.value.find(c => c.id === form.categoryId)
  return cat?.categoryName ?? ''
})

const beforeUpload = (rawFile: File) => {
  if (!rawFile.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过5MB')
    return false
  }
  return true
}

const handleCoverUpload = async (options: UploadRequestOptions) => {
  try {
    const url = await uploadImage(options.file)
    form.coverImage = url
  } catch {
    ElMessage.error('封面图上传失败')
  }
}

const handleImageUpload = async (options: UploadRequestOptions) => {
  try {
    const url = await uploadImage(options.file)
    imageUrls.value.push(url)
  } catch {
    ElMessage.error('图片上传失败')
  }
}

const removeImage = (idx: number) => {
  imageUrls.value.splice(idx, 1)
}

const nextStep = async () => {
  if (currentStep.value === 0) {
    try {
      await formRef.value?.validate()
    } catch {
      return
    }
  }
  if (currentStep.value < 2) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handleCancel = () => {
  emit('cancel')
}

const handleSubmit = () => {
  if (form.price <= 0) {
    ElMessage.error('售价必须大于0')
    return
  }
  emit('submit', {
    title: form.title,
    categoryId: form.categoryId!,
    description: form.description,
    price: form.price,
    originalPrice: form.originalPrice > 0 ? form.originalPrice : undefined,
    coverImage: form.coverImage || undefined,
    images: imageUrls.value.join(',') || undefined,
    condition: form.condition || undefined,
    stock: form.stock
  })
}

let draftTimer: ReturnType<typeof setInterval> | null = null
const draftSaved = ref(false)

const saveDraft = () => {
  if (isEdit.value) return
  localStorage.setItem(DRAFT_KEY, JSON.stringify({
    title: form.title,
    categoryId: form.categoryId,
    description: form.description,
    price: form.price,
    originalPrice: form.originalPrice,
    coverImage: form.coverImage,
    imageUrls: imageUrls.value,
    condition: form.condition,
    stock: form.stock
  }))
  draftSaved.value = true
  setTimeout(() => { draftSaved.value = false }, 3000)
}

const loadDraft = () => {
  if (isEdit.value) return
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const draft = JSON.parse(raw)
    if (draft.title) form.title = draft.title
    if (draft.categoryId) form.categoryId = draft.categoryId
    if (draft.description) form.description = draft.description
    if (draft.price) form.price = draft.price
    if (draft.originalPrice) form.originalPrice = draft.originalPrice
    if (draft.coverImage) form.coverImage = draft.coverImage
    if (draft.imageUrls?.length) imageUrls.value = draft.imageUrls
    if (draft.condition) form.condition = draft.condition
    if (draft.stock) form.stock = draft.stock
  } catch { /* ignore */ }
}

const clearDraft = () => {
  localStorage.removeItem(DRAFT_KEY)
}

onMounted(async () => {
  try {
    categories.value = await getCategoryList() ?? []
  } catch { /* ignore */ }

  if (!isEdit.value) {
    loadDraft()
    draftTimer = setInterval(saveDraft, DRAFT_INTERVAL)
  }
})

onUnmounted(() => {
  if (draftTimer) {
    clearInterval(draftTimer)
    draftTimer = null
  }
})

defineExpose({ clearDraft })
</script>

<style scoped lang="scss">
.goods-form {
  --el-color-primary: var(--primary);
}

.goods-form-steps {
  margin-bottom: var(--spacing-xl);
}

.step-content {
  min-height: 400px;
}

.step-form {
  max-width: 600px;
  margin: 0 auto;
}

.full-width {
  width: 100%;
}

.step-upload {
  max-width: 700px;
  margin: 0 auto;
}

.upload-section {
  margin-bottom: var(--spacing-lg);
}

.upload-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
}

.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 400;
}

.cover-uploader {
  :deep(.el-upload) {
    width: 200px;
    height: 200px;
    border: 2px dashed var(--border);
    border-radius: var(--radius-md);
    overflow: hidden;
    cursor: pointer;
    transition: var(--transition);
    &:hover {
      border-color: var(--primary-light);
    }
  }
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-muted);
  gap: var(--spacing-xs);
  font-size: 13px;

  &--small {
    width: 100px;
    height: 100px;
  }
}

.upload-icon {
  font-size: 28px;
  color: var(--text-muted);
}

.remove-cover-btn {
  margin-top: var(--spacing-xs);
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--border);
}

.image-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: var(--color-discount-bg);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: var(--transition-fast);
  &:hover {
    transform: scale(1.15);
  }
}

.image-add {
  :deep(.el-upload) {
    width: 100px;
    height: 100px;
    border: 2px dashed var(--border);
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: var(--transition);
    &:hover {
      border-color: var(--primary-light);
    }
  }
}

.step-preview {
  display: flex;
  justify-content: center;
}

.preview-card {
  width: 360px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-md);
}

.preview-cover-wrap {
  position: relative;
  padding-top: 75%;
  background: linear-gradient(135deg, var(--color-img-placeholder-from), var(--color-img-placeholder-to));
}

.preview-cover-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 4px;
}

.preview-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
  backdrop-filter: blur(6px);
  color: #fff;

  &--category {
    background: var(--color-tag-bg);
  }

  &--condition {
    background: var(--color-condition-bg);
  }
}

.preview-body {
  padding: 14px 16px 18px;
}

.preview-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 12px;
}

.original-price {
  font-size: 13px;
  color: var(--text-muted);
  text-decoration: line-through;
}

.preview-stock {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

.preview-images {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  overflow-x: auto;
}

.preview-img-thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
  border: 1px solid var(--border);
  flex-shrink: 0;
}

.step-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border);
}

.step-actions-right {
  display: flex;
  gap: var(--spacing-sm);
  align-items: center;
}

.draft-saved-hint {
  font-size: 13px;
  color: var(--success);
  font-weight: 500;
}
</style>
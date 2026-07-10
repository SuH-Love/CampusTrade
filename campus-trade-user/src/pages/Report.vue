<template>
  <div class="report-page">
    <el-card>
      <template #header><h3>提交举报</h3></template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" style="max-width: 600px">
        <el-form-item label="举报类型" prop="targetType">
          <el-select v-model="form.targetType" placeholder="选择类型" :disabled="!!route.query.targetType" @change="handleTypeChange">
            <el-option label="商品" :value="1" />
            <el-option label="用户" :value="2" />
            <el-option label="聊天" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报目标" prop="targetId">
          <el-input v-model="form.targetId" disabled placeholder="举报目标ID" />
        </el-form-item>
        <el-form-item label="目标信息" v-if="targetInfo">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="名称">{{ targetInfo.name }}</el-descriptions-item>
            <el-descriptions-item label="详情">{{ targetInfo.detail }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-form-item label="举报原因" prop="reason">
          <el-input v-model="form.reason" placeholder="简要描述原因" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述举报内容" />
        </el-form-item>
        <el-form-item label="证据图片">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleImageChange"
          >
            <el-button type="primary" size="small">选择图片</el-button>
          </el-upload>
          <div v-if="imageList.length > 0" class="uploaded-images">
            <div v-for="(img, idx) in imageList" :key="idx" class="image-preview-item">
              <el-image :src="img" fit="cover" style="width: 80px; height: 80px; border-radius: 8px" :preview-src-list="imageList" :initial-index="idx" />
              <el-button type="danger" :icon="Delete" circle size="small" class="image-delete-btn" @click="removeImage(idx)" />
            </div>
          </div>
        </el-form-item>
        <el-form-item><el-button type="danger" @click="handleSubmit" :loading="loading">提交举报</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header><h3>我的举报</h3></template>
      <el-table :data="reports" stripe>
        <el-table-column prop="targetType" label="类型" width="80">
          <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-if="reports.length === 0" description="暂无举报记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { createReport, listMyReports } from '@/api/report'
import { getGoodsDetail } from '@/api/goods'
import { uploadImage } from '@/api/file'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import type { ReportVO } from '@/api/report'
import type { UploadUserFile } from 'element-plus'

const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const reports = ref<ReportVO[]>([])
const targetInfo = ref<{ name: string; detail: string } | null>(null)
const imageList = ref<string[]>([])
const uploadingImage = ref(false)

const form = reactive({ targetType: 1, targetId: '', reason: '', description: '', images: '' })
const rules = {
  targetType: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
  targetId: [{ required: true, message: '举报目标不能为空', trigger: 'change' }],
  reason: [{ required: true, message: '请输入举报原因', trigger: 'blur' }]
}

const statusTagMap: Record<string, string> = { PENDING: 'warning', FINISHED: '', RESOLVED: 'success', DISMISSED: 'info' }
const statusLabel = (s: string) => ({ PENDING: '待处理', FINISHED: '已处理', RESOLVED: '已解决', DISMISSED: '已驳回' }[s] || s)
const targetTypeLabel = (t: number) => ({ 1: '商品', 2: '用户', 3: '聊天' }[t] || '其他')

const handleTypeChange = () => {
  form.targetId = ''
  targetInfo.value = null
}

const loadTargetInfo = async () => {
  if (!form.targetId) { targetInfo.value = null; return }
  try {
    if (form.targetType === 1) {
      const goods = await getGoodsDetail(Number(form.targetId))
      targetInfo.value = { name: goods.title, detail: `¥${goods.price} · ${goods.categoryName || ''}` }
    }
  } catch (e) { console.error(e); targetInfo.value = null }
}

const handleImageChange = async (uploadFile: UploadUserFile) => {
  if (!uploadFile.raw) return
  uploadingImage.value = true
  try {
    const url = await uploadImage(uploadFile.raw)
    imageList.value.push(url)
    form.images = imageList.value.join(',')
  } catch (e) { console.error(e); ElMessage.error('图片上传失败') }
  finally { uploadingImage.value = false }
}

const removeImage = (idx: number) => {
  imageList.value.splice(idx, 1)
  form.images = imageList.value.join(',')
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await createReport({ targetType: form.targetType, targetId: Number(form.targetId), reason: form.reason, description: form.description, images: form.images || undefined })
    ElMessage.success('举报已提交')
    form.reason = ''
    form.description = ''
    form.images = ''
    imageList.value = []
    loadReports()
  } finally {
    loading.value = false
  }
}

const loadReports = async () => {
  try {
    const res = await listMyReports()
    reports.value = res.list || []
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  if (route.query.targetType) {
    form.targetType = Number(route.query.targetType)
  }
  if (route.query.targetId) {
    form.targetId = String(route.query.targetId)
    await loadTargetInfo()
  }
  loadReports()
})
</script>

<style scoped lang="scss">
.report-page {
  padding: 20px;
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 50%, #f0fdf4 100%);
  min-height: calc(100vh - 60px);
  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid rgba(99, 102, 241, 0.08);
    box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
  }
}
.uploaded-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.image-preview-item { position: relative; display: inline-block; }
.image-delete-btn { position: absolute; top: -6px; right: -6px; z-index: 1; }
</style>

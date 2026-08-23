<template>
  <div class="report-page page-bg">
    <el-card class="report-form-card">
      <template #header>
        <div class="card-header">
          <h3>提交举报</h3>
          <span class="header-desc">我们会认真处理每一条举报</span>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="report-form">
        <el-form-item label="举报类型" prop="targetType">
          <el-select v-model="form.targetType" placeholder="选择类型" :disabled="!!route.query.targetType" @change="handleTypeChange" class="w-full">
            <el-option label="商品" :value="1" />
            <el-option label="用户" :value="2" />
            <el-option label="聊天" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报目标" prop="targetId">
          <el-input v-model="form.targetId" disabled placeholder="举报目标ID" />
        </el-form-item>
        <el-form-item label="目标信息" v-if="targetInfo">
          <div class="target-info-card">
            <div class="target-name">{{ targetInfo.name }}</div>
            <div class="target-detail">{{ targetInfo.detail }}</div>
          </div>
        </el-form-item>
        <el-form-item label="举报原因" prop="reason">
          <el-input v-model="form.reason" placeholder="简要描述原因" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述举报内容，帮助我们更好地处理" />
        </el-form-item>
        <el-form-item label="证据图片">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*"
            :on-change="handleImageChange"
          >
            <el-button type="primary" size="small" round>选择图片</el-button>
          </el-upload>
          <div v-if="imageList.length > 0" class="uploaded-images">
            <div v-for="(img, idx) in imageList" :key="idx" class="image-preview-item">
              <el-image :src="img" fit="cover" class="evidence-img" :preview-src-list="imageList" :initial-index="idx" alt="证据图片" />
              <el-button type="danger" :icon="Delete" circle size="small" class="image-delete-btn" @click="removeImage(idx)" />
            </div>
          </div>
        </el-form-item>
        <el-form-item><el-button type="danger" @click="handleSubmit" :loading="loading" round>提交举报</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card class="report-list-card">
      <template #header>
        <div class="card-header">
          <h3>我的举报</h3>
          <span class="header-desc">共 {{ reports.length }} 条记录</span>
        </div>
      </template>
      <el-table :data="reports" stripe>
        <el-table-column prop="targetType" label="类型" min-width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.targetType === 1 ? '' : row.targetType === 2 ? 'warning' : 'info'">{{ targetTypeLabel(row.targetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'" effect="dark" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.handleResult">{{ row.handleResult }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" min-width="170" />
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

const handleImageChange = async (uploadFile: { raw?: File }) => {
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

  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid var(--border);
    box-shadow: var(--shadow-sm);
  }
}
.card-header {
  display: flex; align-items: baseline; gap: 12px;
  h3 { margin: 0; }
}
.header-desc { font-size: 13px; color: var(--text-muted); font-weight: 400; }
.target-info-card {
  background: var(--bg-glass);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 12px 16px;
  width: 100%;
}
.target-name { font-weight: 600; font-size: 14px; }
.target-detail { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.uploaded-images { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; }
.image-preview-item { position: relative; display: inline-block; }
.image-delete-btn { position: absolute; top: -6px; right: -6px; z-index: 1; }
.report-form { max-width: 600px; }
.report-list-card { margin-top: 20px; }
.evidence-img { width: 80px; height: 80px; border-radius: 10px; }
.text-muted { color: var(--text-muted); }

@media (max-width: 768px) {
  .report-form { max-width: 100%; }
  .evidence-img { width: 60px; height: 60px; }
}
</style>

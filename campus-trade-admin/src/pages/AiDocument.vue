<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>文档知识库</h3>
          <div class="header-actions">
            <el-upload
              ref="uploadRef"
              :show-file-list="false"
              :before-upload="handleUpload"
              accept=".pdf,.docx,.txt,.md,.jpg,.jpeg,.png,.gif,.bmp,.webp"
            >
              <el-button type="primary" :loading="uploading">上传文档</el-button>
            </el-upload>
          </div>
        </div>
      </template>
      <el-table :data="documents" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="文件名" show-overflow-tooltip min-width="200" />
        <el-table-column prop="fileType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ fileTypeLabel(row.fileType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
        <el-table-column prop="createTime" label="上传时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" :title="detailData?.title || '文档详情'" width="800px">
      <div v-if="detailData" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="类型">{{ fileTypeLabel(detailData.fileType) }}</el-descriptions-item>
          <el-descriptions-item label="大小">{{ formatSize(detailData.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="分块数">{{ detailData.chunkCount }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailData.errorMessage" style="margin-top: 12px;">
          <el-alert type="error" :title="detailData.errorMessage" :closable="false" />
        </div>
        <el-divider content-position="left">分块预览</el-divider>
        <div v-for="chunk in detailData.chunks" :key="chunk.id" class="chunk-item">
          <div class="chunk-header">
            <span class="chunk-index">分块 {{ chunk.chunkIndex }}</span>
            <span v-if="chunk.pageNum" class="chunk-page">第{{ chunk.pageNum }}页</span>
          </div>
          <div class="chunk-content">{{ chunk.content }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAiDocuments, uploadAiDocument, deleteAiDocument, getAiDocumentDetail } from '@/api/admin'

const loading = ref(false)
const uploading = ref(false)
const documents = ref<any[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>(null)
let pollTimer: any = null

const loadDocuments = async () => {
  loading.value = true
  try {
    documents.value = await getAiDocuments()
    if (documents.value.some(d => d.status === 'processing') && !pollTimer) {
      pollTimer = setInterval(loadDocuments, 3000)
    } else if (!documents.value.some(d => d.status === 'processing') && pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleUpload = async (file: File) => {
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await uploadAiDocument(formData)
    ElMessage.success('上传成功，正在处理...')
    await loadDocuments()
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
  return false
}

const viewDetail = async (row: any) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await getAiDocumentDetail(row.id)
  } catch (e: any) {
    ElMessage.error(e.message || '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定删除"${row.title}"？`, '提示', { type: 'warning' })
    await deleteAiDocument(row.id)
    ElMessage.success('删除成功')
    await loadDocuments()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const fileTypeLabel = (t: string) => ({ pdf: 'PDF', word: 'Word', image: '图片', text: '文本' }[t] || t)
const statusLabel = (s: string) => ({ processing: '处理中', ready: '就绪', error: '失败' }[s] || s)
const statusType = (s: string) => ({ processing: 'warning', ready: 'success', error: 'danger' }[s] || 'info') as any
const formatSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}
const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-'

onMounted(loadDocuments)
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<style scoped>
.chunk-item {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}
.chunk-header {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.chunk-content {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
}
</style>
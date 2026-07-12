<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>举报审核</h3>
          <div class="admin-filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索举报人/原因" clearable class="filter-input" @keyup.enter="handleSearch" @clear="handleSearch">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="filter-select">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已处理" value="FINISHED" />
              <el-option label="已解决" value="RESOLVED" />
              <el-option label="已驳回" value="DISMISSED" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="reporterName" label="举报人" min-width="90" />
        <el-table-column prop="targetType" label="类型" min-width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.targetType === 1 ? '' : row.targetType === 2 ? 'warning' : 'info'" effect="dark" round>{{ targetTypeLabel(row.targetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标ID" min-width="70" />
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="description" label="详细描述" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
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
        <el-table-column prop="createTime" label="举报时间" min-width="150" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING' || row.status === 'PROCESSING'" v-permission="'report:review'" type="success" size="small" @click="openResolveDialog(row.id)">处理</el-button>
            <el-button v-if="row.status === 'PENDING' || row.status === 'PROCESSING'" v-permission="'report:review'" type="warning" size="small" @click="openDismissDialog(row.id)">驳回</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无举报" :image-size="60" /></template>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, prev, pager, next, sizes"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="举报详情" width="680px">
      <template v-if="detailReport">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="举报人">{{ detailReport.reporterName }}</el-descriptions-item>
          <el-descriptions-item label="举报类型"><el-tag size="small" effect="dark" round>{{ targetTypeLabel(detailReport.targetType) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="目标ID">{{ detailReport.targetId }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusTagMap[detailReport.status] || 'info'" effect="dark" round>{{ statusLabel(detailReport.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="举报原因" :span="2">{{ detailReport.reason }}</el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">{{ detailReport.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理结果" :span="2" v-if="detailReport.handleResult">{{ detailReport.handleResult }}</el-descriptions-item>
          <el-descriptions-item label="举报时间" :span="2">{{ detailReport.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="evidenceImageList.length" class="evidence-section">
          <h4 class="section-title">证据图片</h4>
          <div class="evidence-images">
            <el-image
              v-for="(img, idx) in evidenceImageList"
              :key="idx"
              :src="img"
              fit="cover"
              class="evidence-image-item"
              :preview-src-list="evidenceImageList"
              :initial-index="idx"
            />
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="resolveDialogVisible" title="处理举报" width="460px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="处理原因" required>
          <el-input v-model="resolveReason" type="textarea" :rows="4" placeholder="请输入处理原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="success" @click="handleResolve" :loading="resolveLoading">确认处理</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dismissDialogVisible" title="驳回举报" width="460px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="驳回原因" required>
          <el-input v-model="dismissReason" type="textarea" :rows="4" placeholder="请输入驳回原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dismissDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleDismiss" :loading="dismissLoading">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getReportList, resolveReport, dismissReport } from '@/api/admin'
import { ElMessage } from 'element-plus'
import type { AdminReportVO, PageQueryParams } from '@/types'
import { reportStatusLabel } from '@/utils/labels'

const reports = ref<AdminReportVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailReport = ref<AdminReportVO | null>(null)
const resolveDialogVisible = ref(false)
const resolveReportId = ref<number>(0)
const resolveReason = ref('')
const resolveLoading = ref(false)
const dismissDialogVisible = ref(false)
const dismissReportId = ref<number>(0)
const dismissReason = ref('')
const dismissLoading = ref(false)

const statusTagMap: Record<string, string> = { PENDING: 'warning', PROCESSING: 'primary', FINISHED: '', RESOLVED: 'success', DISMISSED: 'info' }
const statusLabel = (status: string) => reportStatusLabel(status)
const targetTypeLabel = (type: number) => {
  const map: Record<number, string> = { 1: '商品', 2: '用户', 3: '聊天' }
  return map[type] || '其他'
}

const evidenceImageList = computed(() => {
  if (!detailReport.value?.evidenceImages) return []
  return detailReport.value.evidenceImages.split(',').filter(Boolean)
})

const loadData = async () => {
  loading.value = true
  try {
    const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getReportList(params)
    reports.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}

const openResolveDialog = (id: number) => {
  resolveReportId.value = id
  resolveReason.value = ''
  resolveDialogVisible.value = true
}

const handleResolve = async () => {
  if (!resolveReason.value.trim()) {
    ElMessage.warning('请输入处理原因')
    return
  }
  resolveLoading.value = true
  try {
    await resolveReport(resolveReportId.value)
    ElMessage.success('已处理')
    resolveDialogVisible.value = false
    loadData()
  } finally {
    resolveLoading.value = false
  }
}

const openDismissDialog = (id: number) => {
  dismissReportId.value = id
  dismissReason.value = ''
  dismissDialogVisible.value = true
}

const handleDismiss = async () => {
  if (!dismissReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  dismissLoading.value = true
  try {
    await dismissReport(dismissReportId.value)
    ElMessage.success('已驳回')
    dismissDialogVisible.value = false
    loadData()
  } finally {
    dismissLoading.value = false
  }
}

const showDetail = (row: AdminReportVO) => {
  detailReport.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.filter-input { width: 200px; }
.filter-select { width: 140px; }
.text-muted { color: var(--admin-text-secondary); }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.evidence-section { margin-top: 16px; }
.section-title { font-size: 14px; font-weight: 600; color: var(--admin-text); margin: 0 0 8px 0; }
.evidence-images { display: flex; gap: 8px; flex-wrap: wrap; }
.evidence-image-item { width: 120px; height: 120px; border-radius: 8px; border: 1px solid var(--admin-border); }
</style>

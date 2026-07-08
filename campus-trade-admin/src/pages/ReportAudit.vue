<template>
  <div class="report-audit-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>举报审核</h3>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadData" style="width: 160px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="FINISHED" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已驳回" value="DISMISSED" />
          </el-select>
        </div>
      </template>
      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="reporterName" label="举报人" width="100" />
        <el-table-column prop="targetType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ targetTypeLabel(row.targetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" show-overflow-tooltip />
        <el-table-column prop="createTime" label="举报时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleResolve(row.id)">处理</el-button>
            <el-button v-if="row.status === 'PENDING'" type="warning" size="small" @click="handleDismiss(row.id)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="reports.length === 0" description="暂无举报" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getReportList, resolveReport, dismissReport } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminReportVO, PageQueryParams } from '@/types'

const reports = ref<AdminReportVO[]>([])
const statusFilter = ref('PENDING')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const statusTagMap: Record<string, string> = { PENDING: 'warning', FINISHED: '', RESOLVED: 'success', DISMISSED: 'info' }
const statusLabel = (status: string) => {
  const map: Record<string, string> = { PENDING: '待处理', FINISHED: '已处理', RESOLVED: '已解决', DISMISSED: '已驳回' }
  return map[status] || status
}
const targetTypeLabel = (type: number) => {
  const map: Record<number, string> = { 1: '商品', 2: '用户', 3: '聊天' }
  return map[type] || '其他'
}

const loadData = async () => {
  loading.value = true
  try {
  const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await getReportList(params)
  reports.value = res.list || []
  total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleResolve = async (id: number) => {
  await ElMessageBox.confirm('确认处理该举报？处理后将下架相关内容', '确认')
  await resolveReport(id)
  ElMessage.success('已处理')
  loadData()
}

const handleDismiss = async (id: number) => {
  await dismissReport(id)
  ElMessage.success('已驳回')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.report-audit-page { padding: 20px; }
</style>

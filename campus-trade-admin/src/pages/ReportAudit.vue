<template>
  <div class="report-audit-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>举报审核</h3>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadData" style="width: 160px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="RESOLVED" />
            <el-option label="已驳回" value="DISMISSED" />
          </el-select>
        </div>
      </template>
      <el-table :data="reports" stripe>
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
        <el-table-column prop="createTime" label="举报时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="handleResolve(row.id)">处理</el-button>
            <el-button v-if="row.status === 'PENDING'" type="warning" size="small" @click="handleDismiss(row.id)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const reports = ref<any[]>([])
const statusFilter = ref('PENDING')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusTagMap: Record<string, string> = { PENDING: 'warning', RESOLVED: 'success', DISMISSED: 'info' }
const statusLabel = (status: string) => {
  const map: Record<string, string> = { PENDING: '待处理', RESOLVED: '已处理', DISMISSED: '已驳回' }
  return map[status] || status
}
const targetTypeLabel = (type: string) => {
  const map: Record<string, string> = { GOODS: '商品', USER: '用户', ORDER: '订单' }
  return map[type] || type
}

const loadData = async () => {
  const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await request.get('/admin/report', { params })
  reports.value = res.list || []
  total.value = res.total || 0
}

const handleResolve = async (id: number) => {
  await ElMessageBox.confirm('确认处理该举报？处理后将下架相关内容', '确认')
  await request.put(`/admin/report/${id}/resolve`)
  ElMessage.success('已处理')
  loadData()
}

const handleDismiss = async (id: number) => {
  await request.put(`/admin/report/${id}/dismiss`)
  ElMessage.success('已驳回')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.report-audit-page { padding: 20px; }
</style>

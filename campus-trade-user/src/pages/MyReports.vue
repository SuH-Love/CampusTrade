<template>
  <div class="my-reports-page">
    <el-card>
      <template #header><h3 style="margin: 0">我的举报</h3></template>
      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
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
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMyReports } from '@/api/report'
import type { ReportVO } from '@/api/report'

const reports = ref<ReportVO[]>([])
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
    const res = await listMyReports(pageNum.value, pageSize.value)
    reports.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.my-reports-page { padding: 20px; }
</style>
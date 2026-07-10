<template>
  <div class="my-reports-page">
    <el-card>
      <template #header><h3 style="margin: 0">我的举报</h3></template>
      <el-table :data="reports" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="targetType" label="类型" min-width="70">
          <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column label="举报目标" min-width="150">
          <template #default="{ row }">
            <div v-if="row.targetTitle" style="font-weight: 500">{{ row.targetTitle }}</div>
            <div v-else style="color: #999">ID: {{ row.targetId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="证据图片" min-width="120">
          <template #default="{ row }">
            <div v-if="row.images" class="report-images">
              <el-image
                v-for="(img, idx) in row.images.split(',').filter(Boolean)"
                :key="idx"
                :src="img"
                :preview-src-list="row.images.split(',').filter(Boolean)"
                :initial-index="idx"
                fit="cover"
                style="width: 40px; height: 40px; border-radius: 4px; margin-right: 4px"
              />
            </div>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详细描述" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" min-width="150" />
      </el-table>
      <el-empty v-if="reports.length === 0" description="暂无举报记录" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
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
.my-reports-page {
  padding: 20px;
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 50%, #f0fdf4 100%);
  min-height: calc(100vh - 60px);
  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid rgba(99, 102, 241, 0.08);
    box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
  }
}
.report-images { display: flex; flex-wrap: wrap; gap: 2px; }
</style>

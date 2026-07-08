<template>
  <div class="dashboard-page">
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover" class="stat-card">
          <el-statistic :title="item.label" :value="item.value">
            <template #prefix><el-icon :style="{ color: item.color }"><component :is="item.icon" /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span>待处理事项</span></template>
          <div class="todo-list">
            <div class="todo-item" v-for="item in todoItems" :key="item.label" @click="$router.push(item.path)">
              <el-badge :value="item.count" :type="item.count > 0 ? 'danger' : 'info'" />
              <span style="margin-left: 10px">{{ item.label }}</span>
              <el-icon style="margin-left: auto"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>最近操作日志</span></template>
          <el-table :data="recentLogs" size="small" stripe>
            <el-table-column prop="username" label="操作人" width="100" />
            <el-table-column prop="operation" label="操作" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats, getReportList, getOperationLogs } from '@/api/admin'

const stats = ref([
  { label: '用户总数', value: 0, icon: 'User', color: '#409eff' },
  { label: '商品总数', value: 0, icon: 'Goods', color: '#67c23a' },
  { label: '订单总数', value: 0, icon: 'List', color: '#e6a23c' },
  { label: '待审核', value: 0, icon: 'Warning', color: '#f56c6c' }
])

const todoItems = ref([
  { label: '待审核商品', count: 0, path: '/goods' },
  { label: '待处理举报', count: 0, path: '/report' }
])

const recentLogs = ref<any[]>([])

const loadStats = async () => {
  try {
    const res = await getDashboardStats()
    stats.value[0].value = res.userCount || 0
    stats.value[1].value = res.goodsCount || 0
    stats.value[2].value = res.orderCount || 0
    stats.value[3].value = res.pendingAudit || 0
    todoItems.value[0].count = res.pendingAudit || 0
  } catch { /* ignore */ }
  try {
    const res = await getReportList({ pageNum: 1, pageSize: 1, status: 'PENDING' })
    todoItems.value[1].count = res.total || 0
  } catch { /* ignore */ }
}

const loadRecentLogs = async () => {
  try {
    const res = await getOperationLogs({ pageNum: 1, pageSize: 5 })
    recentLogs.value = res.list || []
  } catch { /* ignore */ }
}

onMounted(() => { loadStats(); loadRecentLogs() })
</script>

<style scoped lang="scss">
.dashboard-page { padding: 20px; }
.stat-card { text-align: center; }
.todo-list { .todo-item { display: flex; align-items: center; padding: 12px 0; border-bottom: 1px solid #f0f0f0; cursor: pointer; &:hover { background: #fafafa; } &:last-child { border-bottom: none; } } }
</style>

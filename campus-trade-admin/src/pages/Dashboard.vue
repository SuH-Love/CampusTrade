<template>
  <div class="dashboard-page">
    <el-row :gutter="20" style="margin-bottom: 24px">
      <el-col :xs="12" :sm="6" v-for="item in stats" :key="item.label">
        <div class="stat-card" :style="{ borderLeftColor: item.color }">
          <div class="stat-icon" :style="{ background: item.color + '15', color: item.color }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="todo-card">
          <template #header><span class="card-title">待处理事项</span></template>
          <div class="todo-list">
            <div class="todo-item" v-for="item in todoItems" :key="item.label" @click="$router.push(item.path)">
              <div class="todo-info">
                <span class="todo-label">{{ item.label }}</span>
                <el-tag :type="item.count > 0 ? 'danger' : 'info'" round>{{ item.count }}</el-tag>
              </div>
              <el-icon style="color: var(--admin-text-secondary)"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="card-title">最近操作日志</span></template>
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
import type { OperationLogVO, PageQueryParams } from '@/types'

const stats = ref([
  { label: '用户总数', value: 0, icon: 'User', color: '#6366f1' },
  { label: '商品总数', value: 0, icon: 'Goods', color: '#10b981' },
  { label: '订单总数', value: 0, icon: 'List', color: '#f59e0b' },
  { label: '待审核', value: 0, icon: 'Warning', color: '#ef4444' }
])

const todoItems = ref([
  { label: '待审核商品', count: 0, path: '/goods' },
  { label: '待处理举报', count: 0, path: '/report' }
])

const recentLogs = ref<OperationLogVO[]>([])

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
    const res = await getReportList({ pageNum: 1, pageSize: 1, status: 'PENDING' } as PageQueryParams)
    todoItems.value[1].count = res.total || 0
  } catch { /* ignore */ }
}

const loadRecentLogs = async () => {
  try {
    const res = await getOperationLogs({ pageNum: 1, pageSize: 5 } as PageQueryParams)
    recentLogs.value = res.list || []
  } catch { /* ignore */ }
}

onMounted(() => { loadStats(); loadRecentLogs() })
</script>

<style scoped lang="scss">
.stat-card {
  background: var(--admin-card-bg);
  border-radius: var(--admin-radius);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-left: 4px solid;
  box-shadow: var(--admin-shadow);
  transition: var(--admin-transition);
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
}

.stat-icon {
  width: 52px; height: 52px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.stat-value { font-size: 28px; font-weight: 700; color: var(--admin-text); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--admin-text-secondary); margin-top: 2px; }

.card-title { font-size: 16px; font-weight: 600; }

.todo-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 0; border-bottom: 1px solid var(--admin-border);
  cursor: pointer; transition: var(--admin-transition);
  &:hover { padding-left: 8px; }
  &:last-child { border-bottom: none; }
}

.todo-info { display: flex; align-items: center; gap: 12px; }
.todo-label { font-size: 14px; font-weight: 500; }
</style>

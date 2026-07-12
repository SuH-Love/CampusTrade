<template>
  <div class="dashboard-page admin-page">
    <el-row :gutter="16" class="mb-lg">
      <el-col :xs="12" :sm="6" :md="4" v-for="item in stats" :key="item.label">
        <div class="stat-card" :style="{ borderTopColor: item.color }">
          <div class="stat-icon" :style="{ background: item.color + '18', color: item.color }">
            <el-icon :size="22"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="20" class="mb-lg">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">商品状态分布</span></template>
          <div ref="goodsChartRef" style="height: 300px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span class="card-title">订单状态分布</span></template>
          <div ref="orderChartRef" style="height: 300px" />
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header><span class="card-title">待处理事项</span></template>
          <div class="todo-list">
            <div class="todo-item" v-for="item in todoItems" :key="item.label" @click="$router.push(item.path)">
              <div class="todo-info">
                <span class="todo-label">{{ item.label }}</span>
                <el-tag :type="item.count > 0 ? 'danger' : 'info'" round size="small">{{ item.count }}</el-tag>
              </div>
              <el-icon style="color: var(--admin-text-secondary)"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header><span class="card-title">最近操作日志</span></template>
          <el-table :data="recentLogs" size="small" stripe>
            <el-table-column prop="username" label="操作人" min-width="90" />
            <el-table-column prop="operation" label="操作" show-overflow-tooltip min-width="120">
              <template #default="{ row }">{{ operationLabel(row.operation) }}</template>
            </el-table-column>
            <el-table-column prop="module" label="模块" min-width="90">
              <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
            </el-table-column>
            <el-table-column prop="ip" label="IP" min-width="130" />
            <el-table-column prop="createTime" label="时间" min-width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { User, Sunny, Plus, Box, ShoppingCart, Tickets, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats, getReportList, getOperationLogs } from '@/api/admin'
import { operationLabel, moduleLabel } from '@/utils/labels'
import type { OperationLogVO, PageQueryParams } from '@/types'

const stats = ref([
  { label: '用户总数', value: 0, icon: User, color: '#6366f1' },
  { label: '今日日活', value: 0, icon: Sunny, color: '#f59e0b' },
  { label: '今日新增', value: 0, icon: Plus, color: '#10b981' },
  { label: '商品总数', value: 0, icon: Box, color: '#8b5cf6' },
  { label: '订单总数', value: 0, icon: ShoppingCart, color: '#3b82f6' },
  { label: '今日订单', value: 0, icon: Tickets, color: '#06b6d4' }
])

const todoItems = ref([
  { label: '待审核商品', count: 0, path: '/goods' },
  { label: '待处理举报', count: 0, path: '/report' },
  { label: '退款中订单', count: 0, path: '/order?status=REFUND' }
])

const recentLogs = ref<OperationLogVO[]>([])
const goodsChartRef = ref<HTMLElement>()
const orderChartRef = ref<HTMLElement>()
let goodsChart: echarts.ECharts | null = null
let orderChart: echarts.ECharts | null = null

const goodsStatusData = ref<{ name: string; value: number }[]>([])
const orderStatusData = ref<{ name: string; value: number }[]>([])

const initGoodsChart = () => {
  if (!goodsChartRef.value) return
  goodsChart = echarts.init(goodsChartRef.value)
  goodsChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}' },
      data: goodsStatusData.value
    }],
    color: ['#10b981', '#f59e0b', '#6366f1', '#ef4444', '#8b5cf6', '#64748b']
  })
}

const initOrderChart = () => {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  orderChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}' },
      data: orderStatusData.value
    }],
    color: ['#f59e0b', '#6366f1', '#10b981', '#8b5cf6', '#22c55e', '#64748b', '#ef4444']
  })
}

const loadStats = async () => {
  try {
    const res = await getDashboardStats()
    stats.value[0].value = res.userCount || 0
    stats.value[1].value = res.todayActive || 0
    stats.value[2].value = res.todayNewUsers || 0
    stats.value[3].value = res.goodsCount || 0
    stats.value[4].value = res.orderCount || 0
    stats.value[5].value = res.todayOrders || 0
    todoItems.value[0].count = res.pendingAudit || 0
    if (res.goodsStatusMap) {
      const nameMap: Record<string, string> = { ONLINE: '在售', OFFLINE: '已下架', SOLD: '已售出', PENDING: '待审核', DRAFT: '草稿', REJECTED: '已拒绝' }
      goodsStatusData.value = Object.entries(res.goodsStatusMap)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: nameMap[k] || k, value: v }))
    } else {
      goodsStatusData.value = [{ name: '在售', value: 0 }, { name: '待审核', value: res.pendingAudit || 0 }]
    }
    if (res.orderStatusMap) {
      const nameMap: Record<string, string> = { PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货', PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中' }
      orderStatusData.value = Object.entries(res.orderStatusMap)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: nameMap[k] || k, value: v }))
      const refundEntry = orderStatusData.value.find(d => d.name === '退款中')
      todoItems.value[2].count = refundEntry ? refundEntry.value : 0
    } else {
      orderStatusData.value = [{ name: '订单', value: res.orderCount || 0 }]
    }
    await nextTick()
    initGoodsChart()
    initOrderChart()
  } catch (e) { console.error(e) }
  try {
    const [pendingRes, processingRes] = await Promise.all([
      getReportList({ pageNum: 1, pageSize: 1, status: 'PENDING' } as PageQueryParams),
      getReportList({ pageNum: 1, pageSize: 1, status: 'PROCESSING' } as PageQueryParams)
    ])
    todoItems.value[1].count = (pendingRes.total || 0) + (processingRes.total || 0)
  } catch (e) { console.error(e) }
}

const loadRecentLogs = async () => {
  try {
    const res = await getOperationLogs({ pageNum: 1, pageSize: 8 } as PageQueryParams)
    recentLogs.value = res.list || []
  } catch (e) { console.error(e) }
}

const handleResize = () => {
  goodsChart?.resize()
  orderChart?.resize()
}

onMounted(() => { loadStats(); loadRecentLogs(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { goodsChart?.dispose(); orderChart?.dispose(); window.removeEventListener('resize', handleResize) })
</script>

<style scoped lang="scss">
.stat-card {
  background: var(--admin-card-bg);
  border-radius: 12px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  border-top: 3px solid;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  transition: all 0.25s;
  margin-bottom: 12px;
  &:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.1); }
}

.stat-icon {
  width: 48px; height: 48px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.stat-value { font-size: 24px; font-weight: 700; color: var(--admin-text); line-height: 1.2; }
.stat-label { font-size: 12px; color: var(--admin-text-secondary); margin-top: 2px; }

.card-title { font-size: 15px; font-weight: 600; }

.todo-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0; border-bottom: 1px solid var(--admin-border);
  cursor: pointer; transition: all 0.2s;
  &:hover { padding-left: 8px; background: rgba(99,102,241,0.03); }
  &:last-child { border-bottom: none; }
}

.todo-info { display: flex; align-items: center; gap: 10px; }
.todo-label { font-size: 13px; font-weight: 500; }
</style>

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
    <el-row :gutter="20" style="margin-bottom: 24px">
      <el-col :span="12">
        <el-card>
          <template #header><span class="card-title">商品状态分布</span></template>
          <div ref="goodsChartRef" style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span class="card-title">订单状态分布</span></template>
          <div ref="orderChartRef" style="height: 320px" />
        </el-card>
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
            <el-table-column prop="operation" label="操作" show-overflow-tooltip>
              <template #default="{ row }">{{ operationLabel(row.operation) }}</template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="170" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
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
const goodsChartRef = ref<HTMLElement>()
const orderChartRef = ref<HTMLElement>()
let goodsChart: echarts.ECharts | null = null
let orderChart: echarts.ECharts | null = null

const goodsStatusData = ref<{ name: string; value: number }[]>([])
const orderStatusData = ref<{ name: string; value: number }[]>([])

const operationLabel = (op: string) => {
  const map: Record<string, string> = {
    register: '注册', login: '登录', logout: '退出', createGoods: '发布商品',
    auditGoods: '审核商品', banUser: '封禁用户', unbanUser: '解封用户',
    resolveReport: '处理举报', dismissReport: '驳回举报', createOrder: '创建订单',
    cancelOrder: '取消订单', payOrder: '支付订单', shipOrder: '发货',
    approveRefund: '同意退款', rejectRefund: '拒绝退款'
  }
  return map[op] || op
}

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
    stats.value[1].value = res.goodsCount || 0
    stats.value[2].value = res.orderCount || 0
    stats.value[3].value = res.pendingAudit || 0
    todoItems.value[0].count = res.pendingAudit || 0
    if (res.goodsStatusMap) {
      const nameMap: Record<string, string> = { ONLINE: '在售', OFFLINE: '已下架', SOLD: '已售出', PENDING: '待审核', DRAFT: '草稿', REJECTED: '已拒绝' }
      goodsStatusData.value = Object.entries(res.goodsStatusMap as Record<string, number>)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: nameMap[k] || k, value: v }))
    } else {
      goodsStatusData.value = [
        { name: '在售', value: 0 }, { name: '待审核', value: res.pendingAudit || 0 }
      ]
    }
    if (res.orderStatusMap) {
      const nameMap: Record<string, string> = { PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货', PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中' }
      orderStatusData.value = Object.entries(res.orderStatusMap as Record<string, number>)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: nameMap[k] || k, value: v }))
    } else {
      orderStatusData.value = [{ name: '订单', value: res.orderCount || 0 }]
    }
    await nextTick()
    initGoodsChart()
    initOrderChart()
  } catch (e) { console.error(e) }
  try {
    const res = await getReportList({ pageNum: 1, pageSize: 1, status: 'PENDING' } as PageQueryParams)
    todoItems.value[1].count = res.total || 0
  } catch (e) { console.error(e) }
}

const loadRecentLogs = async () => {
  try {
    const res = await getOperationLogs({ pageNum: 1, pageSize: 5 } as PageQueryParams)
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

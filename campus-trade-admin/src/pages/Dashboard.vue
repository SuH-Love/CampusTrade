<template>
  <div class="dashboard-page admin-page">
    <div class="bento-dashboard">
      <div class="bento-stats">
        <div v-for="(item, idx) in stats" :key="item.label" class="stat-card" :style="{ animationDelay: `${idx * 0.06}s`, borderLeftColor: item.color }">
          <div class="stat-icon" :style="{ background: item.color + '18', color: item.color }">
            <el-icon :size="22"><component :is="item.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ item.value }}</div>
            <div class="stat-label">{{ item.label }}</div>
          </div>
        </div>
      </div>

      <div class="bento-charts">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">📊 商品状态分布</span></template>
          <div ref="goodsChartRef" class="chart-container" />
        </el-card>
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">📈 订单状态分布</span></template>
          <div ref="orderChartRef" class="chart-container" />
        </el-card>
      </div>

      <div class="bento-bottom">
        <el-card shadow="hover" class="todo-card">
          <template #header><span class="card-title">⏰ 待处理事项</span></template>
          <div class="todo-list">
            <div class="todo-item" v-for="item in todoItems" :key="item.label" @click="$router.push(item.path)">
              <div class="todo-info">
                <span class="todo-label">{{ item.label }}</span>
                <el-tag :type="item.count > 0 ? 'danger' : 'info'" round size="small">{{ item.count }}</el-tag>
              </div>
              <el-icon class="todo-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
          <el-divider content-position="left">AI 服务</el-divider>
          <div class="ai-health">
            <el-tag :type="aiHealthStatus === 'UP' ? 'success' : 'danger'" size="small" effect="dark">
              {{ aiHealthStatus === 'UP' ? '正常' : '异常' }}
            </el-tag>
            <span class="ai-health-text">{{ aiHealthDetail }}</span>
          </div>
          <el-divider content-position="left">服务配置</el-divider>
          <div class="service-status-row">
            <div class="service-status-item">
              <span class="service-status-label">📧 邮件服务</span>
              <el-tag :type="emailConfigured ? 'success' : 'danger'" size="small">
                {{ emailConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </div>
            <div class="service-status-item">
              <span class="service-status-label">💰 支付宝</span>
              <el-tag :type="alipayConfigured ? 'success' : 'danger'" size="small">
                {{ alipayConfigured ? '已配置' : '未配置' }}
              </el-tag>
            </div>
          </div>
        </el-card>

        <el-card shadow="hover" class="log-card">
          <template #header><span class="card-title">📝 最近操作日志</span></template>
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { User, Sunny, Plus, Box, ShoppingCart, Tickets, ArrowRight } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats, getReportList, getOperationLogs, getAiHealth, getAlipayStatus, getEmailStatus } from '@/api/admin'
import { operationLabel, moduleLabel, goodsStatusLabel, orderStatusLabel } from '@/utils/labels'
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
const aiHealthStatus = ref('UP')
const aiHealthDetail = ref('')
const emailConfigured = ref(false)
const alipayConfigured = ref(false)
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
      itemStyle: { borderRadius: 8, borderColor: 'transparent', borderWidth: 2 },
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
      itemStyle: { borderRadius: 8, borderColor: 'transparent', borderWidth: 2 },
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
      goodsStatusData.value = Object.entries(res.goodsStatusMap)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: goodsStatusLabel(k), value: v }))
    } else {
      goodsStatusData.value = [{ name: '在售', value: 0 }, { name: '待审核', value: res.pendingAudit || 0 }]
    }
    if (res.orderStatusMap) {
      orderStatusData.value = Object.entries(res.orderStatusMap)
        .filter(([, v]) => v > 0)
        .map(([k, v]) => ({ name: orderStatusLabel(k), value: v }))
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

const loadAiHealth = async () => {
  try {
    const res = await getAiHealth()
    const components = res.components as Record<string, { status: string; details?: Record<string, unknown> }> | undefined
    if (components?.ai) {
      aiHealthStatus.value = components.ai.status
      const details = components.ai.details as Record<string, unknown> | undefined
      if (details) {
        const reason = details['reason'] || details['error']
        const model = details['model']
        const status = details['status']
        aiHealthDetail.value = reason ? String(reason) : `${model || 'unknown'} · ${status || ''}`
      }
    } else {
      aiHealthDetail.value = '指标未暴露'
    }
  } catch { aiHealthStatus.value = 'UNKNOWN'; aiHealthDetail.value = '无法获取' }
}

const loadServiceStatus = async () => {
  try {
    const mail = await getEmailStatus()
    emailConfigured.value = !!mail?.configured
  } catch { emailConfigured.value = false }
  try {
    const alipay = await getAlipayStatus()
    alipayConfigured.value = !!alipay?.configured
  } catch { alipayConfigured.value = false }
}

const handleResize = () => {
  goodsChart?.resize()
  orderChart?.resize()
}

onMounted(() => { loadStats(); loadRecentLogs(); loadAiHealth(); loadServiceStatus(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { goodsChart?.dispose(); orderChart?.dispose(); window.removeEventListener('resize', handleResize) })
</script>

<style scoped lang="scss">
.bento-dashboard { display: flex; flex-direction: column; gap: 20px; }

.bento-stats {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--admin-card-bg);
  border-radius: var(--admin-radius);
  padding: 20px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid var(--admin-border);
  border-left: 3px solid;
  box-shadow: var(--admin-shadow);
  transition: var(--admin-transition-slow);
  animation: fadeInUp 0.5s ease-out backwards;
  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--admin-shadow-lg);
    .stat-icon { transform: scale(1.1); }
  }
}

.stat-icon {
  width: 46px; height: 46px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  transition: var(--admin-transition);
}

.stat-value { font-size: 26px; font-weight: 800; color: var(--admin-text); line-height: 1.1; }
.stat-label { font-size: 12px; color: var(--admin-text-secondary); margin-top: 3px; font-weight: 500; }

.bento-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.chart-card { min-height: 380px; }

.bento-bottom {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}

.card-title { font-size: 15px; font-weight: 600; }

.todo-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0; border-bottom: 1px solid var(--admin-border);
  cursor: pointer; transition: all 0.2s;
  &:hover { padding-left: 8px; background: rgba(99,102,241,0.03); border-radius: 6px; }
  &:last-child { border-bottom: none; }
}

.todo-info { display: flex; align-items: center; gap: 10px; }
.todo-label { font-size: 13px; font-weight: 500; }
.ai-health { display: flex; align-items: center; gap: 8px; }
.ai-health-text { font-size: 13px; color: var(--admin-text-secondary); }
.service-status-row { display: flex; gap: 24px; }
.service-status-item { display: flex; align-items: center; gap: 8px; }
.service-status-label { font-size: 13px; color: var(--admin-text-secondary); }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .bento-stats { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .bento-stats { grid-template-columns: repeat(2, 1fr); }
  .bento-charts { grid-template-columns: 1fr; }
  .bento-bottom { grid-template-columns: 1fr; }
}
</style>

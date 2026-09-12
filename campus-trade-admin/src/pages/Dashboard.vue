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
          <template #header>
            <div class="chart-header">
              <span class="card-title">商品状态分布</span>
              <span class="chart-total" v-if="goodsStatusData.length > 0">共 {{ goodsTotal }} 件</span>
            </div>
          </template>
          <div v-if="goodsStatusData.length > 0" ref="goodsChartRef" class="chart-container" />
          <div v-else class="chart-empty">
            <el-icon :size="48" color="#cbd5e1"><Box /></el-icon>
            <p>暂无商品数据</p>
          </div>
        </el-card>
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="card-title">订单状态分布</span>
              <span class="chart-total" v-if="orderStatusData.length > 0">共 {{ orderTotal }} 笔</span>
            </div>
          </template>
          <div v-if="orderStatusData.length > 0" ref="orderChartRef" class="chart-container" />
          <div v-else class="chart-empty">
            <el-icon :size="48" color="#cbd5e1"><ShoppingCart /></el-icon>
            <p>暂无订单数据</p>
          </div>
        </el-card>
      </div>

      <div class="bento-overview">
        <div v-for="m in overviewMetrics" :key="m.label" class="overview-item">
          <div class="overview-icon" :style="{ color: m.color }">
            <el-icon :size="18"><component :is="m.icon" /></el-icon>
          </div>
          <div class="overview-info">
            <span class="overview-value" :style="{ color: m.color }">{{ m.value }}</span>
            <span class="overview-label">{{ m.label }}</span>
          </div>
        </div>
      </div>

      <div class="bento-bottom">
        <div class="bento-left">
          <el-card shadow="hover" class="todo-card">
            <template #header><span class="card-title">待处理事项</span></template>
            <div class="todo-list">
              <div class="todo-item" v-for="item in todoItems" :key="item.label" @click="$router.push(item.path)">
                <div class="todo-info">
                  <span class="todo-label">{{ item.label }}</span>
                  <el-tag :type="item.count > 0 ? 'danger' : 'info'" round size="small">{{ item.count }}</el-tag>
                </div>
                <el-icon class="todo-arrow"><ArrowRight /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="ai-card">
            <template #header>
              <div class="ai-card-header">
                <span class="card-title">AI 服务</span>
                <div class="ai-header-status">
                  <el-tag :type="aiHealthStatus === 'UP' ? 'success' : 'danger'" size="small" effect="dark" round>{{ aiHealthStatus === 'UP' ? '服务正常' : '服务异常' }}</el-tag>
                  <el-tag :type="aiStats?.embeddingAvailable ? 'success' : 'warning'" size="small" effect="dark" round>{{ aiStats?.embeddingAvailable ? '向量检索' : 'TF-IDF' }}</el-tag>
                </div>
              </div>
            </template>
            <div class="ai-model-badge" v-if="aiHealthDetail">
              <span class="ai-model-badge-text">模型：{{ aiHealthDetail }}</span>
            </div>
            <div class="ai-metrics" v-if="aiStats">
              <div class="ai-metric" style="--mc: #6366f1">
                <span class="ai-metric-val">{{ aiStats.faqCount ?? '-' }}</span>
                <span class="ai-metric-label">FAQ</span>
              </div>
              <div class="ai-metric" style="--mc: #14b8a6">
                <span class="ai-metric-val">{{ aiStats.toolCount ?? '-' }}</span>
                <span class="ai-metric-label">工具</span>
              </div>
              <div class="ai-metric" style="--mc: #f59e0b">
                <span class="ai-metric-val">{{ aiStats.avgRating ?? '-' }}</span>
                <span class="ai-metric-label">评分</span>
              </div>
            </div>
            <div class="ai-ext-services">
              <div class="ai-ext-badge" :class="emailConfigured ? 'badge-on' : 'badge-off'">
                <el-icon :size="14"><component :is="emailConfigured ? 'CircleCheck' : 'CircleClose'" /></el-icon>
                <span>邮件服务</span>
              </div>
              <div class="ai-ext-badge" :class="alipayConfigured ? 'badge-on' : 'badge-off'">
                <el-icon :size="14"><component :is="alipayConfigured ? 'CircleCheck' : 'CircleClose'" /></el-icon>
                <span>支付服务</span>
              </div>
            </div>
          </el-card>
        </div>

        <el-card shadow="hover" class="log-card">
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
            <el-table-column prop="createTime" label="时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { User, Sunny, Plus, Box, ShoppingCart, Tickets, ArrowRight, Lock, TrendCharts, Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboardStats, getReportList, getOperationLogs, getAiHealth, getAlipayStatus, getEmailStatus, getAiStats, getAiTools, getAiFeedbackList } from '@/api/admin'
import { formatDateTime } from '@/utils/labels'
import { operationLabel, moduleLabel, goodsStatusLabel, orderStatusLabel } from '@/utils/labels'
import type { OperationLogVO, PageQueryParams } from '@/types'

const stats = ref([
  { label: '用户总数', value: 0, icon: User, color: '#0EA5E9' },
  { label: '今日日活', value: 0, icon: Sunny, color: '#f59e0b' },
  { label: '今日新增', value: 0, icon: Plus, color: '#10b981' },
  { label: '商品总数', value: 0, icon: Box, color: '#14B8A6' },
  { label: '订单总数', value: 0, icon: ShoppingCart, color: '#0EA5E9' },
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
const aiStats = ref<Record<string, unknown> | null>(null)
const aiTools = ref<Record<string, unknown>[]>([])
const aiFeedbackList = ref<Record<string, unknown>[]>([])
const feedbackLoading = ref(false)
const emailConfigured = ref(false)
const alipayConfigured = ref(false)
const goodsChartRef = ref<HTMLElement>()
const orderChartRef = ref<HTMLElement>()
let goodsChart: echarts.ECharts | null = null
let orderChart: echarts.ECharts | null = null

const goodsStatusData = ref<{ name: string; value: number }[]>([])
const orderStatusData = ref<{ name: string; value: number }[]>([])
const bannedUsers = ref(0)
const pendingAudit = ref(0)
const refundCount = ref(0)

const goodsTotal = computed(() => goodsStatusData.value.reduce((s, d) => s + d.value, 0))
const orderTotal = computed(() => orderStatusData.value.reduce((s, d) => s + d.value, 0))

const overviewMetrics = computed(() => {
  const userCount = stats.value[0].value
  const todayActive = stats.value[1].value
  const todayNewUsers = stats.value[2].value
  const activeRate = userCount > 0 ? ((todayActive / userCount) * 100).toFixed(1) + '%' : '0%'
  const orderCount = stats.value[4].value
  const todayOrders = stats.value[5].value
  const orderRate = orderCount > 0 ? ((todayOrders / orderCount) * 100).toFixed(1) + '%' : '0%'
  const growthRate = userCount > 0 ? ((todayNewUsers / userCount) * 100).toFixed(1) + '%' : '0%'
  const banRate = userCount > 0 ? ((bannedUsers.value / userCount) * 100).toFixed(1) + '%' : '0%'
  return [
    { label: '用户活跃率', value: activeRate, icon: TrendCharts, color: '#0EA5E9' },
    { label: '订单活跃率', value: orderRate, icon: TrendCharts, color: '#14B8A6' },
    { label: '用户增长率', value: growthRate, icon: Plus, color: '#10b981' },
    { label: '封禁率', value: banRate, icon: Lock, color: '#8b5cf6' },
  ]
})

const initGoodsChart = () => {
  if (!goodsChartRef.value) return
  goodsChart = echarts.init(goodsChartRef.value)
  const total = goodsTotal.value
  goodsChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12, color: '#64748b' } },
    graphic: {
      type: 'text', left: 'center', top: '38%',
      style: { text: `{val|${total}}\n{lbl|总数}`, rich: { val: { fontSize: 28, fontWeight: 800, fill: '#1e293b' }, lbl: { fontSize: 12, fill: '#94a3b8', padding: [4, 0, 0, 0] } }, textAlign: 'center' }
    },
    series: [{
      type: 'pie', radius: ['48%', '72%'], center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}', color: '#475569', fontSize: 12 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' }, itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.12)' } },
      data: goodsStatusData.value
    }],
    color: ['#10b981', '#f59e0b', '#0EA5E9', '#ef4444', '#14B8A6', '#8b5cf6', '#64748b']
  })
}

const initOrderChart = () => {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  const total = orderTotal.value
  orderChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12, color: '#64748b' } },
    graphic: {
      type: 'text', left: 'center', top: '38%',
      style: { text: `{val|${total}}\n{lbl|总数}`, rich: { val: { fontSize: 28, fontWeight: 800, fill: '#1e293b' }, lbl: { fontSize: 12, fill: '#94a3b8', padding: [4, 0, 0, 0] } }, textAlign: 'center' }
    },
    series: [{
      type: 'pie', radius: ['48%', '72%'], center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}', color: '#475569', fontSize: 12 },
      emphasis: { label: { fontSize: 14, fontWeight: 'bold' }, itemStyle: { shadowBlur: 12, shadowColor: 'rgba(0,0,0,0.12)' } },
      data: orderStatusData.value
    }],
    color: ['#f59e0b', '#0EA5E9', '#10b981', '#14B8A6', '#22c55e', '#8b5cf6', '#ef4444']
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
    pendingAudit.value = res.pendingAudit || 0
    todoItems.value[0].count = res.pendingAudit || 0
    bannedUsers.value = res.bannedUsers || 0
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
      refundCount.value = refundEntry ? refundEntry.value : 0
      todoItems.value[2].count = refundCount.value
    } else {
      orderStatusData.value = [{ name: '订单', value: res.orderCount || 0 }]
    }
    await nextTick()
    if (goodsStatusData.value.length > 0) initGoodsChart()
    if (orderStatusData.value.length > 0) initOrderChart()
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
    const res = await getOperationLogs({ pageNum: 1, pageSize: 10 } as PageQueryParams)
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
  try {
    aiStats.value = await getAiStats()
  } catch { /* ignore */ }
  try {
    aiTools.value = await getAiTools()
  } catch { /* ignore */ }
  try {
    feedbackLoading.value = true
    const res = await getAiFeedbackList({ page: 1, size: 10, maxRating: 3 })
    aiFeedbackList.value = res.list || []
  } catch { /* ignore */ } finally { feedbackLoading.value = false }
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

.chart-header {
  display: flex; align-items: center; justify-content: space-between;
}
.chart-total {
  font-size: 13px; font-weight: 600; color: var(--admin-text-secondary);
  background: var(--admin-bg-light); border-radius: 6px; padding: 2px 10px;
}

.chart-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 300px; gap: 12px;
  p { font-size: 14px; color: var(--admin-text-muted); margin: 0; }
}

.bento-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  background: var(--admin-card-bg);
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  padding: 16px 20px;
  box-shadow: var(--admin-shadow);
}
.overview-item {
  display: flex; align-items: center; gap: 10px;
  padding: 6px 4px;
  border-right: 1px solid var(--admin-border-light);
  &:last-child { border-right: none; }
}
.overview-icon {
  width: 34px; height: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: var(--admin-bg-light); flex-shrink: 0;
}
.overview-info { display: flex; flex-direction: column; gap: 2px; }
.overview-value { font-size: 18px; font-weight: 800; line-height: 1; }
.overview-label { font-size: 12px; color: var(--admin-text-secondary); font-weight: 500; }

.bento-bottom {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 20px;
}
.bento-left { display: flex; flex-direction: column; gap: 20px; }

.card-title { font-size: 15px; font-weight: 600; }

.todo-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0; border-bottom: 1px solid var(--admin-border);
  cursor: pointer; transition: all 0.2s;
  &:hover { padding-left: 8px; background: rgba(14, 165, 233, 0.03); border-radius: 6px; }
  &:last-child { border-bottom: none; }
}

.todo-info { display: flex; align-items: center; gap: 10px; }
.todo-label { font-size: 13px; font-weight: 500; }

.ai-card-header { display: flex; align-items: center; justify-content: space-between; }
.ai-header-status { display: flex; gap: 6px; }

.ai-model-badge {
  display: inline-flex; align-items: center; gap: 6px;
  background: linear-gradient(135deg, rgba(99,102,241,0.08), rgba(20,184,166,0.08));
  border: 1px solid rgba(99,102,241,0.15);
  border-radius: 6px; padding: 5px 10px; margin-bottom: 14px;
  max-width: 100%; overflow: hidden;
}
.ai-model-badge-text {
  font-size: 12px; font-weight: 600; color: #6366f1;
  font-family: 'SF Mono', 'Fira Code', monospace;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.ai-metrics { display: flex; gap: 12px; margin-bottom: 14px; }
.ai-metric {
  display: flex; align-items: baseline; gap: 6px;
  padding: 6px 10px; flex: 1;
  border-bottom: 2px solid var(--mc, #6366f1);
}
.ai-metric-val { font-size: 20px; font-weight: 800; color: var(--mc, #6366f1); line-height: 1; }
.ai-metric-label { font-size: 12px; color: var(--admin-text-secondary); font-weight: 500; }

.ai-ext-services { display: flex; gap: 10px; padding-top: 12px; border-top: 1px solid var(--admin-border); }
.ai-ext-badge {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 4px 10px; border-radius: 6px; font-size: 13px; font-weight: 600;
  &.badge-on { background: rgba(34,197,94,0.1); color: #16a34a; border: 1px solid rgba(34,197,94,0.2); }
  &.badge-off { background: rgba(239,68,68,0.1); color: #dc2626; border: 1px solid rgba(239,68,68,0.2); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1200px) {
  .bento-stats { grid-template-columns: repeat(3, 1fr); }
  .bento-overview { grid-template-columns: repeat(2, 1fr); }
  .bento-bottom { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .bento-stats { grid-template-columns: repeat(2, 1fr); }
  .bento-overview { grid-template-columns: 1fr; }
  .bento-charts { grid-template-columns: 1fr; }
}
</style>

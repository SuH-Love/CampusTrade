<template>
  <div class="admin-page ai-dashboard">
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card" :style="{ '--card-c1': card.c1, '--card-c2': card.c2 }">
        <div class="stat-card-bg"></div>
        <div class="stat-card-icon">
          <svg v-if="card.svgPath" width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path :d="card.svgPath" /></svg>
          <el-icon v-else :size="20"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-card-content">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="info-row">
      <div class="info-card">
        <div class="info-card-header">
          <el-icon :size="16" color="#6366f1"><Cpu /></el-icon>
          <span>模型配置</span>
        </div>
        <div class="metric-grid">
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(99,102,241,0.1); color: #6366f1"><el-icon :size="20"><Cpu /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value">{{ modelShortName }}</div>
              <div class="metric-label">当前模型</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(16,185,129,0.1); color: #10b981"><el-icon :size="20"><CircleCheck /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" :style="{ color: data.enabled ? '#10b981' : '#ef4444' }">{{ data.enabled ? '运行中' : '已禁用' }}</div>
              <div class="metric-label">AI服务</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(14,165,233,0.1); color: #0EA5E9"><el-icon :size="20"><Connection /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" :style="{ color: data.embeddingAvailable ? '#10b981' : '#f59e0b' }">{{ data.embeddingAvailable ? 'Embedding' : 'TF-IDF' }}</div>
              <div class="metric-label">向量检索</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(245,158,11,0.1); color: #f59e0b"><el-icon :size="20"><Setting /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #f59e0b">{{ data.toolCount || 0 }}</div>
              <div class="metric-label">函数工具</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(33,147,176,0.1); color: #2193b0"><el-icon :size="20"><Document /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #2193b0">{{ data.faqCount || 0 }}</div>
              <div class="metric-label">FAQ数</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(131,77,155,0.1); color: #834d9b"><el-icon :size="20"><Box /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #834d9b">{{ data.knowledgeCount || 0 }}</div>
              <div class="metric-label">知识块</div>
            </div>
          </div>
        </div>
      </div>
      <div class="info-card">
        <div class="info-card-header">
          <el-icon :size="16" color="#f59e0b"><Monitor /></el-icon>
          <span>调用统计</span>
        </div>
        <div class="metric-grid">
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(99,102,241,0.1); color: #6366f1"><el-icon :size="20"><Histogram /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value">{{ data.totalApiCalls || 0 }}</div>
              <div class="metric-label">请求总数</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(16,185,129,0.1); color: #10b981"><el-icon :size="20"><CircleCheck /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #10b981">{{ data.successApiCalls || 0 }}</div>
              <div class="metric-label">成功</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(239,68,68,0.1); color: #ef4444"><el-icon :size="20"><CircleClose /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #ef4444">{{ data.failureApiCalls || 0 }}</div>
              <div class="metric-label">失败</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(245,158,11,0.1); color: #f59e0b"><el-icon :size="20"><Coin /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #f59e0b">{{ formatTokens(data.totalTokens || 0) }}</div>
              <div class="metric-label">Token用量</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(16,185,129,0.1); color: #10b981"><el-icon :size="20"><TrendCharts /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #10b981">{{ data.successRate || 0 }}%</div>
              <div class="metric-label">成功率</div>
            </div>
          </div>
          <div class="metric-cell">
            <div class="metric-icon" style="background: rgba(14,165,233,0.1); color: #0EA5E9"><el-icon :size="20"><Timer /></el-icon></div>
            <div class="metric-text">
              <div class="metric-value" style="color: #0EA5E9">{{ formatLatency(data.avgLatencyMs || 0) }}</div>
              <div class="metric-label">平均延迟</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-card-header"><span>反馈分布</span></div>
        <div v-if="(data.goodCount || 0) + (data.badCount || 0) > 0" ref="pieChartRef" class="chart-box"></div>
        <div v-else class="chart-empty">
          <el-icon :size="44" color="#cbd5e1"><ChatDotRound /></el-icon>
          <p>暂无反馈数据</p>
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-card-header"><span>近7天反馈趋势</span></div>
        <div v-if="hasTrendData" ref="trendChartRef" class="chart-box"></div>
        <div v-else class="chart-empty">
          <el-icon :size="44" color="#cbd5e1"><TrendCharts /></el-icon>
          <p>暂无趋势数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ChatDotRound, Star, TrendCharts, Cpu, Monitor, ChatLineRound, Document, Box, Histogram, CircleCheck, CircleClose, Coin, Timer, Connection, Setting } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAiDashboard } from '@/api/admin'

const data = ref<Record<string, any>>({})
const pieChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const THUMBS_UP_PATH = "M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13.17 1 6.59 8.59C6.22 8.95 6 9.45 6 10v9c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"

const modelShortName = computed(() => {
  const model = (data.value.model || '').toString()
  if (!model) return '未配置'
  const parts = model.split('/')
  return parts.length > 1 ? parts[parts.length - 1] : model
})

const statCards = computed(() => [
  { label: '总反馈', value: data.value.totalFeedback || 0, c1: '#667eea', c2: '#764ba2', icon: ChatLineRound, svgPath: null },
  { label: '好评率', value: goodRate.value + '%', c1: '#11998e', c2: '#38ef7d', icon: null, svgPath: THUMBS_UP_PATH },
  { label: '平均评分', value: data.value.avgRating || 0, c1: '#f12711', c2: '#f5af19', icon: Star, svgPath: null },
  { label: '今日反馈', value: data.value.todayFeedback || 0, c1: '#f59e0b', c2: '#f5576c', icon: ChatDotRound, svgPath: null },
])

const hasTrendData = computed(() => {
  const trend = data.value.trend || []
  return trend.length > 0
})

const goodRate = computed(() => {
  const total = (data.value.goodCount || 0) + (data.value.badCount || 0)
  if (total === 0) return '0'
  return ((data.value.goodCount || 0) / total * 100).toFixed(1)
})

const formatTokens = (n: number) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return String(n)
}

const formatLatency = (ms: number) => {
  if (ms === 0) return '-'
  if (ms >= 1000) return (ms / 1000).toFixed(1) + 's'
  return ms + 'ms'
}

const loadData = async () => {
  try {
    const res = await getAiDashboard()
    data.value = res
    await nextTick()
    renderCharts()
  } catch { /* ignore */ }
}

const renderCharts = () => {
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 5, textStyle: { fontSize: 12, color: '#64748b' } },
      series: [{
        type: 'pie', radius: ['45%', '70%'], center: ['50%', '45%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 3 },
        label: { show: true, formatter: '{b}\n{c}', color: '#475569', fontSize: 12 },
        emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(0,0,0,0.1)' }, label: { fontSize: 14, fontWeight: 'bold' } },
        data: [
          { value: data.value.goodCount || 0, name: '好评', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#11998e' }, { offset: 1, color: '#38ef7d' }]) } },
          { value: data.value.badCount || 0, name: '差评', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 1, [{ offset: 0, color: '#eb3349' }, { offset: 1, color: '#f45c43' }]) } },
        ]
      }]
    })
  }

  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    const trend = data.value.trend || []
    const dateMap = new Map<string, { good: number; bad: number }>()
    for (const item of trend) {
      const date = String(item.date)
      const rating = Number(item.rating)
      const count = Number(item.count)
      if (!dateMap.has(date)) dateMap.set(date, { good: 0, bad: 0 })
      const entry = dateMap.get(date)!
      if (rating > 0) entry.good += count
      else entry.bad += count
    }
    const dates = Array.from(dateMap.keys()).sort()
    const goodData = dates.map(d => dateMap.get(d)!.good)
    const badData = dates.map(d => dateMap.get(d)!.bad)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['好评', '差评'], bottom: 5, textStyle: { color: '#64748b' } },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '8%', containLabel: true },
      xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#64748b' } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
      series: [
        {
          name: '好评', type: 'bar', data: goodData, barWidth: '30%',
          itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#38ef7d' }, { offset: 1, color: '#11998e' }]) },
          emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(56,239,125,0.3)' } }
        },
        {
          name: '差评', type: 'bar', data: badData, barWidth: '30%',
          itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#f45c43' }, { offset: 1, color: '#eb3349' }]) },
          emphasis: { itemStyle: { shadowBlur: 12, shadowColor: 'rgba(235,51,73,0.3)' } }
        },
      ]
    })
  }
}

const handleResize = () => { pieChart?.resize(); trendChart?.resize() }

let sseSource: EventSource | null = null

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
  sseSource = new EventSource('/api/ai/dashboard/stream')
  sseSource.addEventListener('refresh', () => loadData())
  sseSource.onerror = () => { try { sseSource?.close() } catch {} }
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose(); trendChart?.dispose()
  sseSource?.close()
})
</script>

<style scoped lang="scss">
.ai-dashboard {
  display: flex; flex-direction: column; gap: 16px;
  min-height: calc(100vh - 140px);
}

.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
  flex-shrink: 0;
}
.stat-card {
  position: relative; border-radius: 12px; overflow: hidden;
  height: 88px; display: flex; align-items: center; justify-content: center;
  transition: transform 0.3s, box-shadow 0.3s;
  &:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.15); }
}
.stat-card-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, var(--card-c1), var(--card-c2));
  opacity: 0.9;
}
.stat-card-icon {
  position: absolute; top: 10px; left: 12px; z-index: 1;
  color: rgba(255,255,255,0.7);
}
.stat-card-content { position: relative; z-index: 1; text-align: center; color: #fff; }
.stat-value { font-size: 28px; font-weight: 800; line-height: 1.1; text-shadow: 0 2px 4px rgba(0,0,0,0.2); }
.stat-label { font-size: 13px; opacity: 0.85; margin-top: 3px; }

.info-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; flex-shrink: 0; }

.info-card {
  background: var(--admin-card-bg); border-radius: 12px;
  border: 1px solid var(--admin-border); overflow: hidden;
}
.info-card-header {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 16px; font-size: 14px; font-weight: 600;
  border-bottom: 1px solid var(--admin-border);
  background: linear-gradient(135deg, rgba(102,126,234,0.05), rgba(118,75,162,0.05));
}

.metric-grid {
  padding: 16px; display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px;
}
.metric-cell {
  display: flex; align-items: center; gap: 12px;
  padding: 14px; border-radius: 10px;
  background: var(--admin-bg-light, #f8fafc); border: 1px solid var(--admin-border);
  transition: transform 0.2s, box-shadow 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
}
.metric-icon {
  width: 42px; height: 42px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.metric-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.metric-value { font-size: 17px; font-weight: 800; line-height: 1.2; color: var(--admin-text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.metric-label { font-size: 12px; color: var(--admin-text-secondary); }
.code-text { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 14px; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; flex: 1; min-height: 0; }

.chart-card {
  background: var(--admin-card-bg); border-radius: 12px;
  border: 1px solid var(--admin-border); overflow: hidden;
  display: flex; flex-direction: column;
}
.chart-card-header {
  padding: 12px 16px; font-size: 14px; font-weight: 600;
  border-bottom: 1px solid var(--admin-border);
  background: linear-gradient(135deg, rgba(102,126,234,0.05), rgba(118,75,162,0.05));
  flex-shrink: 0;
}
.chart-box { flex: 1; min-height: 280px; }

.chart-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  flex: 1; min-height: 280px; gap: 10px;
  p { font-size: 14px; color: var(--admin-text-muted); margin: 0; }
}

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .info-row { grid-template-columns: 1fr; }
  .chart-row { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: 1fr; }
  .metric-grid { grid-template-columns: 1fr; }
}
</style>

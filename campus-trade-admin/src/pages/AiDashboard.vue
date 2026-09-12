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
      <div class="info-card model-card">
        <div class="info-card-header">
          <el-icon :size="16" color="#6366f1"><Cpu /></el-icon>
          <span>模型配置</span>
        </div>
        <div class="model-info">
          <div class="model-row">
            <span class="model-key">当前模型</span>
            <span class="model-val code-text">{{ (data.model || '-').toString() }}</span>
          </div>
          <div class="model-row">
            <span class="model-key">AI服务</span>
            <el-tag :type="data.enabled ? 'success' : 'danger'" size="small" effect="dark" round>{{ data.enabled ? '运行中' : '已禁用' }}</el-tag>
          </div>
          <div class="model-row">
            <span class="model-key">向量检索</span>
            <el-tag :type="data.embeddingAvailable ? 'success' : 'warning'" size="small" effect="dark" round>{{ data.embeddingAvailable ? 'Embedding' : 'TF-IDF' }}</el-tag>
          </div>
          <div class="model-row">
            <span class="model-key">工具调用</span>
            <span class="model-val">{{ data.toolCount || 0 }} 个函数工具已注册</span>
          </div>
        </div>
      </div>
      <div class="info-card api-card">
        <div class="info-card-header">
          <el-icon :size="16" color="#f59e0b"><Monitor /></el-icon>
          <span>调用统计</span>
        </div>
        <div class="api-info">
          <div class="api-row">
            <span class="api-key">请求总数</span>
            <span class="api-val">{{ data.totalApiCalls || 0 }} 次</span>
          </div>
          <div class="api-row">
            <span class="api-key">成功 / 失败</span>
            <span class="api-val"><span style="color: #10b981">{{ data.successApiCalls || 0 }}</span> / <span style="color: #ef4444">{{ data.failureApiCalls || 0 }}</span></span>
          </div>
          <div class="api-row">
            <span class="api-key">Token 用量</span>
            <span class="api-val" style="color: #f59e0b">{{ data.totalTokens || 0 }}</span>
          </div>
          <div class="api-row">
            <span class="api-key">成功率</span>
            <span class="api-val" style="color: #10b981">{{ data.successRate || 0 }}%</span>
          </div>
          <div class="api-row">
            <span class="api-key">平均延迟</span>
            <span class="api-val" style="color: #0EA5E9">{{ data.avgLatencyMs || 0 }}ms</span>
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
        <div class="chart-card-header"><span>评分概览</span></div>
        <div class="score-overview" v-if="(data.goodCount || 0) + (data.badCount || 0) > 0">
          <div class="score-big">
            <span class="score-num" :style="{ color: ratioColor }">{{ data.avgRating || 0 }}</span>
            <span class="score-tag">平均评分</span>
          </div>
          <div class="score-bar-wrap">
            <div class="score-bar-label">
              <span class="bar-dot" style="background: #10b981"></span>
              <span>好评 {{ data.goodCount || 0 }}</span>
              <span class="bar-pct">{{ goodRate }}%</span>
            </div>
            <div class="score-bar-track">
              <div class="score-bar-fill" :style="{ width: goodRate + '%', background: ratioColor }"></div>
            </div>
            <div class="score-bar-label">
              <span class="bar-dot" style="background: #ef4444"></span>
              <span>差评 {{ data.badCount || 0 }}</span>
              <span class="bar-pct">{{ (100 - parseFloat(goodRate)).toFixed(1) }}%</span>
            </div>
          </div>
        </div>
        <div v-else class="chart-empty">
          <el-icon :size="44" color="#cbd5e1"><Star /></el-icon>
          <p>暂无评分数据</p>
        </div>
      </div>
    </div>

    <div class="bottom-row">
      <div class="chart-card trend-card">
        <div class="chart-card-header"><span>近7天反馈趋势</span></div>
        <div v-if="hasTrendData" ref="trendChartRef" class="trend-box"></div>
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
import { ChatDotRound, Star, TrendCharts, Cpu, Monitor, ChatLineRound, Document, Box } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAiDashboard } from '@/api/admin'

const data = ref<Record<string, any>>({})
const pieChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let pieChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const THUMBS_UP_PATH = "M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13.17 1 6.59 8.59C6.22 8.95 6 9.45 6 10v9c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"
const THUMBS_DOWN_PATH = "M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2z"

const statCards = computed(() => [
  { label: '总反馈', value: data.value.totalFeedback || 0, c1: '#667eea', c2: '#764ba2', icon: ChatLineRound, svgPath: null },
  { label: '好评', value: data.value.goodCount || 0, c1: '#11998e', c2: '#38ef7d', icon: null, svgPath: THUMBS_UP_PATH },
  { label: '差评', value: data.value.badCount || 0, c1: '#eb3349', c2: '#f45c43', icon: null, svgPath: THUMBS_DOWN_PATH },
  { label: '平均评分', value: data.value.avgRating || 0, c1: '#f12711', c2: '#f5af19', icon: Star, svgPath: null },
  { label: 'FAQ数', value: data.value.faqCount || 0, c1: '#2193b0', c2: '#6dd5ed', icon: Document, svgPath: null },
  { label: '知识块', value: data.value.knowledgeCount || 0, c1: '#834d9b', c2: '#d04ed4', icon: Box, svgPath: null },
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

const ratioColor = computed(() => {
  const rate = parseFloat(goodRate.value)
  if (rate >= 70) return '#10b981'
  if (rate >= 40) return '#f59e0b'
  return '#ef4444'
})


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
      legend: { bottom: 0, textStyle: { fontSize: 12, color: '#64748b' } },
      series: [{
        type: 'pie', radius: ['45%', '72%'], center: ['50%', '45%'],
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
      legend: { data: ['好评', '差评'], bottom: 0, textStyle: { color: '#64748b' } },
      grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true },
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

onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose(); trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.ai-dashboard { display: flex; flex-direction: column; gap: 16px; }

.stat-grid {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px;
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

.info-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

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

.model-info { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
.model-row { display: flex; justify-content: space-between; align-items: center; }
.model-key { font-size: 13px; color: var(--admin-text-secondary); font-weight: 500; }
.model-val {
  font-size: 13px; font-weight: 600; color: var(--admin-text);
  max-width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.code-text { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 12px; }

.api-info { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; }
.api-row { display: flex; justify-content: space-between; align-items: center; }
.api-key { font-size: 13px; color: var(--admin-text-secondary); font-weight: 500; }
.api-val { font-size: 13px; font-weight: 600; color: var(--admin-text); }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.bottom-row { display: grid; grid-template-columns: 1fr; gap: 16px; }

.chart-card {
  background: var(--admin-card-bg); border-radius: 12px;
  border: 1px solid var(--admin-border); overflow: hidden;
}
.chart-card-header {
  padding: 12px 16px; font-size: 14px; font-weight: 600;
  border-bottom: 1px solid var(--admin-border);
  background: linear-gradient(135deg, rgba(102,126,234,0.05), rgba(118,75,162,0.05));
}
.chart-box { height: 260px; }
.trend-box { height: 260px; padding: 8px; }

.chart-empty {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 260px; gap: 10px;
  p { font-size: 14px; color: var(--admin-text-muted); margin: 0; }
}

.score-overview {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 20px 24px; height: 260px; gap: 20px;
}
.score-big { display: flex; align-items: baseline; gap: 8px; }
.score-num { font-size: 42px; font-weight: 800; line-height: 1; }
.score-tag { font-size: 14px; color: var(--admin-text-secondary); font-weight: 500; }
.score-bar-wrap { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.score-bar-label {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--admin-text-secondary);
  .bar-pct { margin-left: auto; font-weight: 600; color: var(--admin-text); }
}
.bar-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.score-bar-track {
  height: 10px; background: var(--admin-bg-light); border-radius: 5px; overflow: hidden;
}
.score-bar-fill {
  height: 100%; border-radius: 5px; transition: width 0.6s ease;
}

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
  .info-row { grid-template-columns: 1fr; }
  .chart-row { grid-template-columns: 1fr; }
  .bottom-row { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

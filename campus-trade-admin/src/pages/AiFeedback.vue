<template>
  <div class="admin-page">
    <el-tabs v-model="activeTab" class="feedback-tabs">
      <el-tab-pane label="反馈列表" name="list">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>AI 反馈列表</h3>
          <div class="header-actions">
            <el-radio-group v-model="filter" @change="loadData">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="good">有帮助</el-radio-button>
              <el-radio-button value="bad">无帮助</el-radio-button>
            </el-radio-group>
            <el-button type="success" size="small" @click="openRlhfExport">RLHF导出</el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" stripe v-loading="loading" size="default" @row-dblclick="showDetail">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="rating" label="评价" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.rating > 0 ? 'success' : 'danger'" size="small" class="rating-tag">
              <svg v-if="row.rating > 0" width="13" height="13" viewBox="0 0 24 24" fill="currentColor" style="margin-right: 4px"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13.17 1 6.59 8.59C6.22 8.95 6 9.45 6 10v9c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
              <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="currentColor" style="margin-right: 4px"><path d="M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2zm4 0v12h4V3h-4z"/></svg>
              {{ row.rating > 0 ? '有帮助' : '无帮助' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="user_message" label="用户消息" show-overflow-tooltip min-width="200" :tooltip-options="{ popperClass: 'fb-tooltip' }" />
        <el-table-column prop="ai_response" label="AI回复" show-overflow-tooltip min-width="250" :tooltip-options="{ popperClass: 'fb-tooltip' }" />
        <el-table-column prop="feedback" label="用户反馈" show-overflow-tooltip min-width="150" :tooltip-options="{ popperClass: 'fb-tooltip' }" />
        <el-table-column prop="create_time" label="时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, prev, pager, next, sizes"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="反馈详情" width="700px">
      <template v-if="detailRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detailRow.id }}</el-descriptions-item>
          <el-descriptions-item label="评价">
            <el-tag :type="detailRow.rating > 0 ? 'success' : 'danger'" size="small" class="rating-tag">
              <svg v-if="detailRow.rating > 0" width="13" height="13" viewBox="0 0 24 24" fill="currentColor" style="margin-right: 4px"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13.17 1 6.59 8.59C6.22 8.95 6 9.45 6 10v9c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
              <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="currentColor" style="margin-right: 4px"><path d="M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2zm4 0v12h4V3h-4z"/></svg>
              {{ detailRow.rating > 0 ? '有帮助' : '无帮助' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detailRow.username }}</el-descriptions-item>
          <el-descriptions-item label="时间">{{ formatDateTime(detailRow.create_time) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">用户消息</el-divider>
        <div class="detail-text">{{ detailRow.user_message }}</div>
        <el-divider content-position="left">AI 回复</el-divider>
        <div class="detail-text">{{ detailRow.ai_response }}</div>
        <template v-if="detailRow.feedback">
          <el-divider content-position="left">用户反馈</el-divider>
          <div class="detail-text">{{ detailRow.feedback }}</div>
        </template>
      </template>
    </el-dialog>

    <el-dialog v-model="rlhfVisible" title="RLHF 数据导出" width="800px">
      <div class="rlhf-info">
        <el-alert title="RLHF数据可用于模型微调，每条数据包含用户问题(prompt)、AI回复(completion)和评分(score)" type="info" :closable="false" show-icon />
        <div class="rlhf-actions">
          <el-input-number v-model="rlhfLimit" :min="10" :max="1000" :step="50" size="small" />
          <el-button type="primary" size="small" @click="loadRlhfData" :loading="rlhfLoading">加载数据</el-button>
          <el-button type="success" size="small" @click="downloadRlhfJson" :disabled="!rlhfData.length">下载JSON</el-button>
        </div>
      </div>
      <el-table :data="rlhfData" stripe size="small" max-height="400" v-if="rlhfData.length">
        <el-table-column prop="prompt" label="Prompt" show-overflow-tooltip min-width="200" />
        <el-table-column prop="completion" label="Completion" show-overflow-tooltip min-width="250" />
        <el-table-column prop="score" label="Score" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.score > 0 ? 'success' : 'danger'" size="small">{{ row.score }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="rlhf-empty">暂无数据，点击"加载数据"获取</div>
    </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="差评分析" name="analysis">
    <el-card v-loading="analysisLoading">
      <template #header>
        <div class="admin-card-header">
          <h3>差评自动分析</h3>
          <el-button size="small" @click="loadAnalysis">刷新</el-button>
        </div>
      </template>
      <el-alert title="用户给出差评后，AI自动分析可能的原因和改进建议，结果保留7天" type="info" :closable="false" show-icon class="analysis-alert" />
      <div v-if="analysisList.length === 0" class="analysis-empty">暂无差评分析数据</div>
      <div v-else class="analysis-list">
        <div v-for="(item, idx) in analysisList" :key="idx" class="analysis-item">
          <div class="analysis-item-header">
            <el-tag type="danger" size="small">差评</el-tag>
            <span class="analysis-session">会话: {{ String(item.sessionId).substring(0, 12) }}...</span>
            <span class="analysis-time">{{ formatTimestamp(item.timestamp) }}</span>
          </div>
          <template v-if="parseAnalysis(item.analysis)">
            <div class="analysis-reasons">
              <div class="analysis-section-title">可能原因：</div>
              <ul>
                <li v-for="(reason, ri) in parseAnalysis(item.analysis)?.reasons" :key="ri">{{ reason }}</li>
              </ul>
            </div>
            <div class="analysis-suggestion">
              <div class="analysis-section-title">改进建议：</div>
              <p>{{ parseAnalysis(item.analysis)?.suggestion }}</p>
            </div>
          </template>
          <div v-else class="analysis-raw">{{ item.analysis }}</div>
        </div>
      </div>
    </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiFeedbackList, exportRlhfData, getAiFeedbackAnalysis } from '@/api/admin'
import { formatDateTime } from '@/utils/labels'

const activeTab = ref('list')
const list = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filter = ref('all')

const detailVisible = ref(false)
const detailRow = ref<Record<string, any> | null>(null)

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, number> = { page: pageNum.value, size: pageSize.value }
    if (filter.value === 'good') { params.minRating = 1 }
    if (filter.value === 'bad') { params.maxRating = -1 }
    const res = await getAiFeedbackList(params)
    list.value = res.list || []
    total.value = res.total || 0
  } catch { /* ignore */ } finally { loading.value = false }
}

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}

const showDetail = (row: Record<string, unknown>) => {
  detailRow.value = row
  detailVisible.value = true
}

const rlhfVisible = ref(false)
const rlhfData = ref<Record<string, any>[]>([])
const rlhfLoading = ref(false)
const rlhfLimit = ref(100)

const openRlhfExport = () => {
  rlhfVisible.value = true
  if (!rlhfData.value.length) loadRlhfData()
}

const loadRlhfData = async () => {
  rlhfLoading.value = true
  try {
    const res = await exportRlhfData({ offset: 0, limit: rlhfLimit.value })
    rlhfData.value = res.data || []
  } catch { ElMessage.error('加载失败') } finally { rlhfLoading.value = false }
}

const downloadRlhfJson = () => {
  const blob = new Blob([JSON.stringify(rlhfData.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `rlhf_export_${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadData)

const analysisList = ref<Record<string, any>[]>([])
const analysisLoading = ref(false)
const analysisLoaded = ref(false)

const loadAnalysis = async () => {
  analysisLoading.value = true
  try {
    analysisList.value = await getAiFeedbackAnalysis() || []
    analysisLoaded.value = true
  } catch { ElMessage.error('加载分析数据失败') } finally { analysisLoading.value = false }
}

const parseAnalysis = (text: string): { reasons: string[]; suggestion: string } | null => {
  try {
    const jsonStr = text.replace(/^[^{]*\{/, '{').replace(/\}[^}]*$/, '}')
    return JSON.parse(jsonStr)
  } catch { return null }
}

const formatTimestamp = (ts: number) => {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

watch(activeTab, (tab) => {
  if (tab === 'analysis' && !analysisLoaded.value) loadAnalysis()
})
</script>

<style scoped lang="scss">
.rating-tag { display: inline-flex; align-items: center; }
.detail-text {
  white-space: pre-wrap; word-wrap: break-word;
  max-height: 300px; overflow-y: auto;
  padding: 12px; border-radius: 6px;
  background: var(--el-fill-color-light);
  font-size: 14px; line-height: 1.6;
}
.rlhf-info { margin-bottom: 16px; }
.rlhf-actions { display: flex; align-items: center; gap: 8px; margin-top: 12px; }
.rlhf-empty { text-align: center; padding: 40px; color: var(--el-text-color-secondary); }
.feedback-tabs { :deep(.el-tabs__header) { margin-bottom: 12px; } }
.analysis-alert { margin-bottom: 16px; }
.analysis-empty { text-align: center; padding: 40px; color: var(--el-text-color-secondary); }
.analysis-list { display: flex; flex-direction: column; gap: 12px; }
.analysis-item {
  border: 1px solid var(--el-border-color); border-radius: 8px; padding: 16px;
  background: var(--el-fill-color-light);
}
.analysis-item-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.analysis-session { font-size: 12px; color: var(--el-text-color-secondary); font-family: monospace; }
.analysis-time { font-size: 12px; color: var(--el-text-color-secondary); margin-left: auto; }
.analysis-section-title { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.analysis-reasons ul { margin: 0; padding-left: 20px; }
.analysis-reasons li { font-size: 13px; line-height: 1.8; }
.analysis-suggestion p { font-size: 13px; line-height: 1.6; margin: 0; color: #6366f1; }
.analysis-raw { font-size: 13px; white-space: pre-wrap; word-break: break-all; }
</style>

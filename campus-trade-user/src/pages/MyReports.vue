<template>
  <div class="my-reports-page page-bg">
    <div class="my-reports-inner">
      <div class="my-reports-header">
        <h3 class="my-reports-title">我的举报</h3>
      </div>

      <el-table :data="reports" stripe v-loading="loading" class="reports-table">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="targetType" label="类型" min-width="70">
          <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column label="举报目标" min-width="150">
          <template #default="{ row }">
            <div v-if="row.targetTitle" class="target-title">{{ row.targetTitle }}</div>
            <div v-else class="text-placeholder">ID: {{ row.targetId }}</div>
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
                class="report-thumb"
                alt="证据图片"
              />
            </div>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详细描述" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'">{{ reportStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" min-width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" min-width="150" />
        <el-table-column label="操作" min-width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <EmptyState v-if="reports.length === 0 && !loading" icon="🛡️" title="暂无举报记录" description="如果遇到违规行为，可以随时举报" />
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </div>

    <el-dialog v-model="detailVisible" title="举报详情" width="560px" class="report-detail-dialog" destroy-on-close>
      <div v-if="detailData" class="report-detail">
        <div class="detail-row">
          <span class="detail-label">举报ID</span>
          <span class="detail-value">{{ detailData.id }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">类型</span>
          <span class="detail-value">{{ targetTypeLabel(detailData.targetType) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">举报目标</span>
          <span class="detail-value">{{ detailData.targetTitle || 'ID: ' + detailData.targetId }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">原因</span>
          <span class="detail-value">{{ detailData.reason }}</span>
        </div>
        <div class="detail-row" v-if="detailData.description">
          <span class="detail-label">详细描述</span>
          <span class="detail-value">{{ detailData.description }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="statusTagMap[detailData.status] || 'info'">{{ reportStatusLabel(detailData.status) }}</el-tag>
        </div>
        <div class="detail-row" v-if="detailData.handleResult">
          <span class="detail-label">处理结果</span>
          <span class="detail-value">{{ detailData.handleResult }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交时间</span>
          <span class="detail-value">{{ detailData.createTime }}</span>
        </div>
        <div v-if="detailData.images" class="detail-images-section">
          <span class="detail-label">证据图片</span>
          <div class="detail-images">
            <el-image
              v-for="(img, idx) in detailData.images.split(',').filter(Boolean)"
              :key="idx"
              :src="img"
              :preview-src-list="detailData.images.split(',').filter(Boolean)"
              :initial-index="idx"
              fit="cover"
              class="detail-image"
              alt="证据图片"
            />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMyReports } from '@/api/report'
import EmptyState from '@/components/EmptyState.vue'
import { reportStatusLabel } from '@/utils/labels'
import type { ReportVO } from '@/api/report'

const reports = ref<ReportVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailData = ref<ReportVO | null>(null)

const statusTagMap: Record<string, string> = { PENDING: 'warning', FINISHED: '', RESOLVED: 'success', DISMISSED: 'info' }
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

const openDetail = (row: ReportVO) => {
  detailData.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.my-reports-page { padding: 20px; }
.my-reports-inner {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}
.my-reports-header { margin-bottom: 20px; }
.my-reports-title { margin: 0; }
.reports-table { width: 100%; }
.target-title { font-weight: 500; }
.text-placeholder { color: var(--text-muted); }
.report-images { display: flex; flex-wrap: wrap; gap: 2px; }
.report-thumb { width: 40px; height: 40px; border-radius: 4px; }
.list-pagination { margin-top: 20px; justify-content: center; }

.report-detail { display: flex; flex-direction: column; gap: 16px; }
.detail-row { display: flex; gap: 12px; align-items: flex-start; }
.detail-label { font-weight: 600; color: var(--text-primary); white-space: nowrap; min-width: 80px; }
.detail-value { color: var(--text-secondary); word-break: break-all; }
.detail-images-section { display: flex; flex-direction: column; gap: 8px; }
.detail-images { display: flex; flex-wrap: wrap; gap: 8px; }
.detail-image { width: 120px; height: 120px; border-radius: 8px; cursor: pointer; }

@media (max-width: 576px) {
  .my-reports-page { padding: 12px; }
  .my-reports-inner { padding: 16px; }
  .detail-image { width: 80px; height: 80px; }
}
</style>

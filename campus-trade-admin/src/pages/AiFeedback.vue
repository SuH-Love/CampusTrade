<template>
  <div class="admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>AI 反馈列表</span>
          <el-radio-group v-model="filter" @change="loadData">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="good">有帮助</el-radio-button>
            <el-radio-button value="bad">无帮助</el-radio-button>
          </el-radio-group>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAiFeedbackList } from '@/api/admin'
import { formatDateTime } from '@/utils/labels'

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

onMounted(loadData)
</script>

<style scoped lang="scss">
.card-header { display: flex; justify-content: space-between; align-items: center; }
.rating-tag { display: inline-flex; align-items: center; }
.detail-text {
  white-space: pre-wrap; word-wrap: break-word;
  max-height: 300px; overflow-y: auto;
  padding: 12px; border-radius: 6px;
  background: var(--el-fill-color-light);
  font-size: 14px; line-height: 1.6;
}
</style>

<template>
  <div class="log-center-page admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3 class="m-0">日志中心</h3>
          <el-input v-model="searchKeyword" placeholder="搜索操作人/IP" clearable class="filter-input" @clear="handleSearch" />
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="操作日志" name="operation" />
        <el-tab-pane label="安全日志" name="security" />
      </el-tabs>
      <el-table :data="logs" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column v-if="activeTab === 'operation'" prop="username" label="操作人" min-width="100" />
        <el-table-column v-if="activeTab === 'operation'" prop="operation" label="操作" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ operationLabel(row.operation) }}</template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'operation'" prop="module" label="模块" min-width="80">
          <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'security'" prop="eventType" label="事件类型" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.eventType === 'LOGIN_FAIL' || row.eventType === 'MALICIOUS_INPUT' ? 'danger' : row.eventType === 'LOGIN_SUCCESS' ? 'success' : 'warning'" size="small">{{ eventTypeLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'security'" prop="username" label="用户" min-width="100" />
        <el-table-column prop="ip" label="IP" min-width="120" />
        <el-table-column prop="detail" label="详情" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" min-width="150" />
      </el-table>
      <el-empty v-if="!loading && logs.length === 0" description="暂无日志" />
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next, sizes" :page-sizes="[10, 15, 30, 50]" @current-change="loadData" @size-change="handleSizeChange" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getOperationLogs, getSecurityLogs } from '@/api/admin'
import { operationLabel, moduleLabel, eventTypeLabel } from '@/utils/labels'
import type { OperationLogVO, SecurityLogVO, PageQueryParams } from '@/types'

type LogItem = OperationLogVO | SecurityLogVO

const activeTab = ref('operation')
const logs = ref<LogItem[]>([])
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = activeTab.value === 'operation' ? await getOperationLogs(params) : await getSecurityLogs(params)
    logs.value = res.list || []
    total.value = res.total || 0
  } catch (e) { console.error(e) } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchKeyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(handleSearch, 300)
})

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
:deep(.el-tabs__item.is-active) { color: var(--admin-primary); font-weight: 600; }
:deep(.el-tabs__active-bar) { background: var(--admin-primary); }
</style>

<template>
  <div class="log-center-page">
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="操作日志" name="operation" />
        <el-tab-pane label="安全日志" name="security" />
      </el-tabs>
      <el-table :data="logs" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column v-if="activeTab === 'operation'" prop="username" label="操作人" width="120" />
        <el-table-column v-if="activeTab === 'operation'" prop="operation" label="操作" width="200" show-overflow-tooltip />
        <el-table-column v-if="activeTab === 'operation'" prop="module" label="模块" width="100" />
        <el-table-column v-if="activeTab === 'security'" prop="eventType" label="事件类型" width="150" />
        <el-table-column v-if="activeTab === 'security'" prop="username" label="用户" width="120" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-if="logs.length === 0" description="暂无日志" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOperationLogs, getSecurityLogs } from '@/api/admin'
import type { OperationLogVO, SecurityLogVO, PageQueryParams } from '@/types'

type LogItem = OperationLogVO | SecurityLogVO

const activeTab = ref('operation')
const logs = ref<LogItem[]>([])
const pageNum = ref(1)
const pageSize = ref(15)
const total = ref(0)
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
  const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
  const res = activeTab.value === 'operation' ? await getOperationLogs(params) : await getSecurityLogs(params)
  logs.value = res.list || []
  total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.log-center-page { padding: 20px; }
</style>

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
        <el-table-column v-if="activeTab === 'operation'" prop="operation" label="操作" width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ operationLabel(row.operation) }}</template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'operation'" prop="module" label="模块" width="100">
          <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'security'" prop="eventType" label="事件类型" width="150">
          <template #default="{ row }">{{ eventTypeLabel(row.eventType) }}</template>
        </el-table-column>
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

const moduleLabel = (module: string) => {
  const map: Record<string, string> = {
    AUTH: '认证', USER: '用户', GOODS: '商品', ORDER: '订单',
    REPORT: '举报', NOTIFICATION: '通知', CHAT: '聊天', FILE: '文件',
    ADMIN: '管理', LOG: '日志', SYSTEM: '系统'
  }
  return map[module] || module
}

const operationLabel = (op: string) => {
  if (!op) return ''
  if (op.includes('login') || op.includes('Login')) return '登录'
  if (op.includes('logout') || op.includes('Logout')) return '退出'
  if (op.includes('register') || op.includes('Register')) return '注册'
  if (op.includes('create') || op.includes('Create') || op.includes('publish') || op.includes('Publish')) return '创建'
  if (op.includes('update') || op.includes('Update') || op.includes('edit') || op.includes('Edit')) return '修改'
  if (op.includes('delete') || op.includes('Delete')) return '删除'
  if (op.includes('audit') || op.includes('Audit') || op.includes('approve') || op.includes('Approve')) return '审核'
  if (op.includes('reject') || op.includes('Reject')) return '驳回'
  if (op.includes('ban') || op.includes('Ban')) return '封禁'
  if (op.includes('unban') || op.includes('Unban')) return '解封'
  if (op.includes('upload') || op.includes('Upload')) return '上传'
  if (op.includes('resolve') || op.includes('Resolve')) return '处理'
  if (op.includes('dismiss') || op.includes('Dismiss')) return '驳回'
  if (op.includes('online') || op.includes('Online')) return '上架'
  if (op.includes('offline') || op.includes('Offline')) return '下架'
  if (op.includes('submit') || op.includes('Submit')) return '提交'
  if (op.includes('pay') || op.includes('Pay')) return '支付'
  if (op.includes('ship') || op.includes('Ship')) return '发货'
  if (op.includes('finish') || op.includes('Finish') || op.includes('complete') || op.includes('Complete')) return '完成'
  if (op.includes('cancel') || op.includes('Cancel')) return '取消'
  if (op.includes('refund') || op.includes('Refund')) return '退款'
  if (op.includes('favorite') || op.includes('Favorite') || op.includes('collect') || op.includes('Collect')) return '收藏'
  return op
}

const eventTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    LOGIN_FAIL: '登录失败', LOGIN_SUCCESS: '登录成功', ACCESS_DENIED: '访问拒绝',
    TOKEN_EXPIRED: 'Token过期', RATE_LIMIT: '频率限制', MALICIOUS_INPUT: '恶意输入',
    LOGOUT: '退出登录', REGISTER: '注册'
  }
  return map[type] || type
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.log-center-page { padding: 20px; }
</style>

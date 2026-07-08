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
    Auth: '认证', User: '用户', Goods: '商品', GoodsCategory: '商品分类',
    Order: '订单', Report: '举报', Notification: '通知', Chat: '聊天',
    FileUpload: '文件上传', Admin: '管理'
  }
  return map[module] || module
}

const operationLabel = (op: string) => {
  const map: Record<string, string> = {
    register: '注册', login: '登录', logout: '退出', refreshToken: '刷新Token',
    getUserInfo: '获取用户信息', updateUserInfo: '修改用户信息', updatePassword: '修改密码',
    realNameVerify: '实名认证', uploadAvatar: '上传头像',
    createGoods: '发布商品', updateGoods: '修改商品', deleteGoods: '删除商品',
    getGoodsDetail: '查看商品详情', listGoods: '查询商品列表', hotGoods: '热门商品',
    recommendGoods: '推荐商品', submitAudit: '提交审核', onlineGoods: '上架商品',
    offlineGoods: '下架商品', favoriteGoods: '收藏商品', unfavoriteGoods: '取消收藏',
    listFavorites: '收藏列表',
    listAll: '查询全部分类',
    createOrder: '创建订单', cancelOrder: '取消订单', payOrder: '支付订单',
    shipOrder: '发货', finishOrder: '确认收货', refundOrder: '退款',
    getOrderDetail: '查看订单详情', listBuyerOrders: '买家订单列表', listSellerOrders: '卖家订单列表',
    createReport: '提交举报', listMyReports: '我的举报', handleReport: '处理举报',
    listNotifications: '通知列表', getUnreadCount: '未读数查询',
    markAsRead: '标记已读', markAllAsRead: '全部已读', deleteNotification: '删除通知',
    sendMessage: '发送消息', getHistory: '聊天记录', getRecentContacts: '最近联系人',
    uploadImage: '上传图片', deleteImage: '删除图片',
    dashboardStats: '仪表盘统计', listUsers: '用户列表', banUser: '封禁用户',
    unbanUser: '解封用户', auditGoods: '审核商品', listOrders: '订单列表',
    listReports: '举报列表', resolveReport: '处理举报', dismissReport: '驳回举报',
    listOperationLogs: '操作日志', listSecurityLogs: '安全日志'
  }
  return map[op] || op
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

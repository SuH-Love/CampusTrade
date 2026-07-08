<template>
  <div class="notification-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>通知中心</h3>
          <el-button type="primary" size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">全部已读</el-button>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane :label="`未读 (${unreadCount})`" name="unread" />
      </el-tabs>
      <el-empty v-if="notifications.length === 0" description="暂无通知" />
      <div v-else class="notification-list" v-loading="loading">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleRead(item)"
        >
          <div class="notification-dot" v-if="item.isRead === 0" />
          <div class="notification-body">
            <div class="notification-title">
              <el-tag size="small" :type="typeTagMap[item.notificationType] || 'info'">{{ typeLabel(item.notificationType) }}</el-tag>
              <span style="margin-left: 8px">{{ item.title }}</span>
            </div>
            <div class="notification-content">{{ item.content }}</div>
            <div class="notification-time">{{ item.createTime }}</div>
          </div>
          <el-button type="danger" size="small" text @click.stop="handleDelete(item.id)">删除</el-button>
        </div>
      </div>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listNotifications, getUnreadCount, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'
import type { NotificationVO } from '@/api/notification'
import { ElMessage } from 'element-plus'

const activeTab = ref('all')
const notifications = ref<NotificationVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const unreadCount = ref(0)
const loading = ref(false)

const typeTagMap: Record<string, string> = { SYSTEM: 'danger', ORDER: 'warning', GOODS: 'success', CHAT: '' }
const typeLabel = (type: string) => {
  const map: Record<string, string> = { SYSTEM: '系统', ORDER: '订单', GOODS: '商品', CHAT: '聊天' }
  return map[type] || type
}

const loadData = async () => {
  loading.value = true
  try {
    const isRead = activeTab.value === 'unread' ? 0 : undefined
    const res = await listNotifications(isRead, pageNum.value, pageSize.value)
    notifications.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try { unreadCount.value = await getUnreadCount() } catch { /* ignore */ }
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

const handleRead = async (item: NotificationVO) => {
  if (item.isRead === 0) {
    await markAsRead(item.id)
    item.isRead = 1
    loadUnreadCount()
  }
}

const handleMarkAllRead = async () => {
  await markAllAsRead()
  ElMessage.success('已全部标记为已读')
  loadData()
  loadUnreadCount()
}

const handleDelete = async (id: number) => {
  await deleteNotification(id)
  ElMessage.success('已删除')
  loadData()
  loadUnreadCount()
}

onMounted(() => { loadData(); loadUnreadCount() })
</script>

<style scoped lang="scss">
.notification-page { padding: 20px; }
.notification-list { max-height: 600px; overflow-y: auto; }
.notification-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 16px; border-bottom: 1px solid #f0f0f0; cursor: pointer;
  &:hover { background: #fafafa; }
  &.unread { background: #f0f7ff; }
}
.notification-dot { width: 8px; height: 8px; border-radius: 50%; background: #f56c6c; margin-top: 6px; flex-shrink: 0; }
.notification-body { flex: 1; }
.notification-title { display: flex; align-items: center; font-weight: 500; }
.notification-content { color: #666; margin: 6px 0; font-size: 14px; }
.notification-time { color: #999; font-size: 12px; }
</style>

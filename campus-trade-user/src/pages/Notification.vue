<template>
  <div class="notification-page page-bg">
    <div class="notification-inner">
      <div class="notification-header">
        <h3 class="notification-title">通知中心</h3>
        <div class="header-actions">
          <el-button type="primary" size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">全部已读</el-button>
          <el-button size="small" @click="handleBatchMarkRead" :disabled="selectedIds.length === 0">
            批量标记已读 ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>
      <div class="preference-section">
        <span class="preference-label">通知偏好：</span>
        <el-checkbox-group v-model="enabledTypes" @change="handlePreferenceChange">
          <el-checkbox label="SYSTEM">系统通知</el-checkbox>
          <el-checkbox label="ORDER">订单通知</el-checkbox>
          <el-checkbox label="GOODS">商品通知</el-checkbox>
          <el-checkbox label="CHAT">聊天通知</el-checkbox>
          <el-checkbox label="FOLLOW">关注通知</el-checkbox>
          <el-checkbox label="REPORT">举报通知</el-checkbox>
        </el-checkbox-group>
      </div>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane :label="`未读 (${unreadCount})`" name="unread" />
      </el-tabs>
      <EmptyState v-if="notifications.length === 0 && !loading" icon="🔔" title="暂无通知" description="暂时没有新通知，安静也是一种美好" />
      <TransitionGroup v-else name="list" tag="div" class="notification-list" v-loading="loading">
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: item.isRead === 0 }"
          @click="handleRead(item)"
        >
          <el-checkbox v-model="item._selected" class="notify-check" @click.stop />
          <div class="notification-dot" v-if="item.isRead === 0" />
          <div class="notification-body">
            <div class="notification-title-row">
              <el-tag size="small" :type="typeTagMap[item.notificationType] || 'info'">{{ typeLabel(item.notificationType) }}</el-tag>
              <span class="notify-title-text">{{ item.title }}</span>
            </div>
            <div class="notification-content">{{ item.content }}</div>
            <div class="notification-time">{{ item.createTime }}</div>
          </div>
          <el-button type="danger" size="small" text @click.stop="handleDelete(item.id)">删除</el-button>
        </div>
      </TransitionGroup>
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications, getUnreadCount, markAsRead, markAllAsRead, deleteNotification } from '@/api/notification'
import { getMyPreferences, setPreference } from '@/api/notificationPreference'
import EmptyState from '@/components/EmptyState.vue'
import type { NotificationVO } from '@/api/notification'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatWs } from '@/composables/useChatWs'

interface NotificationItem extends NotificationVO {
  _selected?: boolean
}

const router = useRouter()
const { notifyUnread, onNotification } = useChatWs()

const activeTab = ref('all')
const notifications = ref<NotificationItem[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const unreadCount = ref(0)
const loading = ref(false)

const typeTagMap: Record<string, string> = { SYSTEM: 'danger', ORDER: 'warning', GOODS: 'success', CHAT: '', FOLLOW: 'primary', REPORT: 'warning' }
const typeLabel = (type: string) => {
  const map: Record<string, string> = { SYSTEM: '系统', ORDER: '订单', GOODS: '商品', CHAT: '聊天', FOLLOW: '关注', REPORT: '举报' }
  return map[type] || type
}

const allTypes = ['SYSTEM', 'ORDER', 'GOODS', 'CHAT', 'FOLLOW', 'REPORT']
const enabledTypes = ref<string[]>([...allTypes])

const selectedIds = computed(() =>
  notifications.value.filter(n => n._selected).map(n => n.id)
)

const loadData = async () => {
  loading.value = true
  try {
    const isRead = activeTab.value === 'unread' ? 0 : undefined
    const res = await listNotifications(isRead, pageNum.value, pageSize.value)
    notifications.value = (res.list || []).map((n: NotificationVO) => ({ ...n, _selected: false }))
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const loadUnreadCount = async () => {
  try {
    const count = await getUnreadCount()
    unreadCount.value = count
    notifyUnread.value = count
  } catch { /* ignore */ }
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

const handleRead = async (item: NotificationItem) => {
  if (item.isRead === 0) {
    await markAsRead(item.id)
    item.isRead = 1
    const count = await getUnreadCount()
    unreadCount.value = count
    notifyUnread.value = count
  }
  navigateByNotification(item)
}

const navigateByNotification = (item: NotificationVO) => {
  if (!item.relatedId) return
  switch (item.notificationType) {
    case 'ORDER':
      router.push(`/order/${item.relatedId}`)
      break
    case 'GOODS':
      router.push(`/goods/${item.relatedId}`)
      break
    case 'FOLLOW':
      router.push(`/profile/${item.relatedId}`)
      break
    case 'REPORT':
      router.push('/my-reports')
      break
  }
}

const handleMarkAllRead = async () => {
  await markAllAsRead()
  ElMessage.success('已全部标记为已读')
  notifyUnread.value = 0
  loadData()
}

const handleBatchMarkRead = async () => {
  const ids = selectedIds.value
  if (ids.length === 0) return
  for (const id of ids) {
    const item = notifications.value.find(n => n.id === id)
    if (item && item.isRead === 0) {
      await markAsRead(id)
    }
  }
  ElMessage.success(`已标记 ${ids.length} 条为已读`)
  loadUnreadCount()
  loadData()
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除该通知？', '删除确认', { type: 'warning' })
  await deleteNotification(id)
  ElMessage.success('已删除')
  loadUnreadCount()
  loadData()
}

const loadPreferences = async () => {
  try {
    const prefs = await getMyPreferences()
    if (prefs && prefs.length > 0) {
      enabledTypes.value = prefs.filter(p => p.enabled === 1).map(p => p.notificationType)
    }
  } catch { /* ignore */ }
}

const handlePreferenceChange = async (val: string[]) => {
  for (const type of allTypes) {
    const enabled = val.includes(type) ? 1 : 0
    try { await setPreference(type, enabled) } catch { /* ignore */ }
  }
  ElMessage.success('偏好设置已更新')
}

const removeNotifyHandler = onNotification(() => {
  loadData()
  loadUnreadCount()
})

onMounted(() => { loadData(); loadUnreadCount(); loadPreferences() })
onUnmounted(() => { removeNotifyHandler() })
</script>

<style scoped lang="scss">
.notification-page { padding: 20px; }
.notification-inner {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}
.notification-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.notification-title { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.notification-list { max-height: 600px; overflow-y: auto; }
.notification-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 16px; border-bottom: 1px solid var(--border-light); cursor: pointer;
  transition: var(--transition-fast);
  &:hover { background: var(--bg-hover); border-radius: var(--radius-sm); }
  &.unread { background: var(--primary-lighter); border-radius: var(--radius-sm); }
}
.notify-check { flex-shrink: 0; margin-top: 2px; }
.notification-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--danger); margin-top: 6px; flex-shrink: 0; }
.notification-body { flex: 1; }
.notification-title-row { display: flex; align-items: center; font-weight: 600; }
.notify-title-text { margin-left: 8px; }
.notification-content { color: var(--text-secondary); margin: 6px 0; font-size: 14px; line-height: 1.6; }
.notification-time { color: var(--text-muted); font-size: 12px; }
.preference-section {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 18px; margin-bottom: 14px;
  background: var(--bg-glass);
  backdrop-filter: blur(8px);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
}
.preference-label { font-size: 14px; font-weight: 600; color: var(--text-primary); white-space: nowrap; }
.list-pagination { margin-top: 20px; justify-content: center; }

@media (max-width: 576px) {
  .notification-page { padding: 12px; }
  .notification-inner { padding: 16px; }
  .notification-header { flex-direction: column; align-items: flex-start; }
  .preference-section { flex-wrap: wrap; }
}
</style>

<template>
  <div class="notification-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>通知中心</h3>
          <el-button type="primary" size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">全部已读</el-button>
        </div>
      </template>
      <div class="preference-section">
        <span class="preference-label">通知偏好：</span>
        <el-checkbox-group v-model="enabledTypes" @change="handlePreferenceChange">
          <el-checkbox label="SYSTEM">系统通知</el-checkbox>
          <el-checkbox label="ORDER">订单通知</el-checkbox>
          <el-checkbox label="GOODS">商品通知</el-checkbox>
          <el-checkbox label="CHAT">聊天通知</el-checkbox>
          <el-checkbox label="FOLLOW">关注通知</el-checkbox>
        </el-checkbox-group>
      </div>
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
import { getMyPreferences, setPreference } from '@/api/notificationPreference'
import type { NotificationVO } from '@/api/notification'
import { ElMessage } from 'element-plus'
import { useChatWs } from '@/composables/useChatWs'

const { notifyUnread } = useChatWs()

const activeTab = ref('all')
const notifications = ref<NotificationVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const unreadCount = ref(0)
const loading = ref(false)

const typeTagMap: Record<string, string> = { SYSTEM: 'danger', ORDER: 'warning', GOODS: 'success', CHAT: '', FOLLOW: 'primary' }
const typeLabel = (type: string) => {
  const map: Record<string, string> = { SYSTEM: '系统', ORDER: '订单', GOODS: '商品', CHAT: '聊天', FOLLOW: '关注' }
  return map[type] || type
}

const allTypes = ['SYSTEM', 'ORDER', 'GOODS', 'CHAT', 'FOLLOW']
const enabledTypes = ref<string[]>([...allTypes])

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
  notifyUnread.value = 0
  loadData()
}

const handleDelete = async (id: number) => {
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

onMounted(() => { loadData(); loadUnreadCount(); loadPreferences() })
</script>

<style scoped lang="scss">
.notification-page { padding: 20px; }
.notification-list { max-height: 600px; overflow-y: auto; }
.notification-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 16px; border-bottom: 1px solid var(--border-light); cursor: pointer;
  transition: var(--transition-fast);
  &:hover { background: var(--bg-hover); border-radius: var(--radius-sm); }
  &.unread { background: var(--primary-lighter); border-radius: var(--radius-sm); }
}
.notification-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--danger); margin-top: 6px; flex-shrink: 0; }
.notification-body { flex: 1; }
.notification-title { display: flex; align-items: center; font-weight: 600; }
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
</style>

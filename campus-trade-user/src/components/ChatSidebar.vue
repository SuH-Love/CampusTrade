<template>
  <el-aside width="300px" class="chat-sidebar">
    <div class="sidebar-header">
      <span>消息</span>
      <el-tag v-if="connected" type="success" size="small" effect="dark">在线</el-tag>
      <el-tag v-else type="info" size="small" effect="dark">离线</el-tag>
    </div>
    <div class="sidebar-search">
      <el-input :model-value="searchKeyword" placeholder="搜索联系人..." size="small" clearable @update:model-value="emit('search', $event)" />
    </div>
    <div class="contact-list">
      <div v-for="contact in filteredContacts" :key="contact.userId" class="contact-item" :class="{ active: currentTarget === contact.userId }" @click="emit('select', contact)">
        <div class="avatar-wrap">
          <el-avatar :size="44" :src="contact.avatar" />
          <span v-if="onlineUsers.has(contact.userId)" class="online-dot"></span>
          <span v-if="contact.unread" class="unread-badge">{{ contact.unread > 99 ? '99+' : contact.unread }}</span>
        </div>
        <div class="contact-info">
          <div class="contact-name" @click.stop="router.push(`/profile/${contact.userId}`)">{{ contact.name }}</div>
          <div class="contact-last">{{ contact.lastMessage }}</div>
        </div>
        <el-button size="small" type="danger" plain round @click.stop="emit('block', contact)" title="屏蔽" class="block-btn">
          <el-icon class="icon-mr-xs"><Close /></el-icon>屏蔽
        </el-button>
      </div>
    </div>
    <el-empty v-if="filteredContacts.length === 0" description="暂无会话" :image-size="60" />
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { ContactItem } from '@/types'

const props = defineProps<{
  contacts: ContactItem[]
  currentTarget: number | null
  onlineUsers: Set<number>
  searchKeyword: string
  connected: boolean
}>()

const emit = defineEmits<{
  select: [contact: ContactItem]
  search: [keyword: string]
  block: [contact: ContactItem]
}>()

const router = useRouter()

const filteredContacts = computed(() => {
  if (!props.searchKeyword.trim()) return props.contacts
  const kw = props.searchKeyword.trim().toLowerCase()
  return props.contacts.filter(c => c.name.toLowerCase().includes(kw))
})
</script>

<style scoped lang="scss">
.chat-sidebar {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-right: 1px solid var(--border);
  border-radius: var(--radius-lg) 0 0 var(--radius-lg);
  overflow-y: auto;
}
.sidebar-header {
  padding: 20px;
  font-size: 18px;
  font-weight: 700;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sidebar-search {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}
.contact-list { padding: 4px 0; }
.contact-item {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px; cursor: pointer; transition: var(--transition-fast);
  &:hover { background: var(--bg-hover); }
  &.active { background: var(--primary-lighter); }
}
.avatar-wrap { position: relative; flex-shrink: 0; }
.online-dot { position: absolute; bottom: 1px; right: 1px; width: 10px; height: 10px; background: #22c55e; border: 2px solid var(--bg-card); border-radius: 50%; }
.unread-badge { position: absolute; top: -2px; right: -6px; min-width: 18px; height: 18px; background: var(--danger); color: #fff; font-size: 11px; font-weight: 600; border-radius: 9px; display: flex; align-items: center; justify-content: center; padding: 0 4px; border: 2px solid var(--bg-card); }
.contact-info { flex: 1; overflow: hidden; }
.contact-name { font-weight: 600; font-size: 14px; cursor: pointer; }
.contact-last { font-size: 12px; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }
.block-btn { flex-shrink: 0; font-size: 12px; }
.icon-mr-xs { margin-right: 2px; }
</style>
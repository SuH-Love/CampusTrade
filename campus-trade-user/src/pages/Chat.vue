<template>
  <div class="chat-page">
    <el-container style="height: calc(100vh - 112px)">
      <el-aside width="300px" class="chat-sidebar">
        <div class="sidebar-header">消息</div>
        <div class="contact-list">
          <div v-for="contact in contacts" :key="contact.userId" class="contact-item" :class="{ active: currentTarget === contact.userId }" @click="selectContact(contact)">
            <el-avatar :size="44" :src="contact.avatar" />
            <div class="contact-info">
              <div class="contact-name">{{ contact.name }}</div>
              <div class="contact-last">{{ contact.lastMessage }}</div>
            </div>
          </div>
        </div>
        <el-empty v-if="contacts.length === 0" description="暂无会话" :image-size="60" />
      </el-aside>
      <el-main class="chat-main">
        <template v-if="currentTarget">
          <div class="chat-header">{{ currentContactName }}</div>
          <div class="chat-messages" ref="messagesRef">
            <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ self: msg.senderId === myId }">
              <template v-if="msg.senderId === myId">
                <div class="msg-bubble self-bubble">{{ msg.content }}</div>
              </template>
              <template v-else>
                <el-avatar :size="36" :src="msg.senderAvatar" />
                <div>
                  <div class="sender-name">{{ msg.senderName }}</div>
                  <div class="msg-bubble">{{ msg.content }}</div>
                </div>
              </template>
            </div>
            <el-empty v-if="messages.length === 0" description="暂无消息，发送第一条消息吧" :image-size="60" />
          </div>
          <div class="chat-input">
            <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="handleSend" size="large" />
            <el-button type="primary" size="large" @click="handleSend" :disabled="!inputText.trim()" :loading="sending" round>发送</el-button>
          </div>
        </template>
        <div v-else class="chat-empty"><el-empty description="选择联系人开始聊天" /></div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getRecentContacts, getHistory, sendMessage, markAsRead } from '@/api/chat'
import type { ChatMessageVO } from '@/api/chat'
import type { ContactVO } from '@/types'

const route = useRoute()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id)

interface ContactItem { userId: number; name: string; avatar: string; lastMessage: string }

const contacts = ref<ContactItem[]>([])
const currentTarget = ref<number | null>(null)
const currentContactName = ref('')
const messages = ref<ChatMessageVO[]>([])
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLElement>()

const formatTime = (t: string) => { if (!t) return ''; const d = new Date(t); if (isNaN(d.getTime())) return t; const pad = (n: number) => String(n).padStart(2, '0'); return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}` }

const loadContacts = async () => {
  try {
    const res = await getRecentContacts()
    const list: ContactVO[] = Array.isArray(res) ? res : ((res as { list?: ContactVO[] }).list || [])
    contacts.value = list.map((c) => {
      const isMeSender = c.senderId === myId.value
      return { userId: isMeSender ? c.receiverId : c.senderId, name: isMeSender ? (c.receiverName || '用户' + c.receiverId) : (c.senderName || '用户' + c.senderId), avatar: isMeSender ? (c.receiverAvatar || '') : (c.senderAvatar || ''), lastMessage: c.content || '' }
    })
  } catch { contacts.value = [] }
}

const selectContact = async (contact: ContactItem) => { currentTarget.value = contact.userId; currentContactName.value = contact.name; await loadMessages(); try { await markAsRead(contact.userId) } catch { /* ignore */ } }

const loadMessages = async () => { if (!currentTarget.value) return; try { const res = await getHistory(currentTarget.value); messages.value = res.list || res || []; await nextTick(); scrollToBottom() } catch { messages.value = [] } }

const handleSend = async () => { if (!inputText.value.trim() || !currentTarget.value) return; sending.value = true; try { await sendMessage({ receiverId: currentTarget.value, content: inputText.value.trim() }); inputText.value = ''; await loadMessages() } finally { sending.value = false } }

const scrollToBottom = () => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }

onMounted(async () => {
  await loadContacts()
  if (route.query.targetUserId) {
    const targetId = Number(route.query.targetUserId); const targetName = route.query.name as string || '卖家'
    const existing = contacts.value.find(c => c.userId === targetId)
    if (existing) { selectContact(existing) } else { contacts.value.unshift({ userId: targetId, name: targetName, avatar: '', lastMessage: '' }); currentTarget.value = targetId; currentContactName.value = targetName }
  }
})
</script>

<style scoped lang="scss">
.chat-page { padding: 0 24px; }
.chat-sidebar { background: var(--bg-card); border-right: 1px solid var(--border); border-radius: var(--radius-md) 0 0 var(--radius-md); overflow-y: auto; }
.sidebar-header { padding: 20px; font-size: 18px; font-weight: 700; border-bottom: 1px solid var(--border); }
.contact-list { padding: 4px 0; }
.contact-item { display: flex; align-items: center; gap: 12px; padding: 14px 16px; cursor: pointer; transition: var(--transition); &:hover { background: var(--bg-hover); } &.active { background: var(--primary-lighter); } }
.contact-info { flex: 1; overflow: hidden; }
.contact-name { font-weight: 600; font-size: 14px; }
.contact-last { font-size: 12px; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }

.chat-main { display: flex; flex-direction: column; padding: 0; background: var(--bg-card); border-radius: 0 var(--radius-md) var(--radius-md) 0; }
.chat-header { padding: 16px 20px; border-bottom: 1px solid var(--border); font-weight: 700; font-size: 16px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.message-item { display: flex; gap: 10px; &.self { justify-content: flex-end; } }
.sender-name { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.msg-bubble { background: #f1f5f9; padding: 10px 16px; border-radius: 16px 16px 16px 4px; word-break: break-all; max-width: 60%; font-size: 14px; line-height: 1.6; }
.self-bubble { background: var(--primary); color: #fff; border-radius: 16px 16px 4px 16px; }
.chat-input { display: flex; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--border); }
.chat-empty { display: flex; align-items: center; justify-content: center; height: 100%; }
</style>

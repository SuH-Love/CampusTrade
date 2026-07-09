<template>
  <div class="chat-page">
    <el-container style="height: calc(100vh - 112px)">
      <el-aside width="300px" class="chat-sidebar">
        <div class="sidebar-header">
          <span>消息</span>
          <el-tag v-if="connected" type="success" size="small" effect="dark">在线</el-tag>
          <el-tag v-else type="info" size="small" effect="dark">离线</el-tag>
        </div>
        <div class="contact-list">
          <div v-for="contact in contacts" :key="contact.userId" class="contact-item" :class="{ active: currentTarget === contact.userId }" @click="selectContact(contact)">
            <div class="avatar-wrap">
              <el-avatar :size="44" :src="contact.avatar" />
              <span v-if="isOnline(contact.userId)" class="online-dot"></span>
              <span v-if="contact.unread" class="unread-badge">{{ contact.unread > 99 ? '99+' : contact.unread }}</span>
            </div>
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
          <div class="chat-header">
            <span>{{ currentContactName }}</span>
            <span v-if="isOnline(currentTarget)" class="header-online">在线</span>
            <span v-else class="header-offline">离线</span>
          </div>
          <div class="chat-messages" ref="messagesRef">
            <div v-for="msg in messages" :key="msg.id" class="message-item" :class="{ self: msg.senderId === myId }">
              <template v-if="msg.senderId === myId">
                <div class="msg-wrap self-wrap">
                  <div class="msg-bubble self-bubble">{{ msg.content }}</div>
                  <div class="msg-meta">
                    <span class="msg-time">{{ formatTime(msg.createTime) }}</span>
                    <span v-if="msg.isRead" class="msg-read">已读</span>
                    <span v-else class="msg-unread">未读</span>
                  </div>
                </div>
              </template>
              <template v-else>
                <el-avatar :size="36" :src="msg.senderAvatar" />
                <div class="msg-wrap">
                  <div class="sender-name">{{ msg.senderName }}</div>
                  <div class="msg-bubble">{{ msg.content }}</div>
                  <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
                </div>
              </template>
            </div>
            <div v-if="typingHint" class="typing-hint">{{ typingHint }}</div>
            <el-empty v-if="messages.length === 0" description="暂无消息，发送第一条消息吧" :image-size="60" />
          </div>
          <div class="chat-input">
            <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="handleSend" @input="handleTyping" size="large" />
            <el-button type="primary" size="large" @click="handleSend" :disabled="!inputText.trim()" :loading="sending" round>发送</el-button>
          </div>
        </template>
        <div v-else class="chat-empty"><el-empty description="选择联系人开始聊天" /></div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getRecentContacts, getHistory } from '@/api/chat'
import { useChatWs } from '@/composables/useChatWs'
import type { ChatMessageVO } from '@/api/chat'
import type { ContactVO } from '@/types'

const route = useRoute()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id)

const { connected, onlineUsers, sendChat, sendTyping, sendStopTyping, sendRead, onMessage, unreadMap } = useChatWs()

interface ContactItem { userId: number; name: string; avatar: string; lastMessage: string; unread: number }

const contacts = ref<ContactItem[]>([])
const currentTarget = ref<number | null>(null)
const currentContactName = ref('')
const messages = ref<ChatMessageVO[]>([])
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLElement>()
const typingHint = ref('')
let lastTypingSent = false

const isOnline = (userId: number) => onlineUsers.value.has(userId)

const loadContacts = async () => {
  try {
    const res = await getRecentContacts()
    const list: ContactVO[] = Array.isArray(res) ? res : ((res as { list?: ContactVO[] }).list || [])
    contacts.value = list.map((c) => {
      const isMeSender = c.senderId === myId.value
      const partnerId = isMeSender ? c.receiverId : c.senderId
      return {
        userId: partnerId,
        name: isMeSender ? (c.receiverName || '用户' + c.receiverId) : (c.senderName || '用户' + c.senderId),
        avatar: isMeSender ? (c.receiverAvatar || '') : (c.senderAvatar || ''),
        lastMessage: c.content || '',
        unread: unreadMap.value.get(partnerId) || 0
      }
    })
  } catch { contacts.value = [] }
}

const selectContact = async (contact: ContactItem) => {
  currentTarget.value = contact.userId
  currentContactName.value = contact.name
  contact.unread = 0
  await loadMessages()
  sendRead(contact.userId)
}

const loadMessages = async () => {
  if (!currentTarget.value) return
  try {
    const res = await getHistory(currentTarget.value)
    messages.value = res.list || res || []
    await nextTick(); scrollToBottom()
  } catch { messages.value = [] }
}

const handleSend = async () => {
  if (!inputText.value.trim() || !currentTarget.value) return
  const content = inputText.value.trim()
  sending.value = true
  try {
    const sent = sendChat(currentTarget.value, content)
    if (!sent) {
      const { sendMessage } = await import('@/api/chat')
      await sendMessage({ receiverId: currentTarget.value, content })
      await loadMessages()
    }
    inputText.value = ''
    sendStopTyping(currentTarget.value)
    lastTypingSent = false
  } finally {
    sending.value = false
  }
}

const handleTyping = () => {
  if (!currentTarget.value) return
  if (inputText.value.trim() && !lastTypingSent) {
    sendTyping(currentTarget.value)
    lastTypingSent = true
  }
  if (!inputText.value.trim() && lastTypingSent) {
    sendStopTyping(currentTarget.value)
    lastTypingSent = false
  }
}

const scrollToBottom = () => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }

const formatTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return time
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`
}

const findOrCreateContact = (partnerId: number, name: string, avatar: string, content: string, unread = 0) => {
  const idx = contacts.value.findIndex(c => c.userId === partnerId)
  if (idx > -1) {
    const contact = contacts.value[idx]
    contact.lastMessage = content
    contact.unread = unread
    if (idx > 0) {
      contacts.value.splice(idx, 1)
      contacts.value.unshift(contact)
    }
  } else {
    contacts.value.unshift({ userId: partnerId, name, avatar, lastMessage: content, unread })
  }
}

const removeWsHandler = onMessage((msg) => {
  if (msg.type === 'CHAT' && msg.data) {
    const chatMsg = msg.data as ChatMessageVO
    const partnerId = chatMsg.senderId === myId.value ? chatMsg.receiverId : chatMsg.senderId
    const partnerName = chatMsg.senderId === myId.value ? (chatMsg.receiverName || '用户') : (chatMsg.senderName || '用户')
    const partnerAvatar = chatMsg.senderId === myId.value ? (chatMsg.receiverAvatar || '') : (chatMsg.senderAvatar || '')

    if (currentTarget.value === partnerId) {
      const exists = messages.value.some(m => m.id === chatMsg.id)
      if (!exists) {
        messages.value.push(chatMsg)
        nextTick(scrollToBottom)
      }
      if (chatMsg.senderId !== myId.value) {
        sendRead(partnerId)
      }
      findOrCreateContact(partnerId, partnerName, partnerAvatar, chatMsg.content, 0)
    } else {
      const isFromOther = chatMsg.senderId !== myId.value
      findOrCreateContact(partnerId, partnerName, partnerAvatar, chatMsg.content, isFromOther ? 1 : 0)
    }
    if (chatMsg.senderId === currentTarget.value) {
      typingHint.value = ''
    }
  } else if (msg.type === 'TYPING' && msg.userId === currentTarget.value) {
    const contact = contacts.value.find(c => c.userId === msg.userId)
    typingHint.value = `${contact?.name || '对方'} 正在输入...`
  } else if (msg.type === 'STOP_TYPING' && msg.userId === currentTarget.value) {
    typingHint.value = ''
  } else if (msg.type === 'READ' && msg.userId) {
    messages.value.forEach(m => {
      if (m.senderId === myId.value && m.receiverId === msg.userId) {
        m.isRead = 1
      }
    })
  }
})

onMounted(async () => {
  await loadContacts()
  if (route.query.targetUserId) {
    const targetId = Number(route.query.targetUserId)
    const targetName = route.query.name as string || '卖家'
    const existing = contacts.value.find(c => c.userId === targetId)
    if (existing) { selectContact(existing) } else {
      contacts.value.unshift({ userId: targetId, name: targetName, avatar: '', lastMessage: '', unread: 0 })
      currentTarget.value = targetId
      currentContactName.value = targetName
    }
  }
})

onUnmounted(() => {
  removeWsHandler()
})
</script>

<style scoped lang="scss">
.chat-page { padding: 0 24px; }
.chat-sidebar { background: var(--bg-card); border-right: 1px solid var(--border); border-radius: var(--radius-md) 0 0 var(--radius-md); overflow-y: auto; }
.sidebar-header { padding: 20px; font-size: 18px; font-weight: 700; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; }
.contact-list { padding: 4px 0; }
.contact-item { display: flex; align-items: center; gap: 12px; padding: 14px 16px; cursor: pointer; transition: var(--transition); &:hover { background: var(--bg-hover); } &.active { background: var(--primary-lighter); } }
.avatar-wrap { position: relative; flex-shrink: 0; }
.online-dot { position: absolute; bottom: 1px; right: 1px; width: 10px; height: 10px; background: #22c55e; border: 2px solid var(--bg-card); border-radius: 50%; }
.unread-badge { position: absolute; top: -2px; right: -6px; min-width: 18px; height: 18px; background: #f56c6c; color: #fff; font-size: 11px; font-weight: 600; border-radius: 9px; display: flex; align-items: center; justify-content: center; padding: 0 4px; border: 2px solid var(--bg-card); }
.contact-info { flex: 1; overflow: hidden; }
.contact-name { font-weight: 600; font-size: 14px; }
.contact-last { font-size: 12px; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }

.chat-main { display: flex; flex-direction: column; padding: 0; background: var(--bg-card); border-radius: 0 var(--radius-md) var(--radius-md) 0; }
.chat-header { padding: 16px 20px; border-bottom: 1px solid var(--border); font-weight: 700; font-size: 16px; display: flex; align-items: center; gap: 8px; }
.header-online { font-size: 12px; font-weight: 500; color: #22c55e; background: #f0fdf4; padding: 2px 8px; border-radius: 10px; }
.header-offline { font-size: 12px; font-weight: 500; color: var(--text-muted); background: var(--bg-hover); padding: 2px 8px; border-radius: 10px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.message-item { display: flex; gap: 10px; &.self { justify-content: flex-end; } }
.sender-name { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.msg-wrap { min-width: 0; width: fit-content; max-width: 60%; }
.msg-wrap.self-wrap { display: flex; flex-direction: column; align-items: flex-end; }
.msg-bubble { background: #f1f5f9; padding: 10px 16px; border-radius: 16px 16px 16px 4px; word-break: break-all; font-size: 14px; line-height: 1.6; }
.self-bubble { background: var(--primary); color: #fff; border-radius: 16px 16px 4px 16px; }
.msg-meta { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.msg-time { font-size: 11px; color: var(--text-muted); padding: 0 4px; }
.msg-read { font-size: 11px; color: var(--primary); }
.msg-unread { font-size: 11px; color: var(--text-muted); }
.typing-hint { font-size: 12px; color: var(--text-muted); padding: 4px 8px; font-style: italic; }
.chat-input { display: flex; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--border); }
.chat-empty { display: flex; align-items: center; justify-content: center; height: 100%; }
</style>

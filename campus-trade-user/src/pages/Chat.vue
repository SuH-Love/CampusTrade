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
            <el-tag v-if="connected" type="success" size="small" effect="dark">在线</el-tag>
            <el-tag v-else type="info" size="small" effect="dark">离线</el-tag>
          </div>
          <div class="chat-messages" ref="messagesRef">
            <div v-for="msg in messages" :key="msg.id || msg._tempId" class="message-item" :class="{ self: msg.senderId === myId }">
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
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getRecentContacts, getHistory, getUnreadCount } from '@/api/chat'
import { useChatWs } from '@/composables/useChatWs'
import type { ChatMessageVO } from '@/api/chat'
import type { ContactVO } from '@/types'

interface DisplayMessage extends ChatMessageVO { _tempId?: string }

const route = useRoute()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id || getMyId())

const {
  connected, sendChat, sendTyping, sendStopTyping,
  sendRead, onChatMessage, chatUnreadMap, getMyId
} = useChatWs()

interface ContactItem { userId: number; name: string; avatar: string; lastMessage: string; unread: number }

const contacts = ref<ContactItem[]>([])
const currentTarget = ref<number | null>(null)
const currentContactName = ref('')
const messages = ref<DisplayMessage[]>([])
const inputText = ref('')
const sending = ref(false)
const messagesRef = ref<HTMLElement>()
const typingHint = ref('')
let lastTypingSent = false
let tempIdCounter = 0


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
        unread: chatUnreadMap.value.get(partnerId) || 0
      }
    })
    const unreadPromises = contacts.value.map(async (c) => {
      try {
        const count = await getUnreadCount(c.userId)
        c.unread = typeof count === 'number' ? count : 0
        if (c.unread > 0) {
          const m = new Map(chatUnreadMap.value)
          m.set(c.userId, c.unread)
          m.delete(-1)
          chatUnreadMap.value = m
        }
      } catch { /* ignore */ }
    })
    await Promise.all(unreadPromises)
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
    const res = await getHistory(currentTarget.value, 1, 200)
    const list = res.list || res || []
    messages.value = (Array.isArray(list) ? list : []) as DisplayMessage[]
    await nextTick()
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

const handleSend = async () => {
  if (!inputText.value.trim() || !currentTarget.value) return
  const content = inputText.value.trim()
  const targetId = currentTarget.value
  const id = myId.value
  sending.value = true

  const tempMsg: DisplayMessage = {
    id: 0,
    _tempId: `temp_${++tempIdCounter}`,
    senderId: id || 0,
    receiverId: targetId,
    content,
    messageType: 1,
    isRead: 0,
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19) as any,
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value,
    receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  inputText.value = ''
  nextTick(scrollToBottom)

  try {
    const sent = sendChat(targetId, content)
    if (!sent) {
      const { sendMessage } = await import('@/api/chat')
      await sendMessage({ receiverId: targetId, content })
      const idx = messages.value.findIndex(m => m._tempId === tempMsg._tempId)
      if (idx > -1) {
        await loadMessages()
      }
    }
    sendStopTyping(targetId)
    lastTypingSent = false
    updateContactLastMessage(targetId, content)
  } finally {
    sending.value = false
  }
}

const updateContactLastMessage = (partnerId: number, content: string) => {
  const idx = contacts.value.findIndex(c => c.userId === partnerId)
  if (idx > -1) {
    contacts.value[idx].lastMessage = content
    if (idx > 0) {
      const contact = contacts.value.splice(idx, 1)[0]
      contacts.value.unshift(contact)
    }
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

const formatTime = (t: string | number | null | undefined) => {
  if (!t) return ''
  let d: Date
  if (typeof t === 'number') {
    d = new Date(t)
  } else if (typeof t === 'string' && t.includes('T')) {
    d = new Date(t)
  } else if (typeof t === 'string') {
    d = new Date(t.replace(' ', 'T'))
  } else {
    return ''
  }
  if (isNaN(d.getTime())) return ''
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return time
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${time}`
}

const removeWsHandler = onChatMessage((msg) => {
  if (msg.type === 'CHAT' && msg.data) {
    const chatMsg = msg.data as ChatMessageVO
    const currentMyId = myId.value
    const isFromMe = chatMsg.senderId === currentMyId
    const partnerId = isFromMe ? chatMsg.receiverId : chatMsg.senderId
    const partnerName = isFromMe ? (chatMsg.receiverName || '用户') : (chatMsg.senderName || '用户')
    const partnerAvatar = isFromMe ? (chatMsg.receiverAvatar || '') : (chatMsg.senderAvatar || '')

    if (currentTarget.value === partnerId) {
      const tempIdx = messages.value.findIndex(m =>
        m._tempId && m.senderId === chatMsg.senderId && m.receiverId === chatMsg.receiverId && m.content === chatMsg.content
      )
      if (tempIdx > -1) {
        messages.value[tempIdx] = { ...chatMsg } as DisplayMessage
      } else {
        const exists = messages.value.some(m => m.id !== 0 && m.id === chatMsg.id)
        if (!exists) {
          messages.value.push(chatMsg as DisplayMessage)
        }
      }
      nextTick(scrollToBottom)

      if (!isFromMe) {
        sendRead(partnerId)
      }
      updateContactLastMessage(partnerId, chatMsg.content)
      const c = contacts.value.find(c => c.userId === partnerId)
      if (c) c.unread = 0
    } else {
      const isFromOther = !isFromMe
      const idx = contacts.value.findIndex(c => c.userId === partnerId)
      if (idx > -1) {
        contacts.value[idx].lastMessage = chatMsg.content
        if (isFromOther) contacts.value[idx].unread = (contacts.value[idx].unread || 0) + 1
        if (idx > 0) {
          const contact = contacts.value.splice(idx, 1)[0]
          contacts.value.unshift(contact)
        }
      } else if (isFromOther) {
        contacts.value.unshift({ userId: partnerId, name: partnerName, avatar: partnerAvatar, lastMessage: chatMsg.content, unread: 1 })
      }
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

watch(connected, (val) => {
  if (val && currentTarget.value && messages.value.length === 0) {
    loadMessages()
  }
})

onMounted(async () => {
  await loadContacts()

  const queryTarget = route.query.targetUserId ? Number(route.query.targetUserId) : null
  const queryName = route.query.name as string || '卖家'

  if (queryTarget) {
    const existing = contacts.value.find(c => c.userId === queryTarget)
    if (existing) {
      await selectContact(existing)
    } else {
      const newContact: ContactItem = { userId: queryTarget, name: queryName, avatar: '', lastMessage: '', unread: 0 }
      contacts.value.unshift(newContact)
      currentTarget.value = queryTarget
      currentContactName.value = queryName
      await loadMessages()
      sendRead(queryTarget)
    }
  } else {
    // 不自动恢复上次对话，用户需手动选择联系人
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

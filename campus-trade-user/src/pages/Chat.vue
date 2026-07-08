<template>
  <div class="chat-page">
    <el-container style="height: calc(100vh - 140px)">
      <el-aside width="280px" style="border-right: 1px solid #e4e7ed; overflow-y: auto">
        <div class="contact-list">
          <div
            v-for="contact in contacts"
            :key="contact.userId"
            class="contact-item"
            :class="{ active: currentTarget === contact.userId }"
            @click="selectContact(contact)"
          >
            <el-avatar :size="40" :src="contact.avatar" />
            <div class="contact-info">
              <div class="contact-name">{{ contact.name }}</div>
              <div class="contact-last">{{ contact.lastMessage }}</div>
            </div>
          </div>
        </div>
        <el-empty v-if="contacts.length === 0" description="暂无会话" :image-size="60" />
      </el-aside>
      <el-main style="display: flex; flex-direction: column; padding: 0">
        <template v-if="currentTarget">
          <div class="chat-header">{{ currentContactName }}</div>
          <div class="chat-messages" ref="messagesRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ self: msg.senderId === myId }"
            >
              <template v-if="msg.senderId === myId">
                <div class="message-content self-content">
                  <div class="message-text self-text">{{ msg.content }}</div>
                  <div class="message-time">{{ formatTime(msg.createTime) }}</div>
                </div>
              </template>
              <template v-else>
                <el-avatar :size="36" :src="msg.senderAvatar" />
                <div class="message-content">
                  <div class="sender-name">{{ msg.senderName }}</div>
                  <div class="message-text">{{ msg.content }}</div>
                  <div class="message-time">{{ formatTime(msg.createTime) }}</div>
                </div>
              </template>
            </div>
            <el-empty v-if="messages.length === 0" description="暂无消息，发送第一条消息吧" :image-size="60" />
          </div>
          <div class="chat-input">
            <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="handleSend" />
            <el-button type="primary" @click="handleSend" :disabled="!inputText.trim()">发送</el-button>
          </div>
        </template>
        <el-empty v-else description="选择联系人开始聊天" />
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

const route = useRoute()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id)

interface ContactItem {
  userId: number
  name: string
  avatar: string
  lastMessage: string
}

const contacts = ref<ContactItem[]>([])
const currentTarget = ref<number | null>(null)
const currentContactName = ref('')
const messages = ref<ChatMessageVO[]>([])
const inputText = ref('')
const messagesRef = ref<HTMLElement>()

const formatTime = (t: string) => {
  if (!t) return ''
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const loadContacts = async () => {
  try {
    const res = await getRecentContacts()
    const list = res.list || res || []
    contacts.value = list.map((c: any) => {
      const isMeSender = c.senderId === myId.value
      return {
        userId: isMeSender ? c.receiverId : c.senderId,
        name: isMeSender ? ('用户' + c.receiverId) : (c.senderName || '用户' + c.senderId),
        avatar: isMeSender ? '' : (c.senderAvatar || ''),
        lastMessage: c.content || ''
      }
    })
  } catch { contacts.value = [] }
}

const selectContact = async (contact: ContactItem) => {
  currentTarget.value = contact.userId
  currentContactName.value = contact.name
  await loadMessages()
  try { await markAsRead(contact.userId) } catch { /* ignore */ }
}

const loadMessages = async () => {
  if (!currentTarget.value) return
  try {
    const res = await getHistory(currentTarget.value)
    messages.value = res.list || res || []
    await nextTick()
    scrollToBottom()
  } catch { messages.value = [] }
}

const handleSend = async () => {
  if (!inputText.value.trim() || !currentTarget.value) return
  try {
    await sendMessage({ receiverId: currentTarget.value, content: inputText.value.trim() })
    inputText.value = ''
    await loadMessages()
  } catch { /* ignore */ }
}

const scrollToBottom = () => {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

onMounted(async () => {
  await loadContacts()
  if (route.query.targetUserId) {
    const targetId = Number(route.query.targetUserId)
    const targetName = route.query.name as string || '卖家'
    const existing = contacts.value.find(c => c.userId === targetId)
    if (existing) {
      selectContact(existing)
    } else {
      contacts.value.unshift({ userId: targetId, name: targetName, avatar: '', lastMessage: '' })
      currentTarget.value = targetId
      currentContactName.value = targetName
    }
  }
})
</script>

<style scoped lang="scss">
.chat-page { padding: 0; background: #fff; border-radius: 8px; overflow: hidden; }
.contact-list { padding: 8px 0; }
.contact-item {
  display: flex; align-items: center; gap: 10px; padding: 12px 16px; cursor: pointer;
  &:hover, &.active { background: #ecf5ff; }
}
.contact-info { flex: 1; overflow: hidden; }
.contact-name { font-weight: 500; }
.contact-last { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chat-header { padding: 16px; border-bottom: 1px solid #e4e7ed; font-weight: bold; font-size: 16px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; }
.message-item { display: flex; gap: 10px; margin-bottom: 16px; &.self { justify-content: flex-end; } }
.message-content { max-width: 60%; }
.self-content { text-align: right; }
.sender-name { font-size: 12px; color: #999; margin-bottom: 4px; }
.message-text { background: #f4f4f5; padding: 10px 14px; border-radius: 8px; word-break: break-all; display: inline-block; text-align: left; }
.self-text { background: #409eff; color: #fff; }
.message-time { font-size: 12px; color: #999; margin-top: 4px; }
.chat-input { display: flex; gap: 10px; padding: 16px; border-top: 1px solid #e4e7ed; }
</style>

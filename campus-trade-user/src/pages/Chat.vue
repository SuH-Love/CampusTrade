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
              <div class="contact-name" @click.stop="$router.push(`/profile/${contact.userId}`)" style="cursor: pointer">{{ contact.name }}</div>
              <div class="contact-last">{{ contact.lastMessage }}</div>
            </div>
            <el-button size="small" type="danger" plain round @click.stop="handleBlock(contact)" title="屏蔽" style="flex-shrink: 0; font-size: 12px">
              <el-icon style="margin-right: 2px"><Close /></el-icon>屏蔽
            </el-button>
          </div>
        </div>
        <el-empty v-if="contacts.length === 0" description="暂无会话" :image-size="60" />
      </el-aside>
      <el-main class="chat-main">
        <template v-if="currentTarget">
          <div class="chat-header">
            <span>{{ currentContactName }}</span>
            <span v-if="typingHint" class="header-typing">{{ typingHint }}</span>
            <span v-else-if="isOnline(currentTarget!)" class="header-online">在线</span>
            <span v-else class="header-offline">离线</span>
            <div class="header-search">
              <el-button size="small" circle @click="toggleSearch"><el-icon><Search /></el-icon></el-button>
            </div>
          </div>
          <div v-if="showSearch" class="search-bar">
            <el-input v-model="searchKeyword" placeholder="搜索消息..." size="small" clearable @input="handleSearchInput" ref="searchInputRef">
              <template #append>
                <div class="search-nav">
                  <span class="search-count" v-if="searchMatches.length > 0">{{ searchCurrentIdx + 1 }}/{{ searchMatches.length }}</span>
                  <span class="search-count" v-else-if="searchKeyword">0/0</span>
                  <el-button size="small" text :disabled="searchMatches.length === 0" @click="searchPrev"><el-icon><ArrowUp /></el-icon></el-button>
                  <el-button size="small" text :disabled="searchMatches.length === 0" @click="searchNext"><el-icon><ArrowDown /></el-icon></el-button>
                </div>
              </template>
            </el-input>
          </div>
          <div class="chat-messages" ref="messagesRef">
            <div v-for="(msg, idx) in messages" :key="msg.id || msg._tempId" class="message-item" :class="{ self: msg.senderId === myId }" :data-msg-idx="idx">
              <template v-if="msg.senderId === myId">
                <div class="msg-wrap self-wrap">
                  <div v-if="msg.messageType === 2" class="msg-bubble self-bubble img-bubble"><el-image :src="msg.content" fit="cover" class="chat-img" :preview-src-list="[msg.content]" hide-on-click-modal /></div>
                  <div v-else-if="msg.messageType === 3 && parseMsgType(msg.content) === 'order'" class="msg-bubble self-bubble order-bubble" @click="openOrderLink(msg.content)">
                    <el-icon><List /></el-icon>
                    <div class="order-card-info">
                      <div class="order-card-title">{{ parseOrderNo(msg.content) }}</div>
                      <div class="order-card-sub">¥{{ parseOrderAmount(msg.content) }} · {{ parseOrderStatus(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 3" class="msg-bubble self-bubble goods-bubble" @click="openGoodsLink(msg.content)">
                    <el-icon><Goods /></el-icon>
                    <div class="goods-card-info">
                      <div class="goods-card-title">{{ parseGoodsText(msg.content) }}</div>
                      <div class="goods-card-price" v-if="parseGoodsPrice(msg.content)">¥{{ parseGoodsPrice(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 4" class="msg-bubble self-bubble recall-bubble"><el-icon style="margin-right: 4px"><RefreshLeft /></el-icon>该消息已撤回</div>
                  <div v-else class="msg-bubble self-bubble" v-html="highlightText(msg.content)"></div>
                  <el-button v-if="msg._recallable" size="small" text type="info" @click="handleRecall(msg)" class="recall-btn"><el-icon style="margin-right: 2px"><RefreshLeft /></el-icon>撤回</el-button>
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
                  <div v-if="msg.messageType === 2" class="msg-bubble img-bubble"><el-image :src="msg.content" fit="cover" class="chat-img" :preview-src-list="[msg.content]" hide-on-click-modal /></div>
                  <div v-else-if="msg.messageType === 3 && parseMsgType(msg.content) === 'order'" class="msg-bubble order-bubble" @click="openOrderLink(msg.content)">
                    <el-icon><List /></el-icon>
                    <div class="order-card-info">
                      <div class="order-card-title">{{ parseOrderNo(msg.content) }}</div>
                      <div class="order-card-sub">¥{{ parseOrderAmount(msg.content) }} · {{ parseOrderStatus(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 3" class="msg-bubble goods-bubble" @click="openGoodsLink(msg.content)">
                    <el-icon><Goods /></el-icon>
                    <div class="goods-card-info">
                      <div class="goods-card-title">{{ parseGoodsText(msg.content) }}</div>
                      <div class="goods-card-price" v-if="parseGoodsPrice(msg.content)">¥{{ parseGoodsPrice(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 4" class="msg-bubble recall-bubble"><el-icon style="margin-right: 4px"><RefreshLeft /></el-icon>该消息已撤回</div>
                  <div v-else class="msg-bubble" v-html="highlightText(msg.content)"></div>
                  <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
                </div>
              </template>
            </div>

            <el-empty v-if="messages.length === 0" description="暂无消息，发送第一条消息吧" :image-size="60" />
          </div>
          <div class="chat-input-area">
            <div v-if="showPlusPanel" class="plus-panel">
              <div class="plus-item" @click="triggerImageUpload">
                <div class="plus-icon"><el-icon><Picture /></el-icon></div>
                <span>图片</span>
              </div>
              <div class="plus-item" @click="showOrderPicker = true">
                <div class="plus-icon"><el-icon><List /></el-icon></div>
                <span>订单</span>
              </div>
              <div class="plus-item" @click="showGoodsPicker = true">
                <div class="plus-icon"><el-icon><Goods /></el-icon></div>
                <span>商品</span>
              </div>
            </div>
            <div class="chat-input">
              <el-button size="large" round @click="showPlusPanel = !showPlusPanel" :type="showPlusPanel ? 'primary' : 'default'"><el-icon><Plus /></el-icon></el-button>
              <el-upload ref="uploadRef" action="" :auto-upload="false" :show-file-list="false" accept="image/jpeg,image/png,image/gif,image/webp" :on-change="handleImageSelect" style="display: none" />
              <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="handleSend" @input="handleTyping" size="large" @focus="showPlusPanel = false" />
              <el-button type="primary" size="large" @click="handleSend" :disabled="!inputText.trim()" :loading="sending" round>发送</el-button>
            </div>
          </div>
        </template>
        <div v-else class="chat-empty"><el-empty description="选择联系人开始聊天" /></div>
      </el-main>
    </el-container>

    <el-dialog v-model="showGoodsPicker" title="选择商品" width="480px" destroy-on-close @open="loadPickerGoods">
      <div v-loading="goodsPickerLoading" class="picker-list">
        <div v-for="g in pickerGoodsList" :key="g.id" class="picker-item" @click="confirmSendGoods(g)">
          <el-image :src="g.coverImage || '/default-cover.svg'" style="width: 48px; height: 48px; border-radius: 6px; flex-shrink: 0" fit="cover" />
          <div class="picker-item-info">
            <div class="picker-item-title">{{ g.title }}</div>
            <div class="picker-item-price">¥{{ g.price }}</div>
          </div>
        </div>
        <el-empty v-if="!goodsPickerLoading && pickerGoodsList.length === 0" description="该商家暂无在售商品" :image-size="50" />
      </div>
    </el-dialog>

    <el-dialog v-model="showOrderPicker" title="选择订单" width="480px" destroy-on-close @open="loadPickerOrders">
      <div v-loading="orderPickerLoading" class="picker-list">
        <div v-for="o in pickerOrderList" :key="o.id" class="picker-item" @click="confirmSendOrder(o)">
          <div class="picker-item-info">
            <div class="picker-item-title">{{ o.orderNo }}</div>
            <div class="picker-item-sub">¥{{ o.totalAmount }} · {{ orderStatusLabel(o.status) }}</div>
          </div>
        </div>
        <el-empty v-if="!orderPickerLoading && pickerOrderList.length === 0" description="暂无与该商家的订单" :image-size="50" />
      </div>
    </el-dialog>
  </div>

  <Teleport to="body">
    <div v-if="contextMenu.visible" class="context-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
      <div v-if="contextMenu.canRecall" class="context-menu-item" @click="handleRecall(contextMenu.msg!)">
        <el-icon style="margin-right: 6px"><RefreshLeft /></el-icon>撤回消息
      </div>
      <div v-if="contextMenu.canCopy" class="context-menu-item" @click="handleCopyMsg(contextMenu.msg!)">
        <el-icon style="margin-right: 6px"><CopyDocument /></el-icon>复制内容
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getRecentContacts, getHistory, getUnreadCount, recallMessage } from '@/api/chat'
import { blockUser } from '@/api/blacklist'
import { uploadImage } from '@/api/file'
import { getGoodsList, type GoodsVO } from '@/api/goods'
import { getBuyerOrders, type OrderVO } from '@/api/order'
import { getUserPublicInfo } from '@/api/user'
import { useChatWs } from '@/composables/useChatWs'
import type { ChatMessageVO } from '@/api/chat'
import type { ContactVO } from '@/types'

import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowUp, ArrowDown } from '@element-plus/icons-vue'

interface DisplayMessage extends ChatMessageVO { _tempId?: string; _sentAt?: number; _recallable?: boolean }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id || getMyId())

const scheduleRecallExpire = (key: string) => {
  setTimeout(() => {
    const idx = messages.value.findIndex(m => (m._tempId && m._tempId === key) || (m.id && m.id !== 0 && String(m.id) === key))
    if (idx > -1 && messages.value[idx]._recallable) {
      messages.value[idx]._recallable = false
    }
  }, 2 * 60 * 1000)
}

const markServerRecallable = () => {
  const t = Date.now()
  const uid = myId.value
  for (let i = 0; i < messages.value.length; i++) {
    const m = messages.value[i]
    if (m.senderId !== uid || m.messageType === 4) { m._recallable = false; continue }
    if (m._sentAt) continue
    const sentAt = m.createTime ? new Date(m.createTime.replace(' ', 'T')).getTime() : 0
    m._recallable = sentAt > 0 && (t - sentAt) < 2 * 60 * 1000
  }
}

const {
  connected, onlineUsers, sendChat, sendTyping, sendStopTyping,
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
const showPlusPanel = ref(false)
const showGoodsPicker = ref(false)
const showOrderPicker = ref(false)
const goodsPickerLoading = ref(false)
const orderPickerLoading = ref(false)
const pickerGoodsList = ref<GoodsVO[]>([])
const pickerOrderList = ref<OrderVO[]>([])
const uploadRef = ref<{ $el: HTMLElement }>()
let lastTypingSent = false
let tempIdCounter = 0
let recallTimer: ReturnType<typeof setInterval> | null = null

const contextMenu = ref<{ visible: boolean; x: number; y: number; msg: DisplayMessage | null; canRecall: boolean; canCopy: boolean }>({
  visible: false, x: 0, y: 0, msg: null, canRecall: false, canCopy: false
})

const showSearch = ref(false)
const searchKeyword = ref('')
const searchMatches = ref<number[]>([])
const searchCurrentIdx = ref(-1)
const searchInputRef = ref<{ focus: () => void }>()

const toggleSearch = () => {
  showSearch.value = !showSearch.value
  if (!showSearch.value) {
    searchKeyword.value = ''
    searchMatches.value = []
    searchCurrentIdx.value = -1
  } else {
    nextTick(() => { searchInputRef.value?.focus() })
  }
}

const handleSearchInput = () => {
  searchMatches.value = []
  searchCurrentIdx.value = -1
  if (!searchKeyword.value.trim()) return
  const kw = searchKeyword.value.trim().toLowerCase()
  messages.value.forEach((msg, idx) => {
    if (msg.messageType === 1 && msg.content.toLowerCase().includes(kw)) {
      searchMatches.value.push(idx)
    }
  })
  if (searchMatches.value.length > 0) {
    searchCurrentIdx.value = 0
    scrollToMsg(searchMatches.value[0])
  }
}

const searchPrev = () => {
  if (searchMatches.value.length === 0) return
  searchCurrentIdx.value = (searchCurrentIdx.value - 1 + searchMatches.value.length) % searchMatches.value.length
  scrollToMsg(searchMatches.value[searchCurrentIdx.value])
}

const searchNext = () => {
  if (searchMatches.value.length === 0) return
  searchCurrentIdx.value = (searchCurrentIdx.value + 1) % searchMatches.value.length
  scrollToMsg(searchMatches.value[searchCurrentIdx.value])
}

const scrollToMsg = (msgIdx: number) => {
  const el = messagesRef.value?.querySelector(`[data-msg-idx="${msgIdx}"]`) as HTMLElement | null
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('search-highlight')
    setTimeout(() => { el.classList.remove('search-highlight') }, 1500)
  }
}

const highlightText = (text: string): string => {
  if (!searchKeyword.value.trim() || !text) return text
  const kw = searchKeyword.value.trim()
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark class="msg-highlight">$1</mark>')
}

const onDocContextMenu = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.chat-messages')) return
  const item = target.closest('.message-item') as HTMLElement | null
  if (!item) return
  const idx = Number(item.dataset.msgIdx)
  if (isNaN(idx) || idx < 0 || idx >= messages.value.length) return
  const msg = messages.value[idx]
  if (msg.messageType === 4) return
  e.preventDefault()
  const canRecall = !!msg._recallable
  const canCopy = msg.messageType === 1
  if (!canRecall && !canCopy) return
  const menuW = 160, menuH = (canRecall && canCopy) ? 88 : 44
  const x = e.clientX + menuW > window.innerWidth ? e.clientX - menuW : e.clientX
  const y = e.clientY + menuH > window.innerHeight ? e.clientY - menuH : e.clientY
  contextMenu.value = { visible: true, x, y, msg, canRecall, canCopy }
}

const handleCopyMsg = (msg: DisplayMessage) => {
  contextMenu.value.visible = false
  if (msg.content && navigator.clipboard) {
    navigator.clipboard.writeText(msg.content).catch(() => {})
  }
}

const closeContextMenu = () => { contextMenu.value.visible = false }

const formatLastMessage = (content: string, messageType?: number) => {
  if (messageType === 4) return '[消息已撤回]'
  if (messageType === 2) return '[图片]'
  if (messageType === 3) {
    try {
      const d = JSON.parse(content)
      if (d.type === 'order') return `[订单] ${d.orderNo || ''}`
      return `[商品] ${d.title || ''}`
    } catch { return '[卡片]' }
  }
  return content || ''
}

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
        lastMessage: formatLastMessage(c.content, c.messageType),
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
      } catch (e) { console.error(e) }
    })
    await Promise.all(unreadPromises)
  } catch { contacts.value = [] }
}

const selectContact = async (contact: ContactItem) => {
  contact.unread = 0
  router.replace(`/chat/${contact.userId}`)
}

const loadMessages = async () => {
  if (!currentTarget.value) return
  try {
    const res = await getHistory(currentTarget.value, 1, 200)
    const list = res.list || res || []
    messages.value = (Array.isArray(list) ? list : []) as DisplayMessage[]
    markServerRecallable()
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
    _sentAt: Date.now(),
    _recallable: true,
    senderId: id || 0,
    receiverId: targetId,
    content,
    messageType: 1,
    isRead: 0,

    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value,
    receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  scheduleRecallExpire(tempMsg._tempId!)

  inputText.value = ''
  nextTick(scrollToBottom)

  try {
    const sent = sendChat(targetId, content)
    if (!sent) {
      const { sendMessage } = await import('@/api/chat')
      await sendMessage({ receiverId: targetId, content, messageType: 1 })
      const idx = messages.value.findIndex(m => m._tempId === tempMsg._tempId)
      if (idx > -1) {
        await loadMessages()
      }
    }
    sendStopTyping(targetId)
    lastTypingSent = false
    updateContactLastMessage(targetId, content, 1)
  } finally {
    sending.value = false
  }
}

const updateContactLastMessage = (partnerId: number, content: string, messageType?: number) => {
  const idx = contacts.value.findIndex(c => c.userId === partnerId)
  if (idx > -1) {
    contacts.value[idx].lastMessage = formatLastMessage(content, messageType)
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

const triggerImageUpload = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/jpeg,image/png,image/gif,image/webp'
  input.onchange = async (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (file) {
      const fakeEvent = { raw: file }
      await handleImageSelect(fakeEvent)
    }
  }
  input.click()
  showPlusPanel.value = false
}

const handleImageSelect = async (uploadFile: { raw?: File }) => {
  if (!uploadFile.raw || !currentTarget.value) return
  const file = uploadFile.raw
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) { ElMessage.error('仅支持 jpg/png/gif/webp 格式'); return }
  if (!isLt10M) { ElMessage.error('图片大小不能超过 10MB'); return }
  try {
    const url = await uploadImage(file)
    const targetId = currentTarget.value
    const tempMsg: DisplayMessage = {
      id: 0, _tempId: `temp_${++tempIdCounter}`, _sentAt: Date.now(), _recallable: true,
      senderId: myId.value || 0, receiverId: targetId,
      content: url, messageType: 2, isRead: 0,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
      senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
      senderAvatar: userStore.userInfo?.avatar || '',
      receiverName: currentContactName.value, receiverAvatar: ''
    }
    messages.value.push(tempMsg)
    scheduleRecallExpire(tempMsg._tempId!)
    nextTick(scrollToBottom)
    sendChat(targetId, url, 2)
    updateContactLastMessage(targetId, url, 2)
  } catch { ElMessage.error('图片发送失败') }
}

const parseGoodsText = (content: string) => {
  try {
    const data = JSON.parse(content)
    return data.title || '查看商品'
  } catch { return '查看商品' }
}

const parseGoodsPrice = (content: string) => {
  try {
    const data = JSON.parse(content)
    return data.price || ''
  } catch { return '' }
}

const openGoodsLink = (content: string) => {
  try {
    const data = JSON.parse(content)
    if (data.goodsId) router.push(`/goods/${data.goodsId}`)
  } catch (e) { console.error(e) }
}

const parseMsgType = (content: string): string => {
  try { const d = JSON.parse(content); return d.type === 'order' ? 'order' : 'goods' } catch { return 'goods' }
}

const parseOrderNo = (content: string) => {
  try { const d = JSON.parse(content); return d.orderNo || '查看订单' } catch { return '查看订单' }
}

const parseOrderAmount = (content: string) => {
  try { const d = JSON.parse(content); return d.amount || '' } catch { return '' }
}

const parseOrderStatus = (content: string) => {
  try {
    const d = JSON.parse(content)
    return orderStatusLabel(d.status)
  } catch { return '' }
}

const orderStatusLabel = (status: string) => {
  const map: Record<string, string> = { PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货', PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中' }
  return map[status] || status || ''
}

const openOrderLink = (content: string) => {
  try {
    const data = JSON.parse(content)
    if (data.orderId) router.push(`/order/${data.orderId}`)
  } catch (e) { console.error(e) }
}

const loadPickerOrders = async () => {
  if (!currentTarget.value) return
  orderPickerLoading.value = true
  try {
    const res = await getBuyerOrders({ pageNum: 1, pageSize: 50 })
    const allOrders = (res.list || []) as OrderVO[]
    pickerOrderList.value = allOrders.filter(o => o.sellerId === currentTarget.value)
  } catch { pickerOrderList.value = [] }
  finally { orderPickerLoading.value = false }
}

const confirmSendOrder = (order: OrderVO) => {
  if (!currentTarget.value) return
  showOrderPicker.value = false
  const content = JSON.stringify({ type: 'order', orderId: order.id, orderNo: order.orderNo, amount: order.totalAmount, status: order.status })
  sendChat(currentTarget.value, content, 3)
  const tempMsg: DisplayMessage = {
    id: 0, _tempId: `temp_${++tempIdCounter}`, _sentAt: Date.now(), _recallable: true,
    senderId: myId.value || 0, receiverId: currentTarget.value,
    content, messageType: 3, isRead: 0,
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value, receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  scheduleRecallExpire(tempMsg._tempId!)
  nextTick(scrollToBottom)
  updateContactLastMessage(currentTarget.value, content, 3)
}

const loadPickerGoods = async () => {
  if (!currentTarget.value) return
  goodsPickerLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: 1, pageSize: 50, userId: currentTarget.value, status: 'ONLINE' })
    pickerGoodsList.value = (res.list || []) as GoodsVO[]
  } catch { pickerGoodsList.value = [] }
  finally { goodsPickerLoading.value = false }
}

const confirmSendGoods = (g: GoodsVO) => {
  if (!currentTarget.value) return
  showGoodsPicker.value = false
  const content = JSON.stringify({ goodsId: g.id, title: g.title, price: g.price, coverImage: g.coverImage })
  sendChat(currentTarget.value, content, 3)
  const tempMsg: DisplayMessage = {
    id: 0, _tempId: `temp_${++tempIdCounter}`, _sentAt: Date.now(), _recallable: true,
    senderId: myId.value || 0, receiverId: currentTarget.value,
    content, messageType: 3, isRead: 0,
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value, receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  scheduleRecallExpire(tempMsg._tempId!)
  nextTick(scrollToBottom)
  updateContactLastMessage(currentTarget.value, content, 3)
}


const scrollToBottom = () => {
  if (!messagesRef.value) return
  messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  setTimeout(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }, 100)
  setTimeout(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight }, 300)
}


const handleRecall = async (msg: DisplayMessage) => {
  if (!msg.id && !msg._tempId) return
  if (!msg.id) {
    const found = messages.value.find(m => m._tempId === msg._tempId && m.id && m.id !== 0)
    if (found) msg = found
    else { ElMessage.warning('消息发送中，请稍后撤回'); return }
  }
  try {
    await recallMessage(msg.id)
    const idx = messages.value.findIndex(m => m.id === msg.id)
    if (idx > -1) {
      messages.value[idx].content = '该消息已撤回'
      messages.value[idx].messageType = 4
      messages.value[idx]._recallable = false
    }
    updateContactLastMessage(currentTarget.value || msg.receiverId, '该消息已撤回', 4)
    ElMessage.success('已撤回')
  } catch (e) { console.error(e) }
}

const handleBlock = async (contact: ContactItem) => {
  try {
    await ElMessageBox.confirm(`确定屏蔽「${contact.name}」？屏蔽后对方无法给你发消息`, '屏蔽用户', { type: 'warning' })
    await blockUser(contact.userId)
    ElMessage.success('已屏蔽')
    if (currentTarget.value === contact.userId) currentTarget.value = null
    loadContacts()
  } catch (e) { console.error(e) }
}

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
  const today = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  const isToday = d.toDateString() === today.toDateString()
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
      if (chatMsg.messageType === 4) {
        const recallTarget = messages.value.find(m => m.id !== 0 && m.id === chatMsg.id)
        if (recallTarget) {
          recallTarget.content = '该消息已撤回'
          recallTarget.messageType = 4
          recallTarget._recallable = false
        }
        updateContactLastMessage(partnerId, '该消息已撤回', 4)
        const c = contacts.value.find(c => c.userId === partnerId)
        if (c && !isFromMe) c.unread = Math.max(0, (c.unread || 0) - 1)
      } else {
        const tempIdx = messages.value.findIndex(m =>
          m._tempId && m.senderId === chatMsg.senderId && m.receiverId === chatMsg.receiverId && m.content === chatMsg.content
        )
        if (tempIdx > -1) {
          const oldRecallable = messages.value[tempIdx]._recallable
          const oldSentAt = messages.value[tempIdx]._sentAt
          const replaced = { ...chatMsg, _recallable: oldRecallable, _sentAt: oldSentAt } as DisplayMessage
          messages.value[tempIdx] = replaced
          if (oldRecallable) scheduleRecallExpire(String(chatMsg.id))
        } else {
          const exists = messages.value.some(m => m.id !== 0 && m.id === chatMsg.id)
          if (!exists) {
            const newMsg = chatMsg as DisplayMessage
            messages.value.push(newMsg)
            markServerRecallable()
          }
        }
        nextTick(scrollToBottom)
        if (!isFromMe) {
          sendRead(partnerId)
        }
        updateContactLastMessage(partnerId, chatMsg.content, chatMsg.messageType)
        const c = contacts.value.find(c => c.userId === partnerId)
        if (c) c.unread = 0
      }
    } else {
      const isFromOther = !isFromMe
      const isRecall = chatMsg.messageType === 4
      const formattedLast = formatLastMessage(chatMsg.content, chatMsg.messageType)
      const idx = contacts.value.findIndex(c => c.userId === partnerId)
      if (idx > -1) {
        contacts.value[idx].lastMessage = formattedLast
        if (isFromOther && !isRecall) contacts.value[idx].unread = (contacts.value[idx].unread || 0) + 1
        if (idx > 0) {
          const contact = contacts.value.splice(idx, 1)[0]
          contacts.value.unshift(contact)
        }
      } else if (isFromOther && !isRecall) {
        contacts.value.unshift({ userId: partnerId, name: partnerName, avatar: partnerAvatar, lastMessage: formattedLast, unread: 1 })
      } else if (isFromOther && isRecall) {
        contacts.value.unshift({ userId: partnerId, name: partnerName, avatar: partnerAvatar, lastMessage: formattedLast, unread: 0 })
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

watch(() => route.params.userId, async (newTarget) => {
  if (!newTarget) return
  const userId = Number(newTarget)
  if (!Number.isFinite(userId) || !Number.isInteger(userId) || userId <= 0 || userId === currentTarget.value) return
  try {
    const userInfo = await getUserPublicInfo(userId)
    const name = userInfo.nickname || userInfo.username || `用户${userId}`
    currentTarget.value = userId
    currentContactName.value = name
    const existing = contacts.value.find(c => c.userId === userId)
    if (existing) {
      existing.unread = 0
    } else {
      contacts.value.unshift({ userId, name, avatar: userInfo.avatar || '', lastMessage: '', unread: 0 })
    }
    await loadMessages()
    sendRead(userId)
  } catch {
    ElMessage.error('用户不存在')
    router.replace('/chat')
  }
})

const switchToContact = async (userId: number): Promise<boolean> => {
  try {
    const userInfo = await getUserPublicInfo(userId)
    const name = userInfo.nickname || userInfo.username || `用户${userId}`
    currentTarget.value = userId
    currentContactName.value = name
    const existing = contacts.value.find(c => c.userId === userId)
    if (existing) {
      existing.unread = 0
    } else {
      contacts.value.unshift({ userId, name, avatar: userInfo.avatar || '', lastMessage: '', unread: 0 })
    }
    await nextTick()
    await loadMessages()
    sendRead(userId)
    return true
  } catch {
    return false
  }
}

onMounted(async () => {
  document.addEventListener('click', closeContextMenu)
  document.addEventListener('contextmenu', onDocContextMenu, true)
  recallTimer = setInterval(markServerRecallable, 15000)

  const rawU = route.params.userId || route.query.u || route.query.targetUserId
  const queryTarget = rawU ? Number(rawU) : null
  const queryConsult = route.query.consult as string || ''

  if (queryTarget !== null && (!Number.isFinite(queryTarget) || !Number.isInteger(queryTarget) || queryTarget <= 0)) {
    router.replace('/chat')
    return
  }

  await loadContacts()

  if (queryTarget) {
    const ok = await switchToContact(queryTarget)
    if (!ok) {
      ElMessage.error('用户不存在')
      router.replace('/chat')
      return
    }

    if (queryConsult) {
      try {
        JSON.parse(queryConsult)
        const content = queryConsult
        sendChat(queryTarget, content, 3)
        const tempMsg: DisplayMessage = {
          id: 0, _tempId: `temp_${++tempIdCounter}`, _sentAt: Date.now(), _recallable: true,
          senderId: myId.value || 0, receiverId: currentTarget.value!,
          content, messageType: 3, isRead: 0,
          createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
          senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
          senderAvatar: userStore.userInfo?.avatar || '',
          receiverName: currentContactName.value, receiverAvatar: ''
        }
        messages.value.push(tempMsg)
        scheduleRecallExpire(tempMsg._tempId!)
        await nextTick()
        scrollToBottom()
        updateContactLastMessage(queryTarget, content, 3)
      } catch (e) { console.error(e) }
    }
    if (String(route.params.userId) !== String(queryTarget)) {
      router.replace(`/chat/${queryTarget}`)
    }
  }
})

onUnmounted(() => {
  removeWsHandler()
  document.removeEventListener('click', closeContextMenu)
  document.removeEventListener('contextmenu', onDocContextMenu, true)
  if (recallTimer) clearInterval(recallTimer)
})

</script>

<style scoped lang="scss">
.chat-page { padding: 20px 24px; }
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
.contact-name { font-weight: 600; font-size: 14px; }
.contact-last { font-size: 12px; color: var(--text-muted); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 2px; }

.chat-main { display: flex; flex-direction: column; padding: 0; background: var(--bg-card); border-radius: 0 var(--radius-lg) var(--radius-lg) 0; }
.chat-header { padding: 16px 20px; border-bottom: 1px solid var(--border); font-weight: 700; font-size: 16px; display: flex; align-items: center; gap: 8px; }
.header-search { margin-left: auto; }
.search-bar { padding: 8px 20px; border-bottom: 1px solid var(--border); background: var(--bg-hover); display: flex; align-items: center; }
.search-nav { display: flex; align-items: center; gap: 2px; }
.search-count { font-size: 12px; color: var(--text-muted); margin-right: 4px; white-space: nowrap; }
.msg-highlight { background: #fbbf24; color: #1e293b; border-radius: 2px; padding: 0 1px; }
.search-highlight { animation: search-flash 1.5s ease; }
@keyframes search-flash { 0%, 100% { background: transparent; } 30% { background: rgba(251, 191, 36, 0.25); } }
.header-online { font-size: 12px; font-weight: 500; color: #22c55e; background: #f0fdf4; padding: 2px 8px; border-radius: 10px; }
.header-offline { font-size: 12px; font-weight: 500; color: var(--text-muted); background: var(--bg-hover); padding: 2px 8px; border-radius: 10px; }
.header-typing { font-size: 12px; font-weight: 500; color: #6366f1; background: #eef2ff; padding: 2px 8px; border-radius: 10px; animation: typing-pulse 1.5s ease-in-out infinite; }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
.message-item { display: flex; gap: 10px; &.self { justify-content: flex-end; } }
.sender-name { font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.msg-wrap { min-width: 0; width: fit-content; max-width: 60%; }
.msg-wrap.self-wrap { display: flex; flex-direction: column; align-items: flex-end; }
.msg-bubble { background: #f1f5f9; padding: 10px 16px; border-radius: 16px 16px 16px 4px; word-break: break-all; font-size: 14px; line-height: 1.6; }
.self-bubble { background: var(--primary-gradient); color: #fff; border-radius: 16px 16px 4px 16px; }
.recall-bubble { background: #f1f5f9 !important; color: #94a3b8 !important; font-size: 12px !important; font-style: italic; display: flex; align-items: center; border-radius: 12px !important; }
.img-bubble { padding: 4px !important; background: transparent !important; }
.chat-img { max-width: 200px; max-height: 200px; border-radius: 12px; cursor: pointer; }
.goods-bubble { cursor: pointer; display: flex; align-items: center; gap: 6px; &:hover { opacity: 0.85; } }
.order-bubble { cursor: pointer; display: flex; align-items: center; gap: 6px; &:hover { opacity: 0.85; } }
.order-card-info { flex: 1; min-width: 0; }
.order-card-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-card-sub { font-size: 12px; opacity: 0.85; margin-top: 2px; }
.msg-meta { display: flex; align-items: center; gap: 6px; margin-top: 4px; }
.msg-time { font-size: 11px; color: var(--text-muted); padding: 0 4px; }
.msg-read { font-size: 11px; color: var(--primary); }
.msg-unread { font-size: 11px; color: var(--text-muted); }

.chat-input-area { border-top: 1px solid var(--border); }
.plus-panel {
  display: flex; gap: 16px; padding: 16px 20px; background: #fafbfc; border-bottom: 1px solid var(--border);
}
.plus-item {
  display: flex; flex-direction: column; align-items: center; gap: 6px; cursor: pointer;
  transition: var(--transition-fast);
  &:hover { transform: translateY(-2px); }
}
.plus-icon {
  width: 48px; height: 48px; border-radius: 12px; background: var(--bg-card); border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center; font-size: 22px; color: var(--primary);
  transition: var(--transition-fast);
  &:hover { background: var(--primary-lighter); border-color: var(--primary-light); }
}
.plus-item span { font-size: 12px; color: var(--text-secondary); font-weight: 500; }
.chat-input { display: flex; gap: 10px; padding: 16px 20px; }
.chat-empty { display: flex; align-items: center; justify-content: center; height: 100%; }
.goods-card-info { flex: 1; min-width: 0; }
.goods-card-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-card-price { font-size: 13px; opacity: 0.85; margin-top: 2px; }
.picker-list { max-height: 400px; overflow-y: auto; }
.picker-item {
  display: flex; align-items: center; gap: 12px; padding: 12px; cursor: pointer; border-radius: 8px; transition: var(--transition-fast);
  &:hover { background: var(--bg-hover); }
}
.picker-item-info { flex: 1; min-width: 0; }
.picker-item-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.picker-item-price { font-size: 13px; color: #f56c6c; font-weight: 600; margin-top: 2px; }
.picker-item-sub { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

</style>

<style lang="scss">
.context-menu {
  position: fixed; z-index: 9999; background: #fff; border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15); border: 1px solid #e2e8f0;
  padding: 4px 0; min-width: 140px;
}
.context-menu-item {
  display: flex; align-items: center; padding: 10px 16px; cursor: pointer;
  font-size: 13px; color: #334155; transition: background 0.15s;
  &:hover { background: #eef2ff; color: #6366f1; }
}
@keyframes typing-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>

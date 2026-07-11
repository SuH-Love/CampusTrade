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
            <span v-if="isOnline(currentTarget!)" class="header-online">在线</span>
            <span v-else class="header-offline">离线</span>
          </div>
          <div class="chat-messages" ref="messagesRef">
            <div v-for="msg in messages" :key="msg.id || msg._tempId" class="message-item" :class="{ self: msg.senderId === myId }">
              <template v-if="msg.senderId === myId">
                <div class="msg-wrap self-wrap">
                  <div v-if="msg.messageType === 2" class="msg-bubble self-bubble img-bubble" @contextmenu.prevent="onMsgContext($event, msg)"><el-image :src="msg.content" fit="cover" class="chat-img" :preview-src-list="[msg.content]" hide-on-click-modal /></div>
                  <div v-else-if="msg.messageType === 3 && parseMsgType(msg.content) === 'order'" class="msg-bubble self-bubble order-bubble" @click="openOrderLink(msg.content)" @contextmenu.prevent="onMsgContext($event, msg)">
                    <el-icon><List /></el-icon>
                    <div class="order-card-info">
                      <div class="order-card-title">{{ parseOrderNo(msg.content) }}</div>
                      <div class="order-card-sub">¥{{ parseOrderAmount(msg.content) }} · {{ parseOrderStatus(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 3" class="msg-bubble self-bubble goods-bubble" @click="openGoodsLink(msg.content)" @contextmenu.prevent="onMsgContext($event, msg)">
                    <el-icon><Goods /></el-icon>
                    <div class="goods-card-info">
                      <div class="goods-card-title">{{ parseGoodsText(msg.content) }}</div>
                      <div class="goods-card-price" v-if="parseGoodsPrice(msg.content)">¥{{ parseGoodsPrice(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 4" class="msg-bubble self-bubble recall-bubble"><el-icon style="margin-right: 4px"><RefreshLeft /></el-icon>该消息已撤回</div>
                  <div v-else class="msg-bubble self-bubble" @contextmenu.prevent="onMsgContext($event, msg)">{{ msg.content }}</div>
                  <el-button v-if="isRecallable(msg)" size="small" text type="info" @click="handleRecall(msg)" class="recall-btn"><el-icon style="margin-right: 2px"><RefreshLeft /></el-icon>撤回</el-button>
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
                  <div v-if="msg.messageType === 2" class="msg-bubble img-bubble" @contextmenu.prevent="onMsgContext($event, msg)"><el-image :src="msg.content" fit="cover" class="chat-img" :preview-src-list="[msg.content]" hide-on-click-modal /></div>
                  <div v-else-if="msg.messageType === 3 && parseMsgType(msg.content) === 'order'" class="msg-bubble order-bubble" @click="openOrderLink(msg.content)" @contextmenu.prevent="onMsgContext($event, msg)">
                    <el-icon><List /></el-icon>
                    <div class="order-card-info">
                      <div class="order-card-title">{{ parseOrderNo(msg.content) }}</div>
                      <div class="order-card-sub">¥{{ parseOrderAmount(msg.content) }} · {{ parseOrderStatus(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 3" class="msg-bubble goods-bubble" @click="openGoodsLink(msg.content)" @contextmenu.prevent="onMsgContext($event, msg)">
                    <el-icon><Goods /></el-icon>
                    <div class="goods-card-info">
                      <div class="goods-card-title">{{ parseGoodsText(msg.content) }}</div>
                      <div class="goods-card-price" v-if="parseGoodsPrice(msg.content)">¥{{ parseGoodsPrice(msg.content) }}</div>
                    </div>
                  </div>
                  <div v-else-if="msg.messageType === 4" class="msg-bubble recall-bubble"><el-icon style="margin-right: 4px"><RefreshLeft /></el-icon>该消息已撤回</div>
                  <div v-else class="msg-bubble" @contextmenu.prevent="onMsgContext($event, msg)">{{ msg.content }}</div>
                  <div class="msg-time">{{ formatTime(msg.createTime) }}</div>
                </div>
              </template>
            </div>
            <div v-if="typingHint" class="typing-hint">{{ typingHint }}</div>
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
            <div class="picker-item-sub">¥{{ o.totalAmount }} · {{ o.status }}</div>
          </div>
        </div>
        <el-empty v-if="!orderPickerLoading && pickerOrderList.length === 0" description="暂无与该商家的订单" :image-size="50" />
      </div>
    </el-dialog>
  </div>

  <div v-if="contextMenu.visible" class="context-menu" :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
    <div v-if="contextMenu.canRecall" class="context-menu-item" @click="handleRecall(contextMenu.msg!)">
      <el-icon style="margin-right: 6px"><RefreshLeft /></el-icon>撤回消息
    </div>
    <div v-if="contextMenu.canCopy" class="context-menu-item" @click="handleCopyMsg(contextMenu.msg!)">
      <el-icon style="margin-right: 6px"><CopyDocument /></el-icon>复制内容
    </div>
  </div>
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
import { useChatWs } from '@/composables/useChatWs'
import type { ChatMessageVO } from '@/api/chat'
import type { ContactVO } from '@/types'

import { ElMessage, ElMessageBox } from 'element-plus'

interface DisplayMessage extends ChatMessageVO { _tempId?: string }

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.id || getMyId())

const recallableMap = ref<Record<string, boolean>>({})

const addRecallable = (id: number | undefined, tempId: string | undefined, createTime: string | undefined) => {
  const key = id && id !== 0 ? String(id) : tempId || ''
  if (!key) return
  if (!createTime) return
  const elapsed = Date.now() - new Date(createTime.replace(' ', 'T')).getTime()
  if (elapsed >= 2 * 60 * 1000) return
  recallableMap.value[key] = true
  const remain = 2 * 60 * 1000 - elapsed
  setTimeout(() => { delete recallableMap.value[key] }, remain)
}

const isRecallable = (msg: DisplayMessage) => {
  if (msg.senderId !== myId.value || msg.messageType === 4) return false
  const key = msg.id && msg.id !== 0 ? String(msg.id) : msg._tempId || ''
  return !!recallableMap.value[key]
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

const contextMenu = ref<{ visible: boolean; x: number; y: number; msg: DisplayMessage | null; canRecall: boolean; canCopy: boolean }>({
  visible: false, x: 0, y: 0, msg: null, canRecall: false, canCopy: false
})

const onMsgContext = (e: MouseEvent, msg: DisplayMessage) => {
  if (msg.messageType === 4) return
  const canRecall = msg.senderId === myId.value && (msg.id && msg.id !== 0 || !!msg._tempId) &&
    (Date.now() - new Date(msg.createTime.replace(' ', 'T')).getTime()) < 2 * 60 * 1000
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

document.addEventListener('click', () => { contextMenu.value.visible = false })

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
    messages.value.forEach(m => addRecallable(m.id, m._tempId, m.createTime))
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

    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value,
    receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  addRecallable(tempMsg.id, tempMsg._tempId, tempMsg.createTime)
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
      id: 0, _tempId: `temp_${++tempIdCounter}`,
      senderId: myId.value || 0, receiverId: targetId,
      content: url, messageType: 2, isRead: 0,
      createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
      senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
      senderAvatar: userStore.userInfo?.avatar || '',
      receiverName: currentContactName.value, receiverAvatar: ''
    }
    messages.value.push(tempMsg)
    addRecallable(tempMsg.id, tempMsg._tempId, tempMsg.createTime)
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
    const map: Record<string, string> = { PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货', PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中' }
    return map[d.status] || d.status || ''
  } catch { return '' }
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
    id: 0, _tempId: `temp_${++tempIdCounter}`,
    senderId: myId.value || 0, receiverId: currentTarget.value,
    content, messageType: 3,     isRead: 0,
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value, receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  addRecallable(tempMsg.id, tempMsg._tempId, tempMsg.createTime)
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
    id: 0, _tempId: `temp_${++tempIdCounter}`,
    senderId: myId.value || 0, receiverId: currentTarget.value,
    content, messageType: 3,     isRead: 0,
    createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
    senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
    senderAvatar: userStore.userInfo?.avatar || '',
    receiverName: currentContactName.value, receiverAvatar: ''
  }
  messages.value.push(tempMsg)
  addRecallable(tempMsg.id, tempMsg._tempId, tempMsg.createTime)
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
      delete recallableMap.value[String(msg.id)]
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
      if (chatMsg.messageType === 4) {
        const recallTarget = messages.value.find(m => m.id !== 0 && m.id === chatMsg.id)
        if (recallTarget) {
          recallTarget.content = '该消息已撤回'
          recallTarget.messageType = 4
        }
        updateContactLastMessage(partnerId, '该消息已撤回', 4)
        const c = contacts.value.find(c => c.userId === partnerId)
        if (c && !isFromMe) c.unread = Math.max(0, (c.unread || 0) - 1)
      } else {
        const tempIdx = messages.value.findIndex(m =>
          m._tempId && m.senderId === chatMsg.senderId && m.receiverId === chatMsg.receiverId && m.content === chatMsg.content
        )
        if (tempIdx > -1) {
          const replaced = { ...chatMsg } as DisplayMessage
          messages.value[tempIdx] = replaced
          addRecallable(replaced.id, replaced._tempId, replaced.createTime)
        } else {
          const exists = messages.value.some(m => m.id !== 0 && m.id === chatMsg.id)
          if (!exists) {
            const newMsg = chatMsg as DisplayMessage
            messages.value.push(newMsg)
            addRecallable(newMsg.id, newMsg._tempId, newMsg.createTime)
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

onMounted(async () => {
  const queryTarget = route.query.targetUserId ? Number(route.query.targetUserId) : null
  const queryName = route.query.name as string || '卖家'
  const queryConsult = route.query.consult as string || ''

  if (queryTarget) {
    currentTarget.value = queryTarget
    currentContactName.value = queryName
    await nextTick()
  }

  await loadContacts()

  if (queryTarget) {
    const existing = contacts.value.find(c => c.userId === queryTarget)
    if (existing) {
      currentTarget.value = queryTarget
      currentContactName.value = existing.name
      existing.unread = 0
    } else {
      const newContact: ContactItem = { userId: queryTarget, name: queryName, avatar: '', lastMessage: '', unread: 0 }
      contacts.value.unshift(newContact)
      currentTarget.value = queryTarget
      currentContactName.value = queryName
    }
    await nextTick()
    await loadMessages()
    sendRead(queryTarget)

    if (queryConsult) {
      try {
        JSON.parse(queryConsult)
        const content = queryConsult
        sendChat(queryTarget, content, 3)
        const tempMsg: DisplayMessage = {
          id: 0, _tempId: `temp_${++tempIdCounter}`,
          senderId: myId.value || 0, receiverId: queryTarget,
          content, messageType: 3, isRead: 0,
          createTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
          senderName: userStore.userInfo?.nickname || userStore.userInfo?.username || '',
          senderAvatar: userStore.userInfo?.avatar || '',
          receiverName: currentContactName.value, receiverAvatar: ''
        }
        messages.value.push(tempMsg)
        addRecallable(tempMsg.id, tempMsg._tempId, tempMsg.createTime)
        await nextTick()
        scrollToBottom()
        updateContactLastMessage(queryTarget, content, 3)
      } catch (e) { console.error(e) }
    }
    window.history.replaceState(null, '', '/chat')
  }
})

onUnmounted(() => {
  removeWsHandler()
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
.header-online { font-size: 12px; font-weight: 500; color: #22c55e; background: #f0fdf4; padding: 2px 8px; border-radius: 10px; }
.header-offline { font-size: 12px; font-weight: 500; color: var(--text-muted); background: var(--bg-hover); padding: 2px 8px; border-radius: 10px; }
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
.typing-hint { font-size: 12px; color: var(--text-muted); padding: 4px 8px; font-style: italic; }
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

.context-menu {
  position: fixed; z-index: 9999; background: #fff; border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15); border: 1px solid #e2e8f0;
  padding: 4px 0; min-width: 140px;
}
.context-menu-item {
  display: flex; align-items: center; padding: 10px 16px; cursor: pointer;
  font-size: 13px; color: var(--text-primary); transition: background 0.15s;
  &:hover { background: var(--primary-lighter); color: var(--primary); }
}
</style>

import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { getTotalUnreadCount } from '@/api/chat'
import type { ChatMessageVO } from '@/api/chat'
import type { NotificationVO } from '@/api/notification'

export interface ChatWsMessage {
  type: 'CHAT' | 'TYPING' | 'STOP_TYPING' | 'READ'
  data?: ChatMessageVO
  userId?: number
}

const connected = ref(false)
const chatUnreadMap = ref<Map<number, number>>(new Map())
const notifyUnread = ref(0)

const chatUnread = computed(() => {
  let sum = 0
  chatUnreadMap.value.forEach((v, k) => { if (k > 0) sum += v })
  if (sum === 0 && chatUnreadMap.value.has(-1)) return chatUnreadMap.value.get(-1) || 0
  return sum
})

const chatHandlers = ref<((msg: ChatWsMessage) => void)[]>([])
const notifyHandlers = ref<((msg: NotificationVO) => void)[]>([])

let stompClient: Client | null = null
let chatSub: StompSubscription | null = null
let notifySub: StompSubscription | null = null
let cachedUserId: number | null = null
let reconnectAttempts = 0

function parseUserIdFromToken(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return decoded.userId || decoded.sub || null
  } catch {
    return null
  }
}

function getMyId(): number | null {
  if (cachedUserId) return cachedUserId
  const store = useUserStore()
  if (store.userInfo?.id) {
    cachedUserId = store.userInfo.id
    return cachedUserId
  }
  if (store.token) {
    cachedUserId = parseUserIdFromToken(store.token)
  }
  return cachedUserId
}

function getWsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

function connect() {
  const store = useUserStore()
  if (!store.token) return
  if (stompClient && stompClient.active) return

  cachedUserId = store.userInfo?.id || parseUserIdFromToken(store.token) || null

  const client = new Client({
    brokerURL: getWsUrl(),
    connectHeaders: {
      Authorization: `Bearer ${store.token}`
    },
    reconnectDelay: Math.min(1000 * Math.pow(2, reconnectAttempts), 30000),
    heartbeatIncoming: 20000,
    heartbeatOutgoing: 20000,
    onConnect: () => {
      connected.value = true
      reconnectAttempts = 0
      console.log('[STOMP] Connected, userId:', cachedUserId)
      subscribe(client)
      fetchInitialUnread()
    },
    onDisconnect: () => {
      connected.value = false
      console.log('[STOMP] Disconnected')
    },
    onStompError: (frame) => {
      console.error('[STOMP] Error:', frame.headers['message'], frame.body)
      connected.value = false
      reconnectAttempts++
    },
    onWebSocketClose: () => {
      connected.value = false
    }
  })

  try {
    client.activate()
    stompClient = client
  } catch (e) {
    console.error('[STOMP] Activate failed:', e)
  }
}

function subscribe(client: Client) {
  chatSub = client.subscribe(`/user/queue/chat`, (message: IMessage) => {
    try {
      const body = JSON.parse(message.body)
      if (body.senderId && body.receiverId) {
        const chatMsg = body as ChatMessageVO
        const myId = getMyId()
        if (myId && chatMsg.receiverId === myId) {
          const m = new Map(chatUnreadMap.value)
          m.delete(-1)
          m.set(chatMsg.senderId, (m.get(chatMsg.senderId) || 0) + 1)
          chatUnreadMap.value = m
        }
        chatHandlers.value.forEach(h => h({ type: 'CHAT', data: chatMsg }))
      } else if (body.type === 'TYPING' || body.type === 'STOP_TYPING' || body.type === 'READ') {
        chatHandlers.value.forEach(h => h(body as ChatWsMessage))
      }
    } catch { /* ignore */ }
  })

  notifySub = client.subscribe(`/user/queue/notification`, (message: IMessage) => {
    try {
      const notification = JSON.parse(message.body) as NotificationVO
      notifyUnread.value++
      notifyHandlers.value.forEach(h => h(notification))
    } catch { /* ignore */ }
  })
}

function disconnect() {
  if (chatSub) { chatSub.unsubscribe(); chatSub = null }
  if (notifySub) { notifySub.unsubscribe(); notifySub = null }
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
  connected.value = false
  reconnectAttempts = 0
  cachedUserId = null
}

async function fetchInitialUnread() {
  try {
    const chatCount = await getTotalUnreadCount()
    if (typeof chatCount === 'number' && chatCount > 0) {
      const m = new Map(chatUnreadMap.value)
      m.set(-1, chatCount)
      chatUnreadMap.value = m
    }
  } catch { /* ignore */ }
}

function sendChat(receiverId: number, content: string, messageType: number = 1) {
  if (stompClient && stompClient.active) {
    stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ receiverId, content, messageType })
    })
    return true
  }
  return false
}

function sendTyping(receiverId: number) {
  if (stompClient && stompClient.active) {
    stompClient.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ receiverId })
    })
  }
}

function sendStopTyping(receiverId: number) {
  if (stompClient && stompClient.active) {
    stompClient.publish({
      destination: '/app/chat.stopTyping',
      body: JSON.stringify({ receiverId })
    })
  }
}

function sendRead(receiverId: number) {
  if (stompClient && stompClient.active) {
    stompClient.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ receiverId })
    })
  }
  const m = new Map(chatUnreadMap.value)
  m.delete(receiverId)
  m.delete(-1)
  chatUnreadMap.value = m
}

function onChatMessage(handler: (msg: ChatWsMessage) => void) {
  chatHandlers.value.push(handler)
  return () => {
    const idx = chatHandlers.value.indexOf(handler)
    if (idx > -1) chatHandlers.value.splice(idx, 1)
  }
}

function onNotification(handler: (msg: NotificationVO) => void) {
  notifyHandlers.value.push(handler)
  return () => {
    const idx = notifyHandlers.value.indexOf(handler)
    if (idx > -1) notifyHandlers.value.splice(idx, 1)
  }
}

export function useChatWs() {
  const userStore = useUserStore()

  watch(() => userStore.token, (token) => {
    if (token) connect(); else disconnect()
  }, { immediate: true })

  watch(() => userStore.userInfo?.id, (id) => {
    if (id && !cachedUserId) cachedUserId = id
  })

  return {
    connected,
    chatUnread,
    chatUnreadMap,
    notifyUnread,
    getMyId,
    connect,
    disconnect,
    sendChat,
    sendTyping,
    sendStopTyping,
    sendRead,
    onChatMessage,
    onNotification
  }
}

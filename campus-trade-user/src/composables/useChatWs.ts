import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOnlineUsers } from '@/api/chat'
import type { ChatMessageVO } from '@/api/chat'

interface WsMessage {
  type: string
  data?: ChatMessageVO
  userId?: number
  online?: boolean
}

const wsRef = ref<WebSocket | null>(null)
const connected = ref(false)
const onlineUsers = ref<Set<number>>(new Set())
const unreadMap = ref<Map<number, number>>(new Map())
const totalUnread = computed(() => {
  let sum = 0
  unreadMap.value.forEach(v => { sum += v })
  return sum
})
const messageHandlers = ref<((msg: WsMessage) => void)[]>([])
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectAttempts = 0
let cachedUserId: number | null = null

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
  const token = useUserStore().token
  return `${protocol}//${window.location.host}/ws/chat?token=${token}`
}

function connect() {
  const store = useUserStore()
  if (!store.token || (wsRef.value && wsRef.value.readyState === WebSocket.OPEN)) return

  cachedUserId = store.userInfo?.id || parseUserIdFromToken(store.token) || null

  try {
    const ws = new WebSocket(getWsUrl())

    ws.onopen = () => {
      connected.value = true
      reconnectAttempts = 0
      startHeartbeat(ws)
      fetchOnlineUsers()
    }

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data)
        if (msg.type === 'CHAT' && msg.data) {
          const chatMsg = msg.data as ChatMessageVO
          const myId = getMyId()
          if (myId && chatMsg.receiverId === myId) {
            const partnerId = chatMsg.senderId
            const m = new Map(unreadMap.value)
            m.set(partnerId, (m.get(partnerId) || 0) + 1)
            unreadMap.value = m
          }
          messageHandlers.value.forEach(h => h(msg))
        } else if (msg.type === 'ONLINE_STATUS' && msg.userId != null) {
          const s = new Set(onlineUsers.value)
          if (msg.online) s.add(msg.userId); else s.delete(msg.userId)
          onlineUsers.value = s
          messageHandlers.value.forEach(h => h(msg))
        } else if (msg.type === 'TYPING' || msg.type === 'STOP_TYPING' || msg.type === 'READ') {
          messageHandlers.value.forEach(h => h(msg))
        }
      } catch { /* ignore parse errors */ }
    }

    ws.onclose = () => {
      connected.value = false
      stopHeartbeat()
      scheduleReconnect()
    }

    ws.onerror = () => {
      ws.close()
    }

    wsRef.value = ws
  } catch { scheduleReconnect() }
}

function disconnect() {
  stopHeartbeat()
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
  if (wsRef.value) {
    wsRef.value.onclose = null
    wsRef.value.close()
    wsRef.value = null
  }
  connected.value = false
  reconnectAttempts = 0
  cachedUserId = null
}

function scheduleReconnect() {
  if (reconnectTimer) return
  if (!useUserStore().token) return
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
  reconnectAttempts++
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, delay)
}

function startHeartbeat(ws: WebSocket) {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'PING' }))
    }
  }, 30000)
}

function stopHeartbeat() {
  if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null }
}

function sendChat(receiverId: number, content: string, messageType: number = 1) {
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) {
    wsRef.value.send(JSON.stringify({ type: 'CHAT', receiverId, content, messageType }))
    return true
  }
  return false
}

function sendTyping(receiverId: number) {
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) {
    wsRef.value.send(JSON.stringify({ type: 'TYPING', receiverId }))
  }
}

function sendStopTyping(receiverId: number) {
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) {
    wsRef.value.send(JSON.stringify({ type: 'STOP_TYPING', receiverId }))
  }
}

function sendRead(receiverId: number) {
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) {
    wsRef.value.send(JSON.stringify({ type: 'READ', receiverId }))
  }
  const m = new Map(unreadMap.value)
  m.delete(receiverId)
  unreadMap.value = m
}

function onMessage(handler: (msg: WsMessage) => void) {
  messageHandlers.value.push(handler)
  return () => {
    const idx = messageHandlers.value.indexOf(handler)
    if (idx > -1) messageHandlers.value.splice(idx, 1)
  }
}

async function fetchOnlineUsers() {
  try {
    const ids = await getOnlineUsers()
    if (Array.isArray(ids)) onlineUsers.value = new Set(ids)
  } catch { /* ignore */ }
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
    totalUnread,
    unreadMap,
    onlineUsers,
    getMyId,
    connect,
    disconnect,
    sendChat,
    sendTyping,
    sendStopTyping,
    sendRead,
    onMessage
  }
}

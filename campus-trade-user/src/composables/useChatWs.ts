import { ref, computed, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { getOnlineUsers, getTotalUnreadCount } from '@/api/chat'
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
  unreadMap.value.forEach((v, k) => { if (k > 0) sum += v })
  if (sum === 0 && unreadMap.value.has(-1)) return unreadMap.value.get(-1) || 0
  return sum
})
const messageHandlers = ref<((msg: WsMessage) => void)[]>([])
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectAttempts = 0
let cachedUserId: number | null = null
let connecting = false

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
  if (!store.token || connecting) return
  if (wsRef.value && wsRef.value.readyState === WebSocket.OPEN) return
  if (wsRef.value && wsRef.value.readyState === WebSocket.CONNECTING) return

  connecting = true
  cachedUserId = store.userInfo?.id || parseUserIdFromToken(store.token) || null

  try {
    const ws = new WebSocket(getWsUrl())

    ws.onopen = () => {
      connecting = false
      connected.value = true
      reconnectAttempts = 0
      console.log('[WS] Connected, userId:', cachedUserId)
      startHeartbeat(ws)
      fetchOnlineUsers()
      fetchTotalUnread()
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
            m.delete(-1)
            m.set(partnerId, (m.get(partnerId) || 0) + 1)
            unreadMap.value = m
          }
        }
        messageHandlers.value.forEach(h => {
          try { h(msg) } catch { /* ignore handler errors */ }
        })
      } catch { /* ignore parse errors */ }
    }

    ws.onclose = (event) => {
      connecting = false
      connected.value = false
      stopHeartbeat()
      console.log('[WS] Disconnected, code:', event.code, 'reason:', event.reason)
      if (event.code !== 1000) {
        scheduleReconnect()
      }
    }

    ws.onerror = (event) => {
      connecting = false
      console.error('[WS] Error:', event)
      ws.close()
    }

    wsRef.value = ws
  } catch (e) {
    connecting = false
    console.error('[WS] Connect failed:', e)
    scheduleReconnect()
  }
}

function disconnect() {
  stopHeartbeat()
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
  if (wsRef.value) {
    wsRef.value.onclose = null
    wsRef.value.close(1000)
    wsRef.value = null
  }
  connected.value = false
  reconnectAttempts = 0
  cachedUserId = null
  connecting = false
}

function scheduleReconnect() {
  if (reconnectTimer) return
  if (!useUserStore().token) return
  const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
  reconnectAttempts++
  console.log('[WS] Reconnecting in', delay, 'ms, attempt:', reconnectAttempts)
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
  console.warn('[WS] Cannot send: not connected')
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
  m.delete(-1)
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

async function fetchTotalUnread() {
  try {
    const count = await getTotalUnreadCount()
    if (typeof count === 'number' && count > 0) {
      const m = new Map(unreadMap.value)
      m.set(-1, count)
      unreadMap.value = m
    }
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

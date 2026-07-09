import { ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import type { ChatMessageVO } from '@/api/chat'

interface WsMessage {
  type: string
  data?: ChatMessageVO
  userId?: number
  online?: boolean
}

const wsRef = ref<WebSocket | null>(null)
const connected = ref(false)
const wsUnreadCount = ref(0)
const onlineUsers = ref<Set<number>>(new Set())
const messageHandlers = ref<((msg: WsMessage) => void)[]>([])
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectAttempts = 0

function getWsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const token = useUserStore().token
  return `${protocol}//${window.location.host}/ws/chat?token=${token}`
}

function connect() {
  const token = useUserStore().token
  if (!token || (wsRef.value && wsRef.value.readyState === WebSocket.OPEN)) return

  try {
    const ws = new WebSocket(getWsUrl())

    ws.onopen = () => {
      connected.value = true
      reconnectAttempts = 0
      startHeartbeat(ws)
    }

    ws.onmessage = (event) => {
      try {
        const msg: WsMessage = JSON.parse(event.data)
        if (msg.type === 'CHAT' && msg.data) {
          wsUnreadCount.value++
          messageHandlers.value.forEach(h => h(msg))
        } else if (msg.type === 'ONLINE_STATUS' && msg.userId != null) {
          const s = new Set(onlineUsers.value)
          if (msg.online) s.add(msg.userId); else s.delete(msg.userId)
          onlineUsers.value = s
          messageHandlers.value.forEach(h => h(msg))
        } else if (msg.type === 'TYPING') {
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

function onMessage(handler: (msg: WsMessage) => void) {
  messageHandlers.value.push(handler)
  return () => {
    const idx = messageHandlers.value.indexOf(handler)
    if (idx > -1) messageHandlers.value.splice(idx, 1)
  }
}

export function useChatWs() {
  const userStore = useUserStore()

  watch(() => userStore.token, (token) => {
    if (token) connect(); else disconnect()
  }, { immediate: true })

  return {
    connected,
    wsUnreadCount,
    onlineUsers,
    connect,
    disconnect,
    sendChat,
    sendTyping,
    onMessage
  }
}
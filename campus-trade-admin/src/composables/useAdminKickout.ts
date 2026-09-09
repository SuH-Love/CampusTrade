import { watch } from 'vue'
import { useAdminStore } from '@/stores/admin'
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { ElMessage } from 'element-plus'
import router from '@/router'

let stompClient: Client | null = null
let kickoutSub: StompSubscription | null = null

function getWsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws`
}

function connect(token: string) {
  if (stompClient && stompClient.active) return

  const client = new Client({
    brokerURL: getWsUrl(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 500,
    heartbeatIncoming: 20000,
    heartbeatOutgoing: 20000,
    onConnect: () => {
      kickoutSub = client.subscribe(`/user/queue/kickout`, (message: IMessage) => {
        try {
          const body = JSON.parse(message.body)
          if (body.type === 'KICKED') {
            const store = useAdminStore()
            store.clearAuth()
            disconnect()
            ElMessage.error({ message: '账号在其他设备登录，您已被自动退出', duration: 5000, grouping: true })
            router.push('/login')
          }
        } catch { /* ignore */ }
      })
    },
    onStompError: () => {},
    onWebSocketClose: () => {}
  })

  try {
    client.activate()
    stompClient = client
  } catch { /* ignore */ }
}

function disconnect() {
  if (kickoutSub) { kickoutSub.unsubscribe(); kickoutSub = null }
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

export function useAdminKickout() {
  const adminStore = useAdminStore()

  watch(() => adminStore.token, (token) => {
    if (token) connect(token); else disconnect()
  }, { immediate: true })
}
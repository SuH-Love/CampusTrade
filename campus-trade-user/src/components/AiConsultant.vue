<template>
  <div class="ai-consultant">
    <transition name="slide-up">
      <div v-if="visible" class="chat-panel">
        <div class="chat-header">
          <div class="header-info">
            <div class="avatar">
              <el-icon :size="20"><ChatDotRound /></el-icon>
            </div>
            <div class="header-text">
              <span class="title">小校 AI助手</span>
              <span class="subtitle">{{ statusText }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip content="清空对话" placement="top">
              <el-button :icon="Delete" circle size="small" @click="handleClear" />
            </el-tooltip>
            <el-button :icon="Close" circle size="small" @click="visible = false" />
          </div>
        </div>

        <div class="chat-body" ref="bodyRef">
          <div v-if="messages.length === 0" class="welcome">
            <div class="welcome-icon">
              <el-icon :size="36"><ChatDotRound /></el-icon>
            </div>
            <p class="welcome-title">你好，我是小校</p>
            <p class="welcome-desc">校园贸易平台AI助手，有什么可以帮你的吗？</p>
            <div class="suggestions">
              <el-button
                v-for="s in suggestions"
                :key="s"
                size="small"
                round
                @click="sendMessage(s)"
              >{{ s }}</el-button>
            </div>
          </div>

          <div v-for="msg in messages" :key="msg.id" :class="['msg-row', msg.role]">
            <div class="msg-bubble">
              <span v-if="msg.role === 'assistant' && msg.loading && !msg.content" class="typing">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </span>
              <template v-else>
                <div v-if="msg.toolCalls.length > 0" class="tool-calls">
                  <div
                    v-for="tc in msg.toolCalls"
                    :key="tc.id"
                    class="tool-call-card"
                  >
                    <div class="tool-call-header" @click="tc.expanded = !tc.expanded">
                      <el-icon :size="14"><Tools /></el-icon>
                      <span class="tool-name">{{ toolDisplayName(tc.name) }}</span>
                      <el-icon :size="12" class="expand-icon">
                        <ArrowDown v-if="!tc.expanded" />
                        <ArrowUp v-else />
                      </el-icon>
                    </div>
                    <div v-if="tc.expanded" class="tool-call-body">
                      <div v-if="tc.args && Object.keys(tc.args).length > 0" class="tool-args">
                        <span class="tool-label">参数：</span>
                        <code>{{ JSON.stringify(tc.args) }}</code>
                      </div>
                      <div v-if="tc.result" class="tool-result">
                        <span class="tool-label">结果：</span>
                        <pre>{{ tc.result }}</pre>
                      </div>
                    </div>
                  </div>
                </div>
                <div
                  v-if="msg.content"
                  class="msg-content"
                  v-html="msg.role === 'assistant' && !msg.streaming ? renderMarkdown(msg.content) : escapeHtml(msg.content)"
                ></div>
                <div v-if="msg.thinkingStatus" class="thinking-status">
                  <el-icon :size="12" class="is-loading"><Loading /></el-icon>
                  {{ msg.thinkingStatus }}
                </div>
                <div v-if="msg.error" class="msg-error">
                  <span>{{ msg.content }}</span>
                  <el-button size="small" text @click="retryLastMessage">重试</el-button>
                </div>
                <div v-if="msg.content && !msg.loading" class="msg-footer">
                  <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
                  <el-button
                    v-if="msg.role === 'assistant'"
                    size="small"
                    text
                    class="copy-btn"
                    @click="copyMessage(msg.content)"
                  >
                    <el-icon :size="12"><CopyDocument /></el-icon>
                  </el-button>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <el-input
            v-model="inputText"
            placeholder="输入你的问题..."
            @keyup.enter="handleEnter"
            :disabled="loading"
            maxlength="500"
            clearable
          >
            <template #append>
              <el-button
                v-if="!loading"
                :icon="Promotion"
                @click="handleSend"
                :disabled="!inputText.trim()"
              />
              <el-button
                v-else
                :icon="VideoPause"
                @click="handleStop"
              />
            </template>
          </el-input>
        </div>
      </div>
    </transition>

    <div v-if="!visible" class="float-btn" @click="visible = true; hasNewBadge = false">
      <el-badge :is-dot="hasNewBadge" type="primary">
        <el-icon :size="28"><ChatDotRound /></el-icon>
      </el-badge>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ChatDotRound, Close, Delete, Promotion, Tools, ArrowDown, ArrowUp, Loading, VideoPause, CopyDocument } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { chatStream, getAiStatus, clearSession, getSessionHistory } from '@/api/ai'
import { useUserStore } from '@/stores/user'
import MarkdownIt from 'markdown-it'

interface ToolCallInfo {
  id: number
  name: string
  args: Record<string, unknown>
  result: string
  expanded: boolean
}

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  streaming?: boolean
  thinkingStatus?: string
  error?: boolean
  toolCalls: ToolCallInfo[]
  timestamp: number
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true
})

const renderMarkdown = (text: string): string => {
  return md.render(text || '')
}

const escapeHtml = (text: string): string => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML.replace(/\n/g, '<br>')
}

const formatTime = (ts?: number): string => {
  if (!ts) return ''
  const d = new Date(ts)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const copyMessage = async (content: string) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(content)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = content
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success({ message: '已复制', duration: 1000 })
  } catch {
    ElMessage.warning('复制失败')
  }
}

const toolDisplayName = (name: string): string => {  const names: Record<string, string> = {
    get_order_status: '查询订单', get_order_by_no: '查询订单详情',
    search_goods: '搜索商品', get_user_profile: '查询个人信息',
    get_user_stats: '查询统计', get_my_goods: '查询我的商品',
    get_goods_detail: '查询商品详情', get_favorites: '查询收藏',
    get_cart: '查询购物车', get_addresses: '查询地址',
    get_ratings: '查询评价', get_notifications: '查询通知',
    get_unread_message_count: '查询未读消息', get_recent_contacts: '查询联系人',
    get_follow_list: '查询关注', get_categories: '查询分类',
    get_order_fund_logs: '查询资金流水', get_announcements: '查询公告',
    cancel_order: '取消订单', confirm_receipt: '确认收货',
    ship_order: '发货', request_refund: '申请退款',
    rate_order: '评价订单', toggle_favorite: '收藏/取消',
    add_to_cart: '加入购物车', toggle_follow_user: '关注/取消',
    online_offline_goods: '上架/下架', add_address: '添加地址',
    submit_report: '举报', admin_dashboard: '平台概览',
    admin_list_users: '用户列表', admin_ban_user: '封禁/解封',
    admin_audit_goods: '审核商品', admin_list_reports: '举报列表',
    admin_handle_refund: '处理退款'
  }
  return names[name] || name
}

const visible = ref(false)
const inputText = ref('')
const loading = ref(false)
const messages = ref<Message[]>([])
const sessionId = ref<string | undefined>(undefined)
const bodyRef = ref<HTMLElement>()
const aiEnabled = ref(true)
const hasNewBadge = ref(true)
const statusText = ref('在线')
let streamHandle: { close: () => void } | null = null
let msgIdCounter = 0
let toolCallIdCounter = 0
let lastUserMessage = ''

const userStore = useUserStore()
const STORAGE_KEY = 'ai:sessionId'

const getUserSessionId = (): string | undefined => {
  const uid = userStore.userInfo?.id
  if (uid != null) return `user:${uid}`
  return undefined
}

const restoreSession = () => {
  const userSid = getUserSessionId()
  if (userSid) {
    sessionId.value = userSid
    localStorage.setItem(STORAGE_KEY, userSid)
    return
  }
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved && !saved.startsWith('user:')) {
    sessionId.value = saved
  } else {
    localStorage.removeItem(STORAGE_KEY)
  }
}

const loadHistory = async () => {
  if (!sessionId.value) return
  try {
    const history = await getSessionHistory(sessionId.value)
    if (history && history.length > 0) {
      const recent = history.slice(-10)
      messages.value = recent.map(msg => ({
        id: ++msgIdCounter,
        role: msg.role as 'user' | 'assistant',
        content: msg.content,
        toolCalls: [],
        timestamp: Date.now()
      }))
      if (history.length > 10) {
        messages.value.unshift({ id: ++msgIdCounter, role: 'assistant', content: `（已加载最近10条，共${history.length}条历史对话）`, toolCalls: [], timestamp: Date.now() })
      }
      scrollToBottom()
    }
  } catch {}
}

const suggestions = [
  '如何发布商品？',
  '怎么支付订单？',
  '忘记密码怎么办？',
  '平台收手续费吗？'
]

const scrollToBottom = () => {
  nextTick(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
  })
}

const sendMessage = async (text: string) => {
  const trimmed = text.trim()
  if (!trimmed || loading.value) return

  if (!sessionId.value) {
    const userSid = getUserSessionId()
    if (userSid) {
      sessionId.value = userSid
      localStorage.setItem(STORAGE_KEY, userSid)
    }
  }

  lastUserMessage = trimmed
  inputText.value = ''
  messages.value.push({ id: ++msgIdCounter, role: 'user', content: trimmed, toolCalls: [], timestamp: Date.now() })
  const assistantMsg: Message = {
    id: ++msgIdCounter,
    role: 'assistant',
    content: '',
    loading: true,
    streaming: true,
    toolCalls: [],
    timestamp: Date.now()
  }
  messages.value.push(assistantMsg)
  loading.value = true
  scrollToBottom()

  if (!aiEnabled.value) {
    assistantMsg.loading = false
    assistantMsg.streaming = false
    assistantMsg.content = 'AI助手暂时不可用，请稍后再试或联系人工客服。'
    loading.value = false
    return
  }

  try {
    let accumulated = ''
    const tokenQueue: string[] = []
    let rafId: number | null = null
    let finished = false

    const finishStream = () => {
      assistantMsg.loading = false
      assistantMsg.streaming = false
      assistantMsg.thinkingStatus = undefined
      loading.value = false
      streamHandle = null
      scrollToBottom()
    }

    const processQueue = () => {
      if (tokenQueue.length > 0) {
        accumulated += tokenQueue.shift()!
        assistantMsg.content = accumulated
        assistantMsg.loading = false
        assistantMsg.thinkingStatus = undefined
        scrollToBottom()
        rafId = requestAnimationFrame(processQueue)
      } else {
        rafId = null
        if (finished) finishStream()
      }
    }

    streamHandle = chatStream(
      trimmed,
      sessionId.value,
      (token: string) => {
        tokenQueue.push(token)
        if (rafId === null) {
          rafId = requestAnimationFrame(processQueue)
        }
      },
      () => {
        finished = true
        if (rafId === null && tokenQueue.length === 0) {
          finishStream()
        }
      },
      (error: string) => {
        if (rafId !== null) {
          cancelAnimationFrame(rafId)
          rafId = null
        }
        tokenQueue.length = 0
        assistantMsg.loading = false
        assistantMsg.streaming = false
        assistantMsg.thinkingStatus = undefined
        assistantMsg.content = error || 'AI服务暂时不可用，请稍后再试。'
        assistantMsg.error = true
        loading.value = false
        streamHandle = null
      },
      (sid: string) => {
        sessionId.value = sid
        localStorage.setItem(STORAGE_KEY, sid)
      },
      (status: string) => {
        if (!assistantMsg.content) {
          assistantMsg.thinkingStatus = status
        }
      },
      (toolCall: { name: string; args: Record<string, unknown> }) => {
        assistantMsg.toolCalls.push({
          id: ++toolCallIdCounter,
          name: toolCall.name,
          args: toolCall.args,
          result: '',
          expanded: false
        })
        assistantMsg.thinkingStatus = `正在调用 ${toolDisplayName(toolCall.name)}...`
        scrollToBottom()
      },
      (toolResult: { name: string; result: string }) => {
        const tc = assistantMsg.toolCalls.find(t => t.name === toolResult.name && !t.result)
        if (tc) {
          tc.result = toolResult.result
        }
        assistantMsg.thinkingStatus = undefined
        scrollToBottom()
      }
    )
  } catch (e) {
    assistantMsg.loading = false
    assistantMsg.streaming = false
    assistantMsg.content = '请求失败，请稍后再试。'
    assistantMsg.error = true
    loading.value = false
  }
}

const handleSend = () => sendMessage(inputText.value)
const handleEnter = () => {
  if (inputText.value.trim()) handleSend()
}
const handleStop = () => {
  if (streamHandle) {
    streamHandle.close()
    streamHandle = null
  }
  loading.value = false
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg && lastMsg.role === 'assistant') {
    lastMsg.loading = false
    lastMsg.streaming = false
    lastMsg.thinkingStatus = undefined
    if (!lastMsg.content) {
      lastMsg.content = '已停止'
    }
  }
}

const retryLastMessage = () => {
  if (lastUserMessage) {
    const lastAssistant = messages.value[messages.value.length - 1]
    if (lastAssistant && lastAssistant.role === 'assistant') {
      messages.value.pop()
    }
    sendMessage(lastUserMessage)
  }
}

const handleClear = async () => {
  if (streamHandle) {
    streamHandle.close()
    streamHandle = null
  }
  loading.value = false
  if (sessionId.value) {
    try {
      await clearSession(sessionId.value)
    } catch {
      ElMessage.warning('清空远程对话失败，本地已清空')
    }
  }
  messages.value = []
  localStorage.removeItem(STORAGE_KEY)
  const userSid = getUserSessionId()
  if (userSid) {
    sessionId.value = userSid
    localStorage.setItem(STORAGE_KEY, userSid)
  } else {
    sessionId.value = undefined
  }
  ElMessage.success('对话已清空')
}

watch(visible, async (val) => {
  if (val) {
    if (!sessionId.value) restoreSession()
    await loadHistory()
  }
})

watch(() => userStore.userInfo, async () => {
  const userSid = getUserSessionId()
  if (userSid && sessionId.value !== userSid) {
    sessionId.value = userSid
    localStorage.setItem(STORAGE_KEY, userSid)
    if (visible.value) await loadHistory()
  }
})

onMounted(async () => {
  try {
    const status = await getAiStatus()
    aiEnabled.value = status.enabled
    statusText.value = status.enabled ? `在线 · ${status.model}` : '离线'
  } catch {
    aiEnabled.value = false
    statusText.value = '离线'
  }
  if (userStore.token && !userStore.userInfo) {
    try { await userStore.fetchUserInfo() } catch {}
  }
  restoreSession()
})

onUnmounted(() => {
  if (streamHandle) streamHandle.close()
})
</script>

<style scoped lang="scss">
.ai-consultant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 200;
}

.float-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.3);
  }
}

.chat-panel {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 380px;
  height: 560px;
  max-height: calc(100vh - 48px);
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
}

.chat-header {
  padding: 12px 16px;
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;

  .header-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .header-text {
    display: flex;
    flex-direction: column;

    .title {
      font-size: 15px;
      font-weight: 600;
    }

    .subtitle {
      font-size: 12px;
      opacity: 0.8;
    }
  }

  .header-actions {
    display: flex;
    gap: 4px;

    :deep(.el-button) {
      color: #fff;
      background: rgba(255, 255, 255, 0.15);
      border: none;

      &:hover {
        background: rgba(255, 255, 255, 0.25);
      }
    }
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.welcome {
  text-align: center;
  padding: 24px 0;

  .welcome-icon {
    color: var(--el-color-primary);
    margin-bottom: 12px;
  }

  .welcome-title {
    font-size: 18px;
    font-weight: 700;
    margin: 0 0 4px;
  }

  .welcome-desc {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin: 0 0 16px;
  }

  .suggestions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
  }
}

.msg-row {
  display: flex;

  &.user {
    justify-content: flex-end;
  }

  &.assistant {
    justify-content: flex-start;
  }
}

.msg-bubble {
  max-width: 85%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;

  .user & {
    background: var(--el-color-primary);
    color: #fff;
    border-bottom-right-radius: 4px;
  }

  .assistant & {
    background: var(--el-fill-color-light);
    color: var(--el-text-color-primary);
    border-bottom-left-radius: 4px;
  }
}

.msg-content {
  :deep(p) {
    margin: 0 0 8px;
    &:last-child { margin-bottom: 0; }
  }
  :deep(ul), :deep(ol) {
    margin: 4px 0 8px;
    padding-left: 20px;
  }
  :deep(li) {
    margin: 2px 0;
  }
  :deep(code) {
    background: rgba(0, 0, 0, 0.06);
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 13px;
  }
  :deep(pre) {
    background: rgba(0, 0, 0, 0.06);
    padding: 8px 12px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 4px 0;
    code {
      background: none;
      padding: 0;
    }
  }
  :deep(table) {
    border-collapse: collapse;
    margin: 4px 0;
    th, td {
      border: 1px solid var(--el-border-color);
      padding: 4px 8px;
    }
    th {
      background: rgba(0, 0, 0, 0.04);
    }
  }
  :deep(strong) {
    font-weight: 700;
  }
  :deep(a) {
    color: var(--el-color-primary);
    text-decoration: none;
    &:hover { text-decoration: underline; }
  }
}

.tool-calls {
  margin-bottom: 8px;
}

.tool-call-card {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 8px;
  margin-bottom: 4px;
  overflow: hidden;
  font-size: 12px;
}

.tool-call-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  cursor: pointer;
  color: var(--el-text-color-secondary);

  .tool-name {
    font-weight: 600;
    color: var(--el-color-primary);
  }

  .expand-icon {
    margin-left: auto;
  }

  &:hover {
    background: rgba(0, 0, 0, 0.06);
  }
}

.tool-call-body {
  padding: 6px 10px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.tool-args, .tool-result {
  margin: 4px 0;

  .tool-label {
    color: var(--el-text-color-secondary);
    font-weight: 600;
  }

  code {
    background: rgba(0, 0, 0, 0.06);
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 11px;
  }

  pre {
    background: rgba(0, 0, 0, 0.06);
    padding: 6px 8px;
    border-radius: 4px;
    overflow-x: auto;
    margin: 4px 0;
    font-size: 11px;
    white-space: pre-wrap;
    max-height: 120px;
    overflow-y: auto;
  }
}

.thinking-status {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-top: 4px;

  .is-loading {
    animation: rotating 1.5s linear infinite;
  }
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.msg-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-color-danger);
}

.msg-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  opacity: 0.6;
}

.msg-time {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.copy-btn {
  padding: 2px;
  min-height: auto;
}

.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--el-text-color-secondary);
    animation: typing-bounce 1.4s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.chat-footer {
  padding: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.slide-up-enter-from,
.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

@media (max-width: 480px) {
  .chat-panel {
    width: calc(100vw - 48px);
    height: calc(100vh - 120px);
  }
}
</style>

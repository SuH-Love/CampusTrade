<template>
  <div class="ai-consultant">
    <transition name="slide-up">
      <div v-if="visible" class="chat-panel" :class="{ expanded }">
        <div class="chat-header">
          <div class="header-info">
            <div class="avatar">
              <span class="ai-header-text">AI</span>
            </div>
            <div class="header-text">
              <span class="title">小苏 AI助手</span>
              <span class="subtitle">{{ statusText }}</span>
            </div>
          </div>
          <div class="header-actions">
            <el-tooltip :content="expanded ? '收起' : '放大'" placement="top">
              <el-button :icon="expanded ? CopyDocument : FullScreen" circle size="small" @click="toggleExpand" />
            </el-tooltip>
            <el-tooltip content="清空对话" placement="top">
              <el-button :icon="Delete" circle size="small" @click="handleClear" />
            </el-tooltip>
            <el-button :icon="Close" circle size="small" @click="visible = false" />
          </div>
        </div>

        <div class="chat-body" ref="bodyRef" @scroll="onBodyScroll">
          <div v-if="messages.length === 0" class="welcome">
            <div class="welcome-icon">
              <el-icon :size="36"><ChatDotRound /></el-icon>
            </div>
            <p class="welcome-title">你好，我是小苏</p>
            <p class="welcome-desc">校园贸易平台AI助手，有什么可以帮你的吗？</p>
            <div class="suggestions">
              <p class="suggestions-label">猜你想了解</p>
              <div class="suggestions-grid">
                <button
                  v-for="s in displaySuggestions"
                  :key="s"
                  class="suggestion-item"
                  @click="sendMessage(s)"
                >
                  <span class="suggestion-text">{{ s }}</span>
                </button>
              </div>
            </div>
          </div>

          <div v-if="totalHistoryCount > messages.filter(m => !m.isSystem).length" class="load-more-bar">
            <el-button text size="small" @click="loadAllHistory">加载全部历史对话（共{{ totalHistoryCount }}条）</el-button>
          </div>

          <div v-for="(msg, idx) in messages" :key="msg.id" :class="['msg-row', msg.role]">
            <div class="msg-wrapper">
              <div class="msg-bubble">
                <span v-if="msg.role === 'assistant' && msg.loading && !msg.content" class="typing">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </span>
                <template v-else>
                  <div v-if="msg.thinkingSteps.length > 0" class="thinking-process">
                    <div class="process-header" @click="!msg.loading && (msg.thinkingExpanded = !msg.thinkingExpanded)">
                      <el-icon v-if="!msg.thinkingStatus" size="14" class="process-check"><CircleCheck /></el-icon>
                      <el-icon v-else size="14" class="is-loading"><Loading /></el-icon>
                      <span class="process-title">{{ msg.thinkingStatus || '分析完成' }}</span>
                      <span class="process-time">{{ ((msg.thinkingEndTime || Date.now()) - (msg.thinkingStartTime || Date.now())) / 1000 | 1 }}s</span>
                      <el-icon v-if="!msg.loading" size="12" class="process-expand-icon">
                        <ArrowDown v-if="!msg.thinkingExpanded" />
                        <ArrowUp v-else />
                      </el-icon>
                    </div>
                    <div v-if="msg.loading || msg.thinkingExpanded" class="process-body">
                      <template v-for="(step, si) in msg.thinkingSteps" :key="si">
                        <div class="process-step">
                          <div class="step-marker">
                            <div class="step-dot"></div>
                            <div v-if="si < msg.thinkingSteps.length - 1" class="step-line"></div>
                          </div>
                          <div class="step-content">
                            <div class="step-title">{{ step.status }}</div>
                            <div v-if="step.detail" class="step-detail">{{ step.detail }}</div>
                          </div>
                        </div>
                      </template>
                    </div>
                  </div>
                  <div v-if="msg.toolCalls.length > 0" class="tool-calls">
                    <div
                      v-for="tc in msg.toolCalls"
                      :key="tc.id"
                      class="tool-call-card"
                    >
                      <div class="tool-call-header" @click="tc.expanded = !tc.expanded">
                        <el-icon :size="14"><Tools /></el-icon>
                        <span class="tool-name">{{ toolDisplayName(tc.name) }}</span>
                        <span v-if="tc.status === 'pending'" class="tool-status tool-status-pending">等待中</span>
                        <span v-else-if="tc.status === 'running'" class="tool-status tool-status-running">执行中</span>
                        <span v-else-if="tc.status === 'error'" class="tool-status tool-status-error">失败</span>
                        <span v-else-if="tc.duration" class="tool-duration">{{ tc.duration }}ms</span>
                        <el-icon :size="12" class="expand-icon">
                          <ArrowDown v-if="!tc.expanded" />
                          <ArrowUp v-else />
                        </el-icon>
                      </div>
                      <div v-if="tc.expanded" class="tool-call-body">
                        <div v-if="tc.result" class="tool-result">
                          <pre>{{ tc.result }}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div
                    v-if="msg.role === 'user' && parseQuote(msg.content).quote"
                    class="msg-quote-display"
                  >
                    <span class="quote-mark">"</span>
                    <span class="quote-text-display">{{ parseQuote(msg.content).quote.length > 40 ? parseQuote(msg.content).quote.slice(0, 40) + '...' : parseQuote(msg.content).quote }}</span>
                  </div>
                  <div
                    v-if="msg.content && parseQuote(msg.content).body"
                    class="msg-content"
                    v-html="msg.role === 'assistant' && !msg.streaming ? renderMarkdown(parseQuote(msg.content).body) : escapeHtml(parseQuote(msg.content).body)"
                  ></div>
                  <div
                    v-if="msg.content && !parseQuote(msg.content).quote && !parseQuote(msg.content).body"
                    class="msg-content"
                    v-html="msg.role === 'assistant' && !msg.streaming ? renderMarkdown(msg.content) : escapeHtml(msg.content)"
                  ></div>
                  <div v-if="msg.error" class="msg-error">
                    <span>{{ msg.content }}</span>
                    <el-button size="small" text @click="retryLastMessage">重试</el-button>
                  </div>
                  <div v-if="msg.role === 'assistant' && msg.content && !msg.loading && !msg.isSystem" class="response-footer">
                    <div class="footer-left">
                      <template v-if="msg.thinkingSteps.length > 0">
                        <el-icon :size="14" class="check-icon"><CircleCheck /></el-icon>
                        <span class="footer-status">任务完成 | 总耗时 {{ ((msg.thinkingEndTime || Date.now()) - msg.thinkingStartTime) / 1000 | 1 }}s</span>
                      </template>
                    </div>
                    <div class="footer-right">
                      <span class="footer-time">{{ formatTime(msg.timestamp) }}</span>
                      <button class="action-btn" title="复制" @click="copyMessage(msg.content)">
                        <el-icon :size="13"><CopyDocument /></el-icon>
                      </button>
                      <button class="action-btn" title="引用" @click="quoteMessage(msg.content)">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z"/></svg>
                      </button>
                      <button
                        v-if="!msg.error && idx === messages.length - 1 && !loading"
                        class="action-btn"
                        title="重新回答"
                        @click="regenerateAnswer(idx)"
                      >
                        <el-icon :size="13"><RefreshRight /></el-icon>
                      </button>
                      <button
                        v-if="!msg.error && !msg.loading"
                        class="action-btn"
                        :class="{ active: msg.feedback === 1 }"
                        title="有帮助"
                        @click="handleFeedback(msg, 1, idx)"
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13.17 1 6.59 8.59C6.22 8.95 6 9.45 6 10v9c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"/></svg>
                      </button>
                      <button
                        v-if="!msg.error && !msg.loading"
                        class="action-btn"
                        :class="{ active: msg.feedback === -1 }"
                        title="无帮助"
                        @click="handleFeedback(msg, -1, idx)"
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v2c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L10.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2zm4 0v12h4V3h-4z"/></svg>
                      </button>
                    </div>
                  </div>
                </template>
              </div>
              <div v-if="msg.role === 'user' && msg.content && !msg.loading && msg.timestamp" class="msg-footer">
                <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
              </div>
            </div>
          </div>
        </div>

        <transition name="fade-scale">
          <div v-if="showScrollBottom" class="scroll-bottom-btn" @click="scrollToBottom(true)">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="6 9 12 15 18 9"></polyline>
            </svg>
          </div>
        </transition>

        <div class="chat-footer">
          <div v-if="pendingImages.length > 0" class="image-preview-bar">
            <div v-for="(img, i) in pendingImages" :key="i" class="image-preview-item">
              <img :src="img.url" alt="preview" />
              <button class="remove-img" @click="pendingImages.splice(i, 1)">×</button>
            </div>
          </div>
          <div v-if="quotedContent" class="quote-bar">
            <span class="quote-icon">"</span>
            <span class="quote-text">{{ quotedContent.length > 40 ? quotedContent.slice(0, 40) + '...' : quotedContent }}</span>
            <button class="quote-close" @click="quotedContent = ''">
              <el-icon :size="14"><Close /></el-icon>
            </button>
          </div>
          <div class="input-wrapper" :class="{ disabled: loading }">
            <button class="upload-btn" :disabled="loading" @click="triggerUpload">
              <el-icon :size="18"><Picture /></el-icon>
            </button>
            <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleImageUpload" />
            <textarea
              ref="textareaRef"
              v-model="inputText"
              class="chat-textarea"
              placeholder="输入你的问题..."
              :disabled="loading"
              maxlength="500"
              rows="1"
              @keydown.enter="handleEnter"
              @input="autoResize"
              @paste="handlePaste"
            ></textarea>
            <button
              v-if="!loading"
              class="send-btn"
              :disabled="!inputText.trim()"
              @click="handleSend"
            >
              <el-icon :size="18"><Promotion /></el-icon>
            </button>
            <button v-else class="send-btn stop-btn" @click="handleStop">
              <el-icon :size="18"><VideoPause /></el-icon>
            </button>
          </div>
          <div class="input-hint">
            <span>Enter 发送 · Shift+Enter 换行</span>
          </div>
        </div>
      </div>
    </transition>

    <div v-if="!visible" class="float-btn"
      :class="{ snapping: !isDragging }"
      :style="{ left: btnPos.x + 'px', top: btnPos.y + 'px', right: 'auto', bottom: 'auto' }"
      @mousedown="onDragStart"
      @touchstart="onDragStart"
    >
      <el-badge :is-dot="hasNewBadge" type="primary">
        <span class="ai-fab-text">AI</span>
      </el-badge>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { ChatDotRound, Close, Delete, Promotion, Tools, ArrowDown, ArrowUp, Loading, VideoPause, CopyDocument, RefreshRight, FullScreen, CircleCheck, Picture } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatStream, getAiStatus, clearSession, getSessionHistory, submitAiFeedback, getSessionFeedback } from '@/api/ai'
import { useUserStore } from '@/stores/user'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const ThinkingIcon = Loading

interface ToolCallInfo {
  id: number
  callId?: string
  name: string
  args: Record<string, unknown>
  result: string
  expanded: boolean
  duration?: string
  status?: 'pending' | 'running' | 'done' | 'error'
}

interface ThinkingStep {
  status: string
  detail?: string
  startTime: number
  endTime?: number
  expanded?: boolean
}

interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  streaming?: boolean
  thinkingStatus?: string
  error?: boolean
  isSystem?: boolean
  toolCalls: ToolCallInfo[]
  timestamp: number
  thinkingStartTime?: number
  thinkingEndTime?: number
  thinkingSteps: ThinkingStep[]
  thinkingExpanded?: boolean
  feedback?: number
}

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight: function (str: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

const renderMarkdown = (text: string): string => {
  return md.render(text || '')
}

const escapeHtml = (text: string): string => {
  const cleaned = text.replace(/\[图片:\s*([^\]]+)\]\([^)]+\)/g, '[图片: $1]')
  const div = document.createElement('div')
  div.textContent = cleaned
  return div.innerHTML.replace(/\n/g, '<br>')
}

const parseQuote = (content: string): { quote: string; body: string } => {
  if (!content || !content.startsWith('> ')) return { quote: '', body: content }
  const lines = content.split('\n')
  let quoteEnd = 1
  for (let i = 1; i < lines.length; i++) {
    if (lines[i].trim() === '') { quoteEnd = i; break }
    if (lines[i].startsWith('> ')) { quoteEnd = i + 1 } else { quoteEnd = i; break }
  }
  const quoteRaw = lines.slice(0, quoteEnd).join(' ').replace(/^>\s*/, '').replace(/>\s*/g, ' ').trim()
  const rest = lines.slice(quoteEnd).join('\n').replace(/^\n+/, '')
  return { quote: quoteRaw, body: rest }
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

const quoteMessage = (content: string) => {
  const clean = content.replace(/<[^>]*>/g, '').trim()
  quotedContent.value = clean
  if (!visible.value) visible.value = true
  nextTick(() => textareaRef.value?.focus())
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
const expanded = ref(false)
const inputText = ref('')
const loading = ref(false)
const messages = ref<Message[]>([])
const sessionId = ref<string | undefined>(undefined)
const bodyRef = ref<HTMLElement>()
const showScrollBottom = ref(false)
const onBodyScroll = () => {
  if (!bodyRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = bodyRef.value
  showScrollBottom.value = scrollHeight - scrollTop - clientHeight > 120
}
const textareaRef = ref<HTMLTextAreaElement>()
const fileInputRef = ref<HTMLInputElement>()
const pendingImages = ref<{ url: string; name: string; uploadedPath?: string }[]>([])
const quotedContent = ref('')
const aiEnabled = ref(true)
const hasNewBadge = ref(true)
const statusText = ref('在线')
let streamHandle: { close: () => void } | null = null
let msgIdCounter = 0
let toolCallIdCounter = 0
let lastUserMessage = ''
let thinkingTimerId: number | null = null

const btnPos = ref({ x: window.innerWidth - 80, y: window.innerHeight - 80 })
const isDragging = ref(false)
let dragStart = { x: 0, y: 0, bx: 0, by: 0, moved: false }

const toggleExpand = () => {
  expanded.value = !expanded.value
  nextTick(() => scrollToBottom())
}

const autoResize = () => {
  const ta = textareaRef.value
  if (!ta) return
  ta.style.height = 'auto'
  ta.style.height = Math.min(ta.scrollHeight, 120) + 'px'
}

const onDragStart = (e: MouseEvent | TouchEvent) => {
  const point = 'touches' in e ? e.touches[0] : e
  dragStart = { x: point.clientX, y: point.clientY, bx: btnPos.value.x, by: btnPos.value.y, moved: false }
  isDragging.value = true
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('touchend', onDragEnd)
}

const onDragMove = (e: MouseEvent | TouchEvent) => {
  const point = 'touches' in e ? e.touches[0] : e
  const dx = point.clientX - dragStart.x
  const dy = point.clientY - dragStart.y
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) dragStart.moved = true
  const nx = Math.max(16, Math.min(window.innerWidth - 72, dragStart.bx + dx))
  const ny = Math.max(16, Math.min(window.innerHeight - 72, dragStart.by + dy))
  btnPos.value = { x: nx, y: ny }
  if ('touches' in e) e.preventDefault()
}

const onDragEnd = () => {
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('touchend', onDragEnd)
  isDragging.value = false
  if (!dragStart.moved) {
    visible.value = true
    hasNewBadge.value = false
    return
  }
  const centerX = btnPos.value.x + 28
  const clampedY = Math.max(16, Math.min(window.innerHeight - 72, btnPos.value.y))
  if (centerX < window.innerWidth / 2) {
    btnPos.value = { x: 16, y: clampedY }
  } else {
    btnPos.value = { x: window.innerWidth - 72, y: clampedY }
  }
  localStorage.setItem('ai:btnPos', JSON.stringify(btnPos.value))
}

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
  }
}

const sanitizeContentKey = (content: string): string => {
  return content
    .replace(/<.*?DSML.*?>[\s\S]*?<\/.*?DSML.*?>/g, '')
    .replace(/<.*?DSML.*?>/g, '')
    .replace(/<.*?tool_calls.*?>[\s\S]*?<\/.*?tool_calls.*?>/g, '')
    .replace(/<.*?invoke.*?name.*?>[\s\S]*?<\/.*?invoke.*?>/g, '')
    .replace(/<.*?invoke.*?name.*?>/g, '')
    .replace(/<.*?parameter.*?>[\s\S]*?<\/.*?parameter.*?>/g, '')
    .replace(/<.*?parameter.*?>/g, '')
    .trim()
    .slice(-50)
}

const MAX_THINKING_ENTRIES = 50

const saveThinkingData = (sid: string, msg: Message) => {
  if (msg.role !== 'assistant' || !msg.content) return
  try {
    const key = `ai:thinking:${sid}`
    const data = JSON.parse(localStorage.getItem(key) || '{}')
    const contentKey = sanitizeContentKey(msg.content)
    data[contentKey] = {
      thinkingSteps: msg.thinkingSteps,
      thinkingStartTime: msg.thinkingStartTime,
      thinkingEndTime: msg.thinkingEndTime,
      toolCalls: msg.toolCalls.map(tc => ({ ...tc, expanded: false }))
    }
    const keys = Object.keys(data)
    if (keys.length > MAX_THINKING_ENTRIES) {
      for (let i = 0; i < keys.length - MAX_THINKING_ENTRIES; i++) {
        delete data[keys[i]]
      }
    }
    localStorage.setItem(key, JSON.stringify(data))
  } catch {}
}

const restoreThinkingData = (sid: string, msg: Message) => {
  if (msg.role !== 'assistant' || !msg.content) return
  try {
    const key = `ai:thinking:${sid}`
    const data = JSON.parse(localStorage.getItem(key) || '{}')
    const contentKey = sanitizeContentKey(msg.content)
    const saved = data[contentKey]
    if (saved) {
      msg.thinkingSteps = saved.thinkingSteps || []
      msg.thinkingStartTime = saved.thinkingStartTime
      msg.thinkingEndTime = saved.thinkingEndTime
      msg.toolCalls = saved.toolCalls || []
    }
  } catch {}
}

const totalHistoryCount = ref(0)

const applyFeedbackStatus = async () => {
  if (!sessionId.value) return
  try {
    const feedbackMap = await getSessionFeedback(sessionId.value)
    if (feedbackMap) {
      for (const msg of messages.value) {
        if (msg.role === 'assistant' && msg.content) {
          const key = msg.content.substring(0, Math.min(50, msg.content.length))
          if (key in feedbackMap) {
            msg.feedback = feedbackMap[key]
          }
        }
      }
    }
  } catch {}
}

const loadHistory = async () => {
  if (!sessionId.value) return
  try {
    const history = await getSessionHistory(sessionId.value)
    if (history && history.length > 0) {
      totalHistoryCount.value = history.length
      const recent = history.slice(-20)
      messages.value = recent.map(msg => {
        const m: Message = {
          id: ++msgIdCounter,
          role: msg.role as 'user' | 'assistant',
          content: msg.content,
          toolCalls: (msg.toolCalls || []).map((tc: any) => ({ ...tc, expanded: false, status: 'done' as const })),
          timestamp: msg.timestamp || Date.now(),
          thinkingSteps: (msg.thinkingSteps || []).map((ts: any) => ({ ...ts, startTime: ts.startTime || 0, endTime: ts.endTime || 0 }))
        }
        if (m.role === 'assistant' && sessionId.value) {
          restoreThinkingData(sessionId.value, m)
        }
        return m
      })
      if (history.length > 20) {
        messages.value.unshift({ id: ++msgIdCounter, role: 'assistant', content: `已加载最近20条对话（共${history.length}条）`, isSystem: true, toolCalls: [], timestamp: 0, thinkingSteps: [] })
      }
      await applyFeedbackStatus()
      scrollToBottom()
    } else {
      totalHistoryCount.value = 0
    }
  } catch {}
}

const loadAllHistory = async () => {
  if (!sessionId.value) return
  try {
    const history = await getSessionHistory(sessionId.value)
    if (history && history.length > 0) {
      totalHistoryCount.value = history.length
      messages.value = history.map(msg => {
        const m: Message = {
          id: ++msgIdCounter,
          role: msg.role as 'user' | 'assistant',
          content: msg.content,
          toolCalls: (msg.toolCalls || []).map((tc: any) => ({ ...tc, expanded: false, status: 'done' as const })),
          timestamp: msg.timestamp || Date.now(),
          thinkingSteps: (msg.thinkingSteps || []).map((ts: any) => ({ ...ts, startTime: ts.startTime || 0, endTime: ts.endTime || 0 }))
        }
        if (m.role === 'assistant' && sessionId.value) {
          restoreThinkingData(sessionId.value, m)
        }
        return m
      })
      await applyFeedbackStatus()
      scrollToBottom()
    }
  } catch {}
}

const allSuggestions = [
  '怎么发布二手商品？',
  '商品怎么上架？',
  '搜索商品怎么用？',
  '商品审核要多久？',
  '怎么编辑已发布的商品？',
  '商品下架后还能上架吗？',
  '我的订单到哪了？',
  '怎么支付订单？',
  '如何取消订单？',
  '怎么申请退款？',
  '怎么确认收货？',
  '如何给卖家评价？',
  '订单有哪些状态？',
  '忘记密码怎么找回？',
  '怎么修改登录密码？',
  '如何实名认证？',
  '怎么修改个人信息？',
  '怎么绑定邮箱？',
  '查看我的资金流水',
  '我总共消费了多少？',
  '怎么查看收入记录？',
  '平台收手续费吗？',
  '支持哪些支付方式？',
  '怎么设置收货地址？',
  '怎么管理收款账号？',
  '如何收藏商品？',
  '购物车怎么用？',
  '怎么关注其他用户？',
  '怎么查看通知消息？',
  '如何联系卖家？',
  '怎么举报违规商品？',
  '商品审核被拒怎么办？',
  '怎么查看我的商品？',
  '关注后有什么好处？',
  '交易安全怎么保障？',
  '支付宝担保交易是什么？',
  '怎么区分买家和卖家订单？',
  '可以线下自提吗？'
]
const displaySuggestions = ref<string[]>([])
const refreshSuggestions = () => {
  const shuffled = [...allSuggestions].sort(() => Math.random() - 0.5)
  displaySuggestions.value = shuffled.slice(0, 4)
}

let scrollRafId: number | null = null
const scrollToBottom = (smooth = false) => {
  if (smooth && bodyRef.value) {
    bodyRef.value.scrollTo({ top: bodyRef.value.scrollHeight, behavior: 'smooth' })
    return
  }
  if (scrollRafId !== null) return
  scrollRafId = requestAnimationFrame(() => {
    if (bodyRef.value) {
      bodyRef.value.scrollTop = bodyRef.value.scrollHeight
    }
    scrollRafId = null
  })
}

const startThinkingTimer = () => {
  if (thinkingTimerId) clearInterval(thinkingTimerId)
  thinkingTimerId = window.setInterval(() => {
    nextTick()
  }, 100)
}

const stopThinkingTimer = () => {
  if (thinkingTimerId) {
    clearInterval(thinkingTimerId)
    thinkingTimerId = null
  }
}

const startAIStream = async (text: string, assistantMsg: Message, regenerate = false) => {
  if (!aiEnabled.value) {
    assistantMsg.loading = false
    assistantMsg.streaming = false
    assistantMsg.content = 'AI助手暂时不可用，请稍后再试或联系人工客服。'
    assistantMsg.thinkingEndTime = Date.now()
    loading.value = false
    stopThinkingTimer()
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
      assistantMsg.thinkingEndTime = Date.now()
      const lastStep = assistantMsg.thinkingSteps[assistantMsg.thinkingSteps.length - 1]
      if (lastStep && !lastStep.endTime) lastStep.endTime = Date.now()
      if (sessionId.value) saveThinkingData(sessionId.value, assistantMsg)
      loading.value = false
      streamHandle = null
      stopThinkingTimer()
      scrollToBottom()
    }

    const processQueue = () => {
      if (tokenQueue.length > 0) {
        accumulated += tokenQueue.shift()!
        assistantMsg.content = accumulated
        assistantMsg.loading = false
        if (assistantMsg.thinkingStatus) {
          assistantMsg.thinkingStatus = undefined
          const lastStep = assistantMsg.thinkingSteps[assistantMsg.thinkingSteps.length - 1]
          if (lastStep && !lastStep.endTime) lastStep.endTime = Date.now()
        }
        scrollToBottom()
        rafId = requestAnimationFrame(processQueue)
      } else {
        rafId = null
        if (finished) finishStream()
      }
    }

    streamHandle = chatStream(
      text,
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
        assistantMsg.thinkingEndTime = Date.now()
        loading.value = false
        streamHandle = null
        stopThinkingTimer()
      },
      (sid: string) => {
        sessionId.value = sid
        localStorage.setItem(STORAGE_KEY, sid)
      },
      (thinkingData: { step: string; detail?: string }) => {
        if (!assistantMsg.content) {
          assistantMsg.thinkingStatus = thinkingData.step
          assistantMsg.thinkingSteps.push({ status: thinkingData.step, detail: thinkingData.detail, startTime: Date.now() })
          if (assistantMsg.thinkingSteps.length > 1) {
            assistantMsg.thinkingSteps[assistantMsg.thinkingSteps.length - 2].endTime = Date.now()
          }
        }
      },
      (toolCall: { id: string; name: string; args: Record<string, unknown> }) => {
        const tcStartTime = Date.now()
        assistantMsg.toolCalls.push({
          id: ++toolCallIdCounter,
          callId: toolCall.id,
          name: toolCall.name,
          args: toolCall.args,
          result: '',
          expanded: false,
          status: 'pending'
        })
        assistantMsg.thinkingStatus = `正在调用 ${toolDisplayName(toolCall.name)}...`
        assistantMsg.thinkingSteps.push({ status: `调用工具: ${toolDisplayName(toolCall.name)}`, startTime: tcStartTime })
        if (assistantMsg.thinkingSteps.length > 1) {
          assistantMsg.thinkingSteps[assistantMsg.thinkingSteps.length - 2].endTime = tcStartTime
        }
        scrollToBottom()
      },
      (toolResult: { id: string; name: string; result: string }) => {
        const tc = assistantMsg.toolCalls.find(t => t.callId === toolResult.id && t.status !== 'done')
        if (tc) {
          tc.result = toolResult.result
          tc.status = 'done'
          tc.duration = String(Date.now() - (assistantMsg.thinkingSteps[assistantMsg.thinkingSteps.length - 1]?.startTime || Date.now()))
        }
        assistantMsg.thinkingStatus = undefined
        const lastStep = assistantMsg.thinkingSteps[assistantMsg.thinkingSteps.length - 1]
        if (lastStep) lastStep.endTime = Date.now()
        scrollToBottom()
      },
      (toolStart: { id: string; name: string }) => {
        const tc = assistantMsg.toolCalls.find(t => t.callId === toolStart.id && t.status === 'pending')
        if (tc) {
          tc.status = 'running'
        }
        assistantMsg.thinkingStatus = `正在执行 ${toolDisplayName(toolStart.name)}...`
        scrollToBottom()
      },
      (toolError: { id: string; name: string; error: string }) => {
        const tc = assistantMsg.toolCalls.find(t => t.callId === toolError.id && t.status === 'running')
        if (tc) {
          tc.status = 'error'
          tc.result = `执行失败: ${toolError.error}`
        }
        scrollToBottom()
      }
    , regenerate)
  } catch (e) {
    assistantMsg.loading = false
    assistantMsg.streaming = false
    assistantMsg.content = '请求失败，请稍后再试。'
    assistantMsg.error = true
    assistantMsg.thinkingEndTime = Date.now()
    loading.value = false
    stopThinkingTimer()
  }
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
  if (textareaRef.value) textareaRef.value.style.height = 'auto'
  messages.value.push({ id: ++msgIdCounter, role: 'user', content: trimmed, toolCalls: [], timestamp: Date.now(), thinkingSteps: [] })
  const assistantMsg: Message = {
    id: ++msgIdCounter,
    role: 'assistant',
    content: '',
    loading: true,
    streaming: true,
    toolCalls: [],
    timestamp: Date.now(),
    thinkingStartTime: Date.now(),
    thinkingSteps: []
  }
  messages.value.push(assistantMsg)
  loading.value = true
  startThinkingTimer()
  scrollToBottom()

  await startAIStream(trimmed, assistantMsg)
}

const triggerUpload = () => fileInputRef.value?.click()

const handleImageUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  target.value = ''
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过5MB')
    return
  }
  const previewUrl = URL.createObjectURL(file)
  pendingImages.value.push({ url: previewUrl, name: file.name })
  try {
    const formData = new FormData()
    formData.append('file', file)
    const token = localStorage.getItem('token') || ''
    const resp = await fetch('/api/file/upload', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: formData
    })
    const json = await resp.json()
    if (json.code === 200 && json.data) {
      const idx = pendingImages.value.findIndex(p => p.url === previewUrl)
      if (idx >= 0) pendingImages.value[idx] = { url: previewUrl, name: file.name, uploadedPath: json.data }
    }
  } catch {
    ElMessage.warning('图片上传失败')
    URL.revokeObjectURL(previewUrl)
    pendingImages.value = pendingImages.value.filter(p => p.url !== previewUrl)
  }
}

const handlePaste = async (e: ClipboardEvent) => {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (!file) continue
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.warning('图片大小不能超过5MB')
        return
      }
      const previewUrl = URL.createObjectURL(file)
      pendingImages.value.push({ url: previewUrl, name: `粘贴图片_${Date.now()}.png` })
      try {
        const formData = new FormData()
        formData.append('file', file)
        const token = localStorage.getItem('token') || ''
        const resp = await fetch('/api/file/upload', {
          method: 'POST',
          headers: { 'Authorization': `Bearer ${token}` },
          body: formData
        })
        const json = await resp.json()
        if (json.code === 200 && json.data) {
          const idx = pendingImages.value.findIndex(p => p.url === previewUrl)
          if (idx >= 0) pendingImages.value[idx] = { url: previewUrl, name: file.name, uploadedPath: json.data }
        }
      } catch {
        ElMessage.warning('图片上传失败')
        URL.revokeObjectURL(previewUrl)
        pendingImages.value = pendingImages.value.filter(p => p.url !== previewUrl)
      }
      return
    }
  }
}

const handleSend = () => {
  let text = inputText.value
  if (quotedContent.value) {
    const flat = quotedContent.value.replace(/\n/g, ' ').replace(/\s+/g, ' ').trim()
    const truncated = flat.length > 50 ? flat.slice(0, 50) + '...' : flat
    text = `> ${truncated}\n\n` + text
    quotedContent.value = ''
  }
  if (pendingImages.value.length > 0) {
    const imgInfo = pendingImages.value.map(img =>
      img.uploadedPath ? `[图片: ${img.name}](${img.uploadedPath})` : `[图片: ${img.name}]`
    ).join(' ')
    text = imgInfo + ' ' + text
    pendingImages.value.forEach(img => URL.revokeObjectURL(img.url))
    pendingImages.value = []
  }
  sendMessage(text)
}
const handleEnter = (e: KeyboardEvent) => {
  if (e.shiftKey) return
  e.preventDefault()
  if (inputText.value.trim()) handleSend()
}
const handleStop = () => {
  if (streamHandle) {
    streamHandle.close()
    streamHandle = null
  }
  loading.value = false
  stopThinkingTimer()
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg && lastMsg.role === 'assistant') {
    lastMsg.loading = false
    lastMsg.streaming = false
    lastMsg.thinkingStatus = undefined
    lastMsg.thinkingEndTime = Date.now()
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

const regenerateAnswer = (idx: number) => {
  if (loading.value) return
  const userMsg = messages.value[idx - 1]
  if (!userMsg || userMsg.role !== 'user') return
  messages.value.splice(idx)
  lastUserMessage = userMsg.content
  const assistantMsg: Message = {
    id: ++msgIdCounter,
    role: 'assistant',
    content: '',
    loading: true,
    streaming: true,
    toolCalls: [],
    timestamp: Date.now(),
    thinkingStartTime: Date.now(),
    thinkingSteps: []
  }
  messages.value.push(assistantMsg)
  loading.value = true
  startThinkingTimer()
  scrollToBottom()
  startAIStream(userMsg.content, assistantMsg, true)
}

const handleFeedback = async (msg: Message, rating: number, idx: number) => {
  if (msg.feedback === rating) return
  const userMsg = messages.value[idx - 1]
  let feedbackText: string | undefined
  if (rating === -1) {
    try {
      const { value } = await ElMessageBox.prompt('请告诉我们哪里做得不好，我们会努力改进', '反馈建议', {
        confirmButtonText: '提交',
        cancelButtonText: '跳过',
        inputPlaceholder: '请输入您的建议（可选）',
        inputType: 'textarea',
        inputValidator: () => true
      })
      feedbackText = value || undefined
    } catch {
      return
    }
  }
  try {
    await submitAiFeedback({
      sessionId: sessionId.value || '',
      messageId: String(msg.id),
      userMessage: userMsg?.content || '',
      aiResponse: msg.content,
      rating,
      feedback: feedbackText
    })
    msg.feedback = rating
    ElMessage.success({ message: '感谢您的反馈', duration: 1500 })
  } catch {
    ElMessage.warning('反馈提交失败')
  }
}

const handleClear = async () => {
  if (streamHandle) {
    streamHandle.close()
    streamHandle = null
  }
  loading.value = false
  stopThinkingTimer()
  if (sessionId.value) {
    try {
      await clearSession(sessionId.value)
    } catch {
      ElMessage.warning('清空远程对话失败，本地已清空')
    }
  }
  messages.value = []
  localStorage.removeItem(STORAGE_KEY)
  if (sessionId.value) localStorage.removeItem(`ai:thinking:${sessionId.value}`)
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
    refreshSuggestions()
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

const onResize = () => {
  const clampedY = Math.max(16, Math.min(window.innerHeight - 72, btnPos.value.y))
  const centerX = btnPos.value.x + 28
  if (centerX < window.innerWidth / 2) {
    btnPos.value = { x: 16, y: clampedY }
  } else {
    btnPos.value = { x: window.innerWidth - 72, y: clampedY }
  }
  localStorage.setItem('ai:btnPos', JSON.stringify(btnPos.value))
}

onMounted(async () => {
  const savedPos = localStorage.getItem('ai:btnPos')
  if (savedPos) {
    try {
      const p = JSON.parse(savedPos)
      const clampedY = Math.max(16, Math.min(window.innerHeight - 72, p.y))
      const centerX = p.x + 28
      btnPos.value = centerX < window.innerWidth / 2
        ? { x: 16, y: clampedY }
        : { x: window.innerWidth - 72, y: clampedY }
    } catch {}
  }
  window.addEventListener('resize', onResize)
  try {
    const status = await getAiStatus()
    aiEnabled.value = status.enabled && status.healthy
    statusText.value = (status.enabled && status.healthy) ? `在线 · ${status.model}` : '离线'
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
  stopThinkingTimer()
  window.removeEventListener('resize', onResize)
  pendingImages.value.forEach(img => URL.revokeObjectURL(img.url))
  pendingImages.value = []
})
</script>

<style scoped lang="scss">
.ai-consultant {
  position: fixed;
  z-index: 200;
}

.float-btn {
  position: fixed;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.35), inset 0 0 0 2px rgba(255, 255, 255, 0.15);
  transition: transform 0.2s, box-shadow 0.2s;
  user-select: none;
  animation: ai-pulse 3s ease-in-out infinite;
  &:hover { transform: scale(1.08); box-shadow: 0 6px 28px rgba(14, 165, 233, 0.45), inset 0 0 0 2px rgba(255, 255, 255, 0.25); animation: none; }
  &:active { cursor: grabbing; transform: scale(0.95); }
  &.snapping { transition: transform 0.2s, box-shadow 0.2s, left 0.3s cubic-bezier(0.4, 0, 0.2, 1), top 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
}

@keyframes ai-pulse {
  0%, 100% { box-shadow: 0 4px 20px rgba(14, 165, 233, 0.35), inset 0 0 0 2px rgba(255, 255, 255, 0.15); }
  50% { box-shadow: 0 4px 28px rgba(14, 165, 233, 0.5), inset 0 0 0 2px rgba(255, 255, 255, 0.2); }
}

.ai-fab-text {
  font-size: 24px;
  font-weight: 900;
  letter-spacing: -0.5px;
  color: #fff;
  text-shadow:
    0 0 6px rgba(255, 255, 255, 0.8),
    0 0 14px rgba(14, 165, 233, 0.6),
    0 2px 4px rgba(0, 0, 0, 0.35);
  font-family: 'Inter', -apple-system, sans-serif;
  line-height: 1;
  user-select: none;
}

.chat-panel {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 430px;
  height: 640px;
  max-height: calc(100vh - 120px);
  background: var(--bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  border-radius: 20px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &.expanded {
    bottom: 0;
    right: 0;
    top: 64px;
    width: 30vw;
    min-width: 430px;
    max-width: 680px;
    height: auto;
    max-height: none;
    border-radius: 0;
    box-shadow: -8px 0 32px rgba(0, 0, 0, 0.12);
    border: 1px solid var(--border);
    border-right: none;
  }
}

.chat-header {
  padding: 14px 18px;
  background: var(--primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;

  .header-info { display: flex; align-items: center; gap: 10px; }
  .avatar {
    width: 38px; height: 38px; border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    display: flex; align-items: center; justify-content: center;
    backdrop-filter: blur(4px);
  }
  .header-text { display: flex; flex-direction: column; .title { font-size: 15px; font-weight: 700; } .subtitle { font-size: 12px; opacity: 0.85; } }
  .header-actions {
    display: flex; gap: 4px;
    :deep(.el-button) {
      color: #fff; background: rgba(255, 255, 255, 0.15); border: none;
      &:hover { background: rgba(255, 255, 255, 0.3); }
    }
  }
}

.chat-body {
  flex: 1; overflow-y: auto; overflow-x: hidden; padding: 16px;
  display: flex; flex-direction: column; gap: 12px;
  background: linear-gradient(180deg, var(--bg-card), var(--bg-hover));
}

.welcome {
  text-align: center; padding: 28px 0;
  .welcome-icon { color: var(--primary); margin-bottom: 12px; }
  .welcome-title { font-size: 20px; font-weight: 800; margin: 0 0 4px; background: var(--primary-gradient-gloss); background-size: 200% auto; -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
  .welcome-desc { font-size: 13px; color: var(--text-secondary); margin: 0 0 18px; }
  .suggestions { margin-top: 4px; }
  .suggestions-label { font-size: 12px; color: var(--text-muted); margin: 0 0 10px; font-weight: 500; }
  .suggestions-grid { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
  .suggestion-item {
    display: inline-flex; align-items: center;
    padding: 6px 14px; border: 1px solid var(--border);
    border-radius: 16px; background: var(--bg-card);
    cursor: pointer; transition: all 0.2s;
    .suggestion-text { font-size: 13px; color: var(--text-primary); white-space: nowrap; }
    &:hover { border-color: var(--primary); background: rgba(14, 165, 233, 0.05); transform: translateY(-1px); box-shadow: 0 2px 8px rgba(14, 165, 233, 0.1); }
    &:active { transform: translateY(0); }
  }
}

.msg-row { display: flex; &.user { justify-content: flex-end; } &.assistant { justify-content: flex-start; } }
.load-more-bar { text-align: center; padding: 4px 0; margin-bottom: 4px; }
.msg-wrapper:has(.msg-content:empty) { display: none; }

.msg-wrapper { display: flex; flex-direction: column; max-width: 85%; min-width: 0; .user & { align-items: flex-end; } .assistant & { align-items: flex-start; } }

.msg-bubble {
  padding: 10px 14px; border-radius: 14px;
  font-size: 14px; line-height: 1.6; word-break: break-word;
  max-width: 100%; overflow: hidden;
  .user & { background: var(--primary-gradient); color: #fff; border-bottom-right-radius: 4px; box-shadow: 0 2px 8px rgba(14, 165, 233, 0.25); }
  .assistant & { background: var(--bg-card); color: var(--text-primary); border-bottom-left-radius: 4px; box-shadow: var(--shadow-sm); border: 1px solid var(--border-light); }
}

.msg-quote-display {
  display: flex; align-items: center; gap: 4px;
  padding: 4px 8px 4px 10px; margin-bottom: 6px;
  background: rgba(255, 255, 255, 0.15);
  border-left: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 6px;
  .quote-mark { font-size: 16px; font-weight: 700; opacity: 0.7; flex-shrink: 0; line-height: 1; }
  .quote-text-display { font-size: 12px; opacity: 0.8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4; }
}

.msg-content {
  max-width: 100%; overflow-x: auto;
  :deep(p) { margin: 0 0 8px; &:last-child { margin-bottom: 0; } }
  :deep(ul), :deep(ol) { margin: 4px 0 8px; padding-left: 20px; }
  :deep(li) { margin: 2px 0; }
  :deep(code) { background: rgba(0, 0, 0, 0.06); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
  :deep(pre) { background: rgba(0, 0, 0, 0.06); padding: 8px 12px; border-radius: 8px; overflow-x: auto; margin: 4px 0; code { background: none; padding: 0; } }
  :deep(table) { border-collapse: collapse; margin: 4px 0; th, td { border: 1px solid var(--border); padding: 4px 8px; } th { background: var(--bg-hover); } }
  :deep(strong) { font-weight: 700; }
  :deep(a) { color: var(--primary); text-decoration: none; &:hover { text-decoration: underline; } }
}

.ai-header-text {
  font-size: 16px;
  font-weight: 900;
  letter-spacing: -0.5px;
  color: #fff;
  text-shadow: 0 0 4px rgba(255, 255, 255, 0.8), 0 0 10px rgba(14, 165, 233, 0.6), 0 1px 3px rgba(0, 0, 0, 0.35);
  font-family: 'Inter', -apple-system, sans-serif;
  line-height: 1;
}

.response-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 6px; margin-top: 8px;
  border-top: 1px solid var(--border-light);
  font-size: 12px;
  .footer-left { display: flex; align-items: center; gap: 4px;
    .check-icon { color: #22c55e; }
    .footer-status { color: var(--primary); font-weight: 600; }
  }
  .footer-right { display: flex; align-items: center; gap: 4px; opacity: 0.6;
    .footer-time { font-size: 11px; color: var(--text-secondary); }
  }
}

.thinking-process {
  margin-bottom: 8px; font-size: 12px;
  background: rgba(14, 165, 233, 0.06);
  border-radius: 6px; overflow: hidden;
  .process-header {
    display: flex; align-items: center; gap: 6px; padding: 6px 10px; cursor: pointer;
    &:hover { background: rgba(14, 165, 233, 0.08); }
    .process-check { color: #22c55e; flex-shrink: 0; }
    .is-loading { color: var(--primary); flex-shrink: 0; animation: rotating 1.5s linear infinite; }
    .process-title { font-weight: 600; color: var(--primary); }
    .process-time { font-size: 10px; color: var(--text-muted); margin-left: auto; font-variant-numeric: tabular-nums; }
    .process-expand-icon { color: var(--text-muted); }
  }
  .process-body { padding: 4px 10px 8px; }
  .process-step {
    display: flex; gap: 8px;
    .step-marker { display: flex; flex-direction: column; align-items: center; flex-shrink: 0; padding-top: 2px;
      .step-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); flex-shrink: 0; }
      .step-line { width: 2px; flex: 1; min-height: 16px; background: rgba(14, 165, 233, 0.2); margin: 2px 0; }
    }
    .step-content { flex: 1; min-width: 0; padding-bottom: 8px;
      .step-title { color: var(--text-secondary); font-weight: 500; }
      .step-detail { margin-top: 2px; padding: 4px 8px; background: rgba(14, 165, 233, 0.06); border-left: 2px solid var(--primary); border-radius: 0 4px 4px 0; font-size: 11px; color: var(--text-regular); white-space: pre-wrap; line-height: 1.6; }
    }
  }
}

.tool-calls { margin-bottom: 8px; }
.tool-call-card { background: rgba(14, 165, 233, 0.06); border-radius: 6px; margin-bottom: 4px; overflow: hidden; font-size: 12px; }
.tool-call-header { display: flex; align-items: center; gap: 6px; padding: 6px 10px; cursor: pointer; color: var(--text-secondary); .tool-name { font-weight: 600; color: var(--primary); } .tool-duration { font-size: 10px; color: var(--text-muted); margin-left: auto; } .tool-status { font-size: 10px; margin-left: auto; padding: 1px 6px; border-radius: 8px; } .tool-status-pending { color: #9ca3af; background: rgba(156,163,175,0.1); } .tool-status-running { color: #f59e0b; background: rgba(245,158,11,0.1); animation: pulse 1.5s infinite; } .tool-status-error { color: #ef4444; background: rgba(239,68,68,0.1); } .expand-icon { color: var(--text-muted); } &:hover { background: rgba(14, 165, 233, 0.08); } }
.tool-call-body { padding: 6px 10px; border-top: 1px solid var(--border-light); }
.tool-args, .tool-result { margin: 4px 0; pre { background: var(--bg-hover); padding: 6px 8px; border-radius: 4px; overflow-x: auto; margin: 4px 0; font-size: 11px; white-space: pre-wrap; max-height: 120px; overflow-y: auto; } }

@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.msg-error { display: flex; align-items: center; gap: 8px; color: var(--danger); }
.msg-footer { display: flex; align-items: center; gap: 8px; margin-top: 4px; padding: 0 4px; opacity: 0.55; }
.msg-time { font-size: 11px; color: var(--text-secondary); }
.msg-actions { display: flex; align-items: center; gap: 2px; }
.action-btn {
  display: flex; align-items: center; justify-content: center;
  width: 22px; height: 22px; padding: 0;
  border: none; outline: none; background: transparent;
  color: var(--text-muted); cursor: pointer; border-radius: 4px;
  transition: color 0.15s, background 0.15s;
  &:hover { color: var(--primary); background: rgba(14, 165, 233, 0.08); }
  &:active { transform: scale(0.92); }
  &.active { color: var(--primary); background: rgba(14, 165, 233, 0.12); }
}
.typing { display: inline-flex; gap: 4px; align-items: center; .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--text-secondary); animation: typing-bounce 1.4s infinite ease-in-out; &:nth-child(2) { animation-delay: 0.2s; } &:nth-child(3) { animation-delay: 0.4s; } } }

.scroll-bottom-btn {
  position: absolute; bottom: 80px; left: 50%; transform: translateX(-50%);
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--primary-gradient); color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 10;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.35);
  transition: all 0.3s ease;
  &:hover { transform: translateX(-50%) translateY(-3px); box-shadow: 0 6px 24px rgba(14, 165, 233, 0.45); }
}
.fade-scale-enter-active { transition: all 0.3s ease; }
.fade-scale-leave-active { transition: all 0.2s ease; }
.fade-scale-enter-from, .fade-scale-leave-to { opacity: 0; transform: translateX(-50%) translateY(20px); }
@keyframes typing-bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-6px); } }

.chat-footer { padding: 0 12px 14px; flex-shrink: 0; background: var(--bg-hover); }

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 22px;
  padding: 6px 6px 6px 14px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: border-color 0.25s, box-shadow 0.25s;
  &.disabled { opacity: 0.6; }
  &:focus-within {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.08), inset 0 1px 2px rgba(0, 0, 0, 0.02);
  }
}
.chat-textarea {
  flex: 1;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  line-height: 22px;
  font-family: inherit;
  max-height: 120px;
  min-height: 34px;
  padding: 4px 0;
  &::placeholder { color: var(--text-muted); opacity: 0.7; }
}
.upload-btn { flex-shrink: 0; width: 34px; height: 34px; border: none; background: transparent; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.2s; &:hover { color: var(--primary); background: rgba(14,165,233,0.06); transform: scale(1.05); } &:disabled { opacity: 0.4; cursor: not-allowed; } }
.image-preview-bar { display: flex; gap: 8px; padding: 6px 12px; flex-wrap: wrap; .image-preview-item { position: relative; width: 60px; height: 60px; border-radius: 6px; overflow: hidden; border: 1px solid var(--border-light); img { width: 100%; height: 100%; object-fit: cover; } .remove-img { position: absolute; top: 2px; right: 2px; width: 16px; height: 16px; border: none; border-radius: 50%; background: rgba(0,0,0,0.5); color: white; cursor: pointer; font-size: 12px; line-height: 1; display: flex; align-items: center; justify-content: center; } } }
.quote-bar { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; padding: 6px 12px 6px 14px; background: rgba(14, 165, 233, 0.06); border-left: 3px solid var(--primary); border-radius: 8px; .quote-icon { color: var(--primary); font-size: 18px; font-weight: 700; flex-shrink: 0; line-height: 1; } .quote-text { flex: 1; font-size: 13px; color: var(--text-secondary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; line-height: 1.4; } .quote-close { flex-shrink: 0; width: 20px; height: 20px; border: none; background: transparent; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.15s; &:hover { color: var(--danger); background: rgba(239, 68, 68, 0.08); } } }
.send-btn {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: var(--primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s, opacity 0.15s, box-shadow 0.15s;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.25);
  &:hover:not(:disabled) { transform: scale(1.06) translateY(-1px); box-shadow: 0 4px 14px rgba(14, 165, 233, 0.35); }
  &:active:not(:disabled) { transform: scale(0.94); }
  &:disabled { opacity: 0.35; cursor: not-allowed; box-shadow: none; }
  &.stop-btn { background: var(--danger); box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3); }
}
.input-hint { text-align: center; font-size: 11px; color: var(--text-muted); opacity: 0.5; margin-top: 6px; letter-spacing: 0.3px; }

.slide-up-enter-active, .slide-up-leave-active { transition: opacity 0.3s, transform 0.3s; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(20px); }

@media (max-width: 768px) {
  .chat-panel {
    width: calc(100vw - 32px);
    max-width: 500px;
    left: 50%; right: auto; transform: translateX(-50%);
    &.expanded { width: 100vw; max-width: none; left: 0; transform: none; }
  }
  .chat-header .header-text .title { font-size: 14px; }
  .msg-bubble { font-size: 13px; }
  .input-hint { display: none; }
}

@media (max-width: 480px) {
  .chat-panel {
    width: calc(100vw - 32px); height: calc(100vh - 120px); bottom: 16px;
    left: 50%; right: auto; transform: translateX(-50%);
    &.expanded { width: 100vw; min-width: 0; max-width: none; left: 0; transform: none; border-radius: 0; }
  }
}
</style>

<style lang="scss">
.hljs {
  display: block;
  overflow-x: auto;
  padding: 0.5em;
  background: #f6f8fa;
  border-radius: 6px;
  color: #24292e;
}
.hljs-comment, .hljs-quote { color: #6a737d; }
.hljs-keyword, .hljs-selector-tag, .hljs-literal, .hljs-section, .hljs-link { color: #d73a49; }
.hljs-function .hljs-title, .hljs-title.class_, .hljs-title.function_ { color: #6f42c1; }
.hljs-string, .hljs-regexp, .hljs-addition, .hljs-attribute, .hljs-variable { color: #032f62; }
.hljs-number, .hljs-symbol, .hljs-bullet, .hljs-meta, .hljs-deletion { color: #005cc5; }
.hljs-type, .hljs-built_in, .hljs-name, .hljs-params { color: #22863a; }
.hljs-attr, .hljs-attribute .hljs-string { color: #032f62; }
.hljs-tag { color: #22863a; }
.hljs-emphasis { font-style: italic; }
.hljs-strong { font-weight: bold; }
</style>

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

          <div v-for="(msg, i) in messages" :key="i" :class="['msg-row', msg.role]">
            <div class="msg-bubble">
              <span v-if="msg.role === 'assistant' && msg.loading" class="typing">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </span>
              <span v-else>{{ msg.content }}</span>
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
                :icon="Promotion"
                :loading="loading"
                @click="handleSend"
                :disabled="!inputText.trim()"
              />
            </template>
          </el-input>
        </div>
      </div>
    </transition>

    <div v-if="!visible" class="float-btn" @click="visible = true">
      <el-badge :is-dot="hasNewBadge" type="primary">
        <el-icon :size="28"><ChatDotRound /></el-icon>
      </el-badge>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import { ChatDotRound, Close, Delete, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { chatStream, getAiStatus, clearSession } from '@/api/ai'

interface Message {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
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

  inputText.value = ''
  messages.value.push({ role: 'user', content: trimmed })
  messages.value.push({ role: 'assistant', content: '', loading: true })
  loading.value = true
  scrollToBottom()

  if (!aiEnabled.value) {
    const fallbackMsg = messages.value[messages.value.length - 1]
    fallbackMsg.loading = false
    fallbackMsg.content = 'AI助手暂时不可用，请稍后再试或联系人工客服。'
    loading.value = false
    return
  }

  try {
    const assistantIndex = messages.value.length - 1
    let accumulated = ''

    streamHandle = chatStream(
      trimmed,
      sessionId.value,
      (token: string) => {
        accumulated += token
        messages.value[assistantIndex].content = accumulated
        messages.value[assistantIndex].loading = false
        scrollToBottom()
      },
      () => {
        messages.value[assistantIndex].loading = false
        loading.value = false
        streamHandle = null
        scrollToBottom()
      },
      (error: string) => {
        messages.value[assistantIndex].loading = false
        messages.value[assistantIndex].content = error || 'AI服务暂时不可用，请稍后再试。'
        loading.value = false
        streamHandle = null
      }
    )
  } catch (e) {
    const fallbackMsg = messages.value[messages.value.length - 1]
    fallbackMsg.loading = false
    fallbackMsg.content = '请求失败，请稍后再试。'
    loading.value = false
  }
}

const handleSend = () => sendMessage(inputText.value)
const handleEnter = () => {
  if (inputText.value.trim()) handleSend()
}

const handleClear = async () => {
  if (sessionId.value) {
    try {
      await clearSession(sessionId.value)
    } catch {}
  }
  messages.value = []
  sessionId.value = undefined
  ElMessage.success('对话已清空')
}

onMounted(async () => {
  try {
    const status = await getAiStatus()
    aiEnabled.value = status.enabled
    statusText.value = status.enabled ? `在线 · ${status.model}` : '离线'
  } catch {
    aiEnabled.value = false
    statusText.value = '离线'
  }
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
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;

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
<template>
  <div class="chat-input-area">
    <Transition name="plus-slide">
      <div v-if="showPlusPanel" class="plus-panel">
      <div class="plus-item" @click="handleImageClick">
        <div class="plus-icon"><el-icon><Picture /></el-icon></div>
        <span>图片</span>
      </div>
      <div class="plus-item" @click="emit('sendOrder')">
        <div class="plus-icon"><el-icon><List /></el-icon></div>
        <span>订单</span>
      </div>
      <div class="plus-item" @click="emit('sendGoods')">
        <div class="plus-icon"><el-icon><Goods /></el-icon></div>
        <span>商品</span>
      </div>
      </div>
    </Transition>
    <div class="chat-input">
      <el-button size="large" round @click="showPlusPanel = !showPlusPanel" :type="showPlusPanel ? 'primary' : 'default'"><el-icon><Plus /></el-icon></el-button>
      <el-input v-model="inputText" placeholder="输入消息..." @keyup.enter="handleSend" @input="handleInput" size="large" @focus="showPlusPanel = false" />
      <el-button type="primary" size="large" @click="handleSend" :disabled="!inputText.trim() || disabled" round>发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  currentTarget: number | null
  disabled: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
  sendGoods: []
  sendOrder: []
  sendImage: [file: File]
  typing: [value: string]
}>()

const inputText = ref('')
const showPlusPanel = ref(false)

const handleSend = () => {
  if (!inputText.value.trim()) return
  emit('send', inputText.value.trim())
  inputText.value = ''
  showPlusPanel.value = false
  emit('typing', '')
}

const handleInput = () => {
  emit('typing', inputText.value)
}

const handleImageClick = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/jpeg,image/png,image/gif,image/webp'
  input.onchange = (e: Event) => {
    const file = (e.target as HTMLInputElement).files?.[0]
    if (file) {
      emit('sendImage', file)
    }
  }
  input.click()
  showPlusPanel.value = false
}
</script>

<style scoped lang="scss">
.chat-input-area { border-top: 1px solid var(--border); }
.plus-panel {
  display: flex; gap: 16px; padding: 16px 20px; background: var(--bg-hover); border-bottom: 1px solid var(--border);
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

.plus-slide-enter-active, .plus-slide-leave-active { transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
.plus-slide-enter-from, .plus-slide-leave-to { opacity: 0; transform: translateY(8px); }
</style>
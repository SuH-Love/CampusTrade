<template>
  <transition name="backtop-fade">
    <div v-show="visible" class="back-to-top" @click="scrollToTop">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="18 15 12 9 6 15"></polyline>
      </svg>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const visible = ref(false)
const threshold = 300

const handleScroll = () => {
  visible.value = window.scrollY > threshold
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => window.addEventListener('scroll', handleScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<style scoped lang="scss">
.back-to-top {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--primary-gradient);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(14, 165, 233, 0.35);
  transition: var(--transition);
  z-index: 999;
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 24px rgba(14, 165, 233, 0.45);
  }
}

.backtop-fade-enter-active { transition: all 0.3s ease; }
.backtop-fade-leave-active { transition: all 0.2s ease; }
.backtop-fade-enter-from, .backtop-fade-leave-to { opacity: 0; transform: translateY(10px); }
</style>
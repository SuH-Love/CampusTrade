<template>
  <div class="not-found">
    <div class="not-found-content">
      <div class="not-found-code">404</div>
      <div class="not-found-illustration">
        <div class="lost-item">🔍</div>
        <div class="trail">
          <span class="dot" v-for="i in 5" :key="i" :style="{ animationDelay: i * 0.15 + 's' }"></span>
        </div>
        <div class="lost-item">📄</div>
      </div>
      <h2 class="not-found-subtitle">哎呀，页面走丢了</h2>
      <p class="not-found-desc">你访问的页面不存在或已被移除，别担心，让我们帮你找到方向</p>
      <div class="search-box">
        <el-input v-model="searchKeyword" placeholder="搜索商品或用户..." size="large" clearable prefix-icon="Search" @keyup.enter="handleSearch" />
      </div>
      <div class="quick-links">
        <el-button type="primary" round @click="$router.push('/')">返回首页</el-button>
        <el-button round @click="$router.push('/goods')">商品列表</el-button>
        <el-button round @click="$router.push('/order')">我的订单</el-button>
      </div>
      <div class="countdown-text">{{ countdown }}秒后自动返回首页</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchKeyword = ref('')
const countdown = ref(5)
let timer: number | null = null

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/goods', query: { keyword: searchKeyword.value.trim() } })
  }
}

onMounted(() => {
  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (timer) clearInterval(timer)
      router.push('/')
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.not-found {
  display: flex; align-items: center; justify-content: center; min-height: 80vh;
  padding: 40px 20px;
}
.not-found-content { text-align: center; max-width: 480px; }
.not-found-code {
  font-size: 140px; font-weight: 900; line-height: 1;
  background: linear-gradient(135deg, var(--primary, #6366f1), #a78bfa, #f472b6);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text; opacity: 0.6;
  animation: float 3s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}
.not-found-illustration {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  margin: 20px 0;
}
.lost-item { font-size: 40px; animation: bob 2s ease-in-out infinite; }
.trail { display: flex; gap: 6px; }
.dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--primary, #6366f1); opacity: 0.4;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes bob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
@keyframes pulse {
  0%, 100% { opacity: 0.2; transform: scale(0.8); }
  50% { opacity: 0.6; transform: scale(1.2); }
}
.not-found-subtitle {
  font-size: 24px; font-weight: 700; color: var(--text-primary, #1e293b);
  margin: 0 0 8px;
}
.not-found-desc {
  font-size: 15px; color: var(--text-muted, #94a3b8); margin: 0 0 28px; line-height: 1.6;
}
.search-box { margin-bottom: 24px; }
.quick-links { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; margin-bottom: 20px; }
.countdown-text { font-size: 13px; color: var(--text-muted, #94a3b8); }

@media (max-width: 576px) {
  .not-found-code { font-size: 100px; }
  .not-found-subtitle { font-size: 20px; }
  .quick-links { flex-direction: column; align-items: center; }
}
</style>

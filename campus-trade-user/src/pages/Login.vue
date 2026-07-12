<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="auth-brand">
        <div class="brand-icon">C</div>
        <h1>CampusTrade</h1>
        <p>校园二手交易平台</p>
      </div>
      <div class="auth-features">
        <div class="feature-item" v-for="f in features" :key="f.title">
          <div class="feature-icon">{{ f.icon }}</div>
          <div><div class="feature-title">{{ f.title }}</div><div class="feature-desc">{{ f.desc }}</div></div>
        </div>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-form-wrap">
        <h2>欢迎回来</h2>
        <p class="auth-subtitle">登录你的账号，开始交易</p>
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password autocomplete="current-password" />
          </el-form-item>
          <el-form-item>
            <div style="display: flex; justify-content: space-between; width: 100%; align-items: center">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <router-link to="/forgot-password" style="font-size: 13px">忘记密码？</router-link>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" native-type="submit" round>登录</el-button>
          </el-form-item>
        </el-form>
        <div class="auth-footer">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const features = [
  { icon: '🔒', title: '安全交易', desc: '实名认证保障买卖安全' },
  { icon: '💬', title: '即时沟通', desc: '买卖双方在线聊天' },
  { icon: '✅', title: '品质保障', desc: '商品审核确保质量' }
]

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const savedUsername = localStorage.getItem('remembered_username')
if (savedUsername) {
  form.username = savedUsername
  rememberMe.value = true
}

const handleLogin = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    if (rememberMe.value) localStorage.setItem('remembered_username', form.username)
    else localStorage.removeItem('remembered_username')
    await userStore.login(form)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e) { console.error(e) } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.auth-page { display: flex; min-height: 100vh; }

.auth-left {
  flex: 1;
  background: linear-gradient(135deg, #6366f1, #8b5cf6, #a78bfa);
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
  position: relative;
  overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    width: 400px; height: 400px;
    background: rgba(255,255,255,0.08);
    border-radius: 50%;
    top: -100px; right: -100px;
  }
  &::after {
    content: '';
    position: absolute;
    width: 300px; height: 300px;
    background: rgba(255,255,255,0.05);
    border-radius: 50%;
    bottom: -80px; left: -60px;
  }
  @media (max-width: 768px) { display: none; }
}

.brand-icon {
  width: 56px; height: 56px;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 800; margin-bottom: 20px;
}

.auth-brand h1 { font-size: 36px; font-weight: 800; margin-bottom: 8px; letter-spacing: -0.5px; }
.auth-brand p { font-size: 16px; opacity: 0.85; }

.auth-features { margin-top: 48px; display: flex; flex-direction: column; gap: 24px; }
.feature-item { display: flex; gap: 14px; align-items: flex-start; }
.feature-icon { font-size: 24px; flex-shrink: 0; margin-top: 2px; }
.feature-title { font-size: 15px; font-weight: 600; }
.feature-desc { font-size: 13px; opacity: 0.75; margin-top: 2px; }

.auth-right {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  padding: 40px;
  background: var(--bg-page);
}

.auth-form-wrap {
  width: 100%; max-width: 400px;
  h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.3px; }
}

.auth-subtitle { color: var(--text-secondary); margin-bottom: 32px; font-size: 15px; }
.auth-footer { text-align: center; margin-top: 16px; color: var(--text-secondary); font-size: 14px; }
</style>

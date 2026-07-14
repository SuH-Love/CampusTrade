<template>
  <AuthLayout subtitle="校园贸易平台" :features="features">
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
        <div class="flex-between w-full">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <router-link to="/forgot-password" class="text-sm">忘记密码？</router-link>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" native-type="submit" round>登录</el-button>
      </el-form-item>
    </el-form>
    <div class="auth-footer">
      还没有账号？<router-link to="/register">立即注册</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'

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
h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.3px; }
.auth-subtitle { color: var(--text-secondary); margin-bottom: 32px; font-size: 15px; }
.auth-footer { text-align: center; margin-top: 16px; color: var(--text-secondary); font-size: 14px; }
</style>

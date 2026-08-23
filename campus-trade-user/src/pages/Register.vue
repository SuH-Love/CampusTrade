<template>
  <AuthLayout subtitle="校园贸易平台" :features="features">
    <h2>创建账号</h2>
    <p class="auth-subtitle">注册后即可发布和购买商品</p>
    <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister" size="large">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名（3-50位）" prefix-icon="User" autocomplete="username" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码（8-50位，含大小写/数字/特殊字符三种）" prefix-icon="Lock" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号（选填）" autocomplete="tel" maxlength="11" />
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="邮箱（用于重置密码）" autocomplete="email" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" native-type="submit" round>注册</el-button>
      </el-form-item>
    </el-form>
    <div class="auth-footer">
      已有账号？<router-link to="/login">去登录</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const features = [
  { icon: '🔒', title: '安全交易', desc: '实名认证保障买卖安全' },
  { icon: '💬', title: '即时沟通', desc: '买卖双方在线聊天' },
  { icon: '✅', title: '品质保障', desc: '商品审核确保质量' }
]

const form = reactive({ username: '', password: '', confirmPassword: '', phone: '', email: '' })

const confirmPwdValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const strongPwdValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (!value) { callback(); return }
  let types = 0
  if (/[a-z]/.test(value)) types++
  if (/[A-Z]/.test(value)) types++
  if (/\d/.test(value)) types++
  if (/[^a-zA-Z\d]/.test(value)) types++
  if (types < 3) callback(new Error('需包含大小写字母、数字、特殊字符中的三种'))
  else callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 50, message: '密码长度8-50位', trigger: 'blur' },
    { validator: strongPwdValidator, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: confirmPwdValidator, trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) { console.error(e) } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.3px; }
.auth-subtitle { color: var(--text-secondary); margin-bottom: 32px; font-size: 15px; }
.auth-footer { text-align: center; margin-top: 16px; color: var(--text-secondary); font-size: 14px; }
</style>

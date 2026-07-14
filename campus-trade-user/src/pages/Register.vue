<template>
  <AuthLayout subtitle="校园贸易平台" :features="features">
    <h2>创建账号</h2>
    <p class="auth-subtitle">注册后即可发布和购买商品</p>
    <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister" size="large">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" autocomplete="username" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="form.password" type="password" placeholder="密码（至少8位）" prefix-icon="Lock" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="手机号（选填）" autocomplete="tel" />
      </el-form-item>
      <el-form-item prop="email">
        <el-input v-model="form.email" placeholder="邮箱（选填）" autocomplete="email" />
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

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 8, message: '密码至少8位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: confirmPwdValidator, trigger: 'blur' }]
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

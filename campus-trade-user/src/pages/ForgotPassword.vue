<template>
  <AuthLayout subtitle="校园二手交易平台">
    <h2>重置密码</h2>
    <p class="auth-subtitle">通过用户名和注册手机号验证身份</p>
    <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleSubmit" size="large">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="注册时绑定的手机号" prefix-icon="Phone" />
      </el-form-item>
      <el-form-item prop="newPassword">
        <el-input v-model="form.newPassword" type="password" placeholder="新密码（6-20位）" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" native-type="submit" round>重置密码</el-button>
      </el-form-item>
    </el-form>
    <div class="auth-footer">
      <router-link to="/login">返回登录</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'

const router = useRouter()
const loading = ref(false)
const formRef = ref<{ validate: () => Promise<void> } | null>(null)

const form = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await request.post('/auth/reset-password', null, {
      params: { username: form.username, phone: form.phone, newPassword: form.newPassword }
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; }
.auth-subtitle { color: var(--text-secondary); margin-bottom: 32px; font-size: 15px; }
.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-secondary);
  a { color: var(--primary); text-decoration: none; font-weight: 600; &:hover { text-decoration: underline; } }
}
</style>

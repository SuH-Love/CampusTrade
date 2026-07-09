<template>
  <div class="auth-page">
    <div class="auth-left">
      <div class="auth-brand">
        <div class="brand-icon">C</div>
        <h1>CampusTrade</h1>
        <p>校园二手交易平台</p>
      </div>
    </div>
    <div class="auth-right">
      <div class="auth-form-wrap">
        <h2>创建账号</h2>
        <p class="auth-subtitle">注册后即可发布和购买商品</p>
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码（至少8位）" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="phone">
            <el-input v-model="form.phone" placeholder="手机号（选填）" />
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="邮箱（选填）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" style="width: 100%" :loading="loading" native-type="submit" round>注册</el-button>
          </el-form-item>
        </el-form>
        <div class="auth-footer">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '', phone: '', email: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 8, message: '密码至少8位', trigger: 'blur' }]
}

const handleRegister = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) { /* ignore */ } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.auth-page { display: flex; min-height: 100vh; }
.auth-left {
  flex: 1; background: linear-gradient(135deg, #6366f1, #8b5cf6, #a78bfa);
  padding: 60px 48px; display: flex; flex-direction: column; justify-content: center; color: #fff;
  position: relative; overflow: hidden;
  &::before {
    content: '';
    position: absolute;
    width: 400px; height: 400px;
    background: rgba(255,255,255,0.08);
    border-radius: 50%;
    top: -100px; right: -100px;
  }
  @media (max-width: 768px) { display: none; }
}
.brand-icon {
  width: 56px; height: 56px; background: rgba(255,255,255,0.2); backdrop-filter: blur(8px); border-radius: 16px;
  display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 800; margin-bottom: 20px;
}
.auth-brand h1 { font-size: 36px; font-weight: 800; margin-bottom: 8px; letter-spacing: -0.5px; }
.auth-brand p { font-size: 16px; opacity: 0.85; }
.auth-right { flex: 1; display: flex; align-items: center; justify-content: center; padding: 40px; background: var(--bg-page); }
.auth-form-wrap { width: 100%; max-width: 400px; h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; letter-spacing: -0.3px; } }
.auth-subtitle { color: var(--text-secondary); margin-bottom: 32px; font-size: 15px; }
.auth-footer { text-align: center; margin-top: 16px; color: var(--text-secondary); font-size: 14px; }
</style>

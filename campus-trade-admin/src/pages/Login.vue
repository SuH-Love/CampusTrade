<template>
  <div class="admin-login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">C</div>
        <h2>CampusTrade 管理后台</h2>
        <p>请使用管理员账号登录</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="管理员账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="w-full" native-type="submit" :loading="loading" round>登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/stores/admin'
import { ElMessage } from 'element-plus'


const router = useRouter()
const adminStore = useAdminStore()
const formRef = ref<{ validate: () => Promise<void> }>()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await adminStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败'
    ElMessage.error(msg)
  } finally { loading.value = false }
}
</script>

<style scoped lang="scss">
.admin-login-page {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #4338ca 100%);
}

.login-card {
  width: 420px;
  background: var(--admin-card-bg);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 25px 50px rgba(0,0,0,0.25);
}

.login-header { text-align: center; margin-bottom: 32px; }
.login-logo {
  width: 56px; height: 56px;
  background: linear-gradient(135deg, var(--admin-primary), var(--admin-primary-light));
  border-radius: 16px;
  display: inline-flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 800; font-size: 24px; margin-bottom: 16px;
}
.login-header h2 { font-size: 22px; font-weight: 700; color: var(--admin-text); margin-bottom: 4px; }
.login-header p { font-size: 14px; color: var(--admin-text-secondary); }
</style>

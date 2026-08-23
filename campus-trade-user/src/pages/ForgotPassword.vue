<template>
  <AuthLayout subtitle="校园贸易平台">
    <h2>重置密码</h2>
    <p class="auth-subtitle">通过手机号验证码安全重置密码</p>

    <el-steps :active="step" align-center class="reset-steps">
      <el-step title="验证身份" />
      <el-step title="输入验证码" />
      <el-step title="设置密码" />
    </el-steps>

    <!-- Step 1: 输入用户名和手机号 -->
    <el-form v-if="step === 0" :model="form" :rules="rules1" ref="formRef1" @submit.prevent="handleSendCode" size="large">
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="form.phone" placeholder="注册时绑定的手机号" prefix-icon="Phone" maxlength="11" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" native-type="submit" round>发送验证码</el-button>
      </el-form-item>
    </el-form>

    <!-- Step 2: 输入验证码 -->
    <el-form v-else-if="step === 1" :model="form" :rules="rules2" ref="formRef2" @submit.prevent="handleVerifyCode" size="large">
      <el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          验证码已发送至手机 {{ maskedPhone }}，请查收（5分钟内有效）
        </el-alert>
      </el-form-item>
      <el-form-item prop="code">
        <div class="code-input-wrapper">
          <el-input v-model="form.code" placeholder="6位验证码" prefix-icon="Message" maxlength="6" class="code-input" />
          <el-button :disabled="countdown > 0" :loading="loading" @click="handleResendCode" round>
            {{ countdown > 0 ? `${countdown}s` : '重新发送' }}
          </el-button>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" native-type="submit" round>下一步</el-button>
      </el-form-item>
    </el-form>

    <!-- Step 3: 设置新密码 -->
    <el-form v-else :model="form" :rules="rules3" ref="formRef3" @submit.prevent="handleResetPassword" size="large">
      <el-form-item prop="newPassword">
        <el-input v-model="form.newPassword" type="password" placeholder="新密码（8-50位，含大小写/数字/特殊字符三种）" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" native-type="submit" round>重置密码</el-button>
      </el-form-item>
    </el-form>

    <div class="auth-footer">
      <a v-if="step > 0" @click="step--" class="back-link">上一步</a>
      <router-link v-else to="/login">返回登录</router-link>
    </div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/components/AuthLayout.vue'
import { sendResetCode, resetPassword } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const step = ref(0)
const countdown = ref(0)
const formRef1 = ref<{ validate: () => Promise<void> } | null>(null)
const formRef2 = ref<{ validate: () => Promise<void> } | null>(null)
const formRef3 = ref<{ validate: () => Promise<void> } | null>(null)

const form = reactive({
  username: '',
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const maskedPhone = computed(() => {
  if (!form.phone || form.phone.length < 7) return form.phone
  return form.phone.slice(0, 3) + '****' + form.phone.slice(-4)
})

const rules1 = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const rules2 = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

const validateConfirm = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules3 = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 50, message: '密码长度8-50位', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        let types = 0
        if (/[a-z]/.test(value)) types++
        if (/[A-Z]/.test(value)) types++
        if (/\d/.test(value)) types++
        if (/[^a-zA-Z\d]/.test(value)) types++
        if (types < 3) callback(new Error('需包含大小写字母、数字、特殊字符中的三种'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

let timer: ReturnType<typeof setInterval> | null = null

const startCountdown = () => {
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!formRef1.value) return
  await formRef1.value.validate()
  loading.value = true
  try {
    await sendResetCode({ username: form.username, phone: form.phone })
    ElMessage.success('验证码已发送')
    step.value = 1
    startCountdown()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleResendCode = async () => {
  loading.value = true
  try {
    await sendResetCode({ username: form.username, phone: form.phone })
    ElMessage.success('验证码已重新发送')
    startCountdown()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleVerifyCode = async () => {
  if (!formRef2.value) return
  await formRef2.value.validate()
  step.value = 2
}

const handleResetPassword = async () => {
  if (!formRef3.value) return
  await formRef3.value.validate()
  loading.value = true
  try {
    await resetPassword({
      username: form.username,
      phone: form.phone,
      code: form.code,
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
h2 { font-size: 28px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; }
.auth-subtitle { color: var(--text-secondary); margin-bottom: 24px; font-size: 15px; }
.reset-steps { margin-bottom: 32px; }
.code-input-wrapper {
  display: flex;
  gap: 12px;
  width: 100%;
  .code-input { flex: 1; }
}
.auth-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-secondary);
  a { color: var(--primary); text-decoration: none; font-weight: 600; cursor: pointer; &:hover { text-decoration: underline; } }
  .back-link { margin-right: 16px; }
}
</style>

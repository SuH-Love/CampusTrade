<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="avatar-section">
            <el-upload
              action="/api/file/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
              accept="image/jpeg,image/png,image/gif,image/webp"
            >
              <el-avatar :size="100" :src="userStore.userInfo?.avatar" class="avatar-clickable" />
              <div class="avatar-overlay">更换头像</div>
            </el-upload>
            <h3>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h3>
            <el-tag v-if="userStore.userInfo?.realVerified === 1" type="success">已认证</el-tag>
            <el-tag v-else type="info">未认证</el-tag>
          </div>
          <el-descriptions :column="1" border style="margin-top: 20px">
            <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '未绑定' }}</el-descriptions-item>
            <el-descriptions-item label="学号">{{ userStore.userInfo?.studentId || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ userStore.userInfo?.createTime }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="编辑资料" name="info">
              <el-form :model="infoForm" :rules="infoRules" ref="infoFormRef" label-width="80px" style="max-width: 500px">
                <el-form-item label="昵称" prop="nickname"><el-input v-model="infoForm.nickname" placeholder="请输入昵称" /></el-form-item>
                <el-form-item label="手机号" prop="phone"><el-input v-model="infoForm.phone" placeholder="请输入手机号" /></el-form-item>
                <el-form-item label="邮箱" prop="email"><el-input v-model="infoForm.email" placeholder="请输入邮箱" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleUpdateInfo" :loading="infoLoading">保存</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="password">
              <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" style="max-width: 500px">
                <el-form-item label="当前密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" /></el-form-item>
                <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-20位密码" /></el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleUpdatePwd" :loading="pwdLoading">修改密码</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="实名认证" name="verify" v-if="userStore.userInfo?.realVerified !== 1">
              <el-form :model="verifyForm" :rules="verifyRules" ref="verifyFormRef" label-width="80px" style="max-width: 500px">
                <el-form-item label="真实姓名" prop="realName"><el-input v-model="verifyForm.realName" placeholder="请输入真实姓名" /></el-form-item>
                <el-form-item label="学号" prop="studentId"><el-input v-model="verifyForm.studentId" placeholder="请输入学号" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleVerify" :loading="verifyLoading">提交认证</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, updatePassword, realNameVerify, uploadAvatar } from '@/api/user'

import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('info')

const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

const beforeAvatarUpload = (file: File) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) ElMessage.error('仅支持 jpg/png/gif/webp 格式')
  if (!isLt10M) ElMessage.error('图片大小不能超过 10MB')
  return isImage && isLt10M
}

const handleAvatarSuccess = async (response: { code: number; data: string; message?: string }) => {
  if (response.code === 200 && response.data) {
    await uploadAvatar(response.data)
    await userStore.fetchUserInfo()
    ElMessage.success('头像更新成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const infoFormRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const verifyFormRef = ref<FormInstance>()
const infoLoading = ref(false)
const pwdLoading = ref(false)
const verifyLoading = ref(false)

const infoForm = reactive({ nickname: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const verifyForm = reactive({ realName: '', studentId: '' })

const phoneValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) callback(new Error('手机号格式不正确'))
  else callback()
}

const emailValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) callback(new Error('邮箱格式不正确'))
  else callback()
}

const confirmPwdValidator = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
  if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const infoRules = {
  nickname: [{ max: 20, message: '昵称不能超过20个字符', trigger: 'blur' }],
  phone: [{ validator: phoneValidator, trigger: 'blur' }],
  email: [{ validator: emailValidator, trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 20, message: '密码长度8-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: confirmPwdValidator, trigger: 'blur' }
  ]
}

const verifyRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号', trigger: 'blur' }]
}

onMounted(() => {
  if (userStore.userInfo) {
    infoForm.nickname = userStore.userInfo.nickname || ''
    infoForm.phone = userStore.userInfo.phone || ''
    infoForm.email = userStore.userInfo.email || ''
  }
})

const handleUpdateInfo = async () => {
  if (!infoFormRef.value) return
  await infoFormRef.value.validate()
  infoLoading.value = true
  try {
    await updateUserInfo(infoForm)
    await userStore.fetchUserInfo()
    ElMessage.success('更新成功')
  } finally {
    infoLoading.value = false
  }
}

const handleUpdatePwd = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } finally {
    pwdLoading.value = false
  }
}

const handleVerify = async () => {
  if (!verifyFormRef.value) return
  await verifyFormRef.value.validate()
  verifyLoading.value = true
  try {
    await realNameVerify(verifyForm.realName, verifyForm.studentId)
    await userStore.fetchUserInfo()
    ElMessage.success('认证申请已提交')
  } finally {
    verifyLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-page { padding: 20px; }
.profile-card { text-align: center; }
.avatar-section {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  position: relative;
}
.avatar-clickable { cursor: pointer; }
.avatar-overlay {
  position: absolute; top: 0; left: 50%; transform: translateX(-50%);
  width: 100px; height: 100px; border-radius: 50%;
  background: rgba(0,0,0,0.5); color: #fff; font-size: 12px;
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s; cursor: pointer; pointer-events: none;
}
.avatar-section:hover .avatar-overlay { opacity: 1; }
</style>

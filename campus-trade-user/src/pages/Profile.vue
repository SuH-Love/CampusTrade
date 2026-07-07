<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="avatar-section">
            <el-avatar :size="100" :src="userStore.userInfo?.avatar" />
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
              <el-form :model="infoForm" label-width="80px" style="max-width: 500px">
                <el-form-item label="昵称"><el-input v-model="infoForm.nickname" /></el-form-item>
                <el-form-item label="手机号"><el-input v-model="infoForm.phone" /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="infoForm.email" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleUpdateInfo">保存</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="password">
              <el-form :model="pwdForm" label-width="100px" style="max-width: 500px">
                <el-form-item label="当前密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
                <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
                <el-form-item label="确认新密码"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleUpdatePwd">修改密码</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="实名认证" name="verify" v-if="userStore.userInfo?.realVerified !== 1">
              <el-form :model="verifyForm" label-width="80px" style="max-width: 500px">
                <el-form-item label="真实姓名"><el-input v-model="verifyForm.realName" /></el-form-item>
                <el-form-item label="学号"><el-input v-model="verifyForm.studentId" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleVerify">提交认证</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, updatePassword, realNameVerify } from '@/api/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const activeTab = ref('info')

const infoForm = reactive({ nickname: '', phone: '', email: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const verifyForm = reactive({ realName: '', studentId: '' })

onMounted(() => {
  if (userStore.userInfo) {
    infoForm.nickname = userStore.userInfo.nickname || ''
    infoForm.phone = userStore.userInfo.phone || ''
    infoForm.email = userStore.userInfo.email || ''
  }
})

const handleUpdateInfo = async () => {
  await updateUserInfo(infoForm)
  await userStore.fetchUserInfo()
  ElMessage.success('更新成功')
}

const handleUpdatePwd = async () => {
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 8) {
    ElMessage.error('密码长度不能少于8位')
    return
  }
  await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码修改成功')
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
}

const handleVerify = async () => {
  await realNameVerify(verifyForm.realName, verifyForm.studentId)
  await userStore.fetchUserInfo()
  ElMessage.success('认证申请已提交')
}
</script>

<style scoped lang="scss">
.profile-page { padding: 20px; }
.profile-card { text-align: center; }
.avatar-section { display: flex; flex-direction: column; align-items: center; gap: 12px; }
</style>

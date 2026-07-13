<template>
  <div class="profile-page page-bg">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :md="8">
        <el-card class="profile-card">
          <div class="avatar-section">
            <template v-if="isSelf">
              <el-upload action="/api/file/upload" :headers="uploadHeaders" :show-file-list="false" :on-success="handleAvatarSuccess" :before-upload="beforeAvatarUpload" accept="image/jpeg,image/png,image/gif,image/webp">
                <el-avatar :size="100" :src="userStore.userInfo?.avatar" class="avatar-clickable" />
                <div class="avatar-overlay">更换头像</div>
              </el-upload>
            </template>
            <el-avatar v-else :size="100" :src="profileUser?.avatar || '/default-avatar.svg'" />
            <h3 class="profile-name">{{ isSelf ? (userStore.userInfo?.nickname || userStore.userInfo?.username) : (profileUser?.nickname || profileUser?.username) }}</h3>
            <template v-if="isSelf">
              <el-tag v-if="userStore.userInfo?.realVerified === 1" type="success" effect="dark" round>已认证</el-tag>
              <el-tag v-else type="info" effect="plain" round>未认证</el-tag>
              <div class="profile-stats">
                <span>{{ selfFollowCounts.following }} 关注</span>
                <span>·</span>
                <span>{{ selfFollowCounts.followers }} 粉丝</span>
                <template v-if="selfAvgRating > 0">
                  <span>·</span>
                  <el-rate :model-value="selfAvgRating" disabled size="small" class="rate-inline" />
                </template>
              </div>
            </template>
            <template v-else>
              <el-tag v-if="profileUser?.realVerified === 1" type="success" effect="dark" round>已认证</el-tag>
              <div class="profile-stats">
                <span>{{ followCounts.following }} 关注</span>
                <span>·</span>
                <span>{{ followCounts.followers }} 粉丝</span>
                <template v-if="avgRating > 0">
                  <span>·</span>
                  <el-rate :model-value="avgRating" disabled size="small" class="rate-inline" />
                </template>
                <template v-else>
                  <span>·</span>
                  <span class="text-muted-sm">暂无评价</span>
                </template>
              </div>
              <el-button v-if="userStore.token" :type="isFollowed ? 'warning' : 'default'" @click="handleToggleFollow" :loading="followLoading" round class="follow-btn">
                {{ isFollowed ? '已关注' : '关注' }}
              </el-button>
            </template>
          </div>
          <el-descriptions :column="1" border class="profile-desc">
            <template v-if="isSelf">
              <el-descriptions-item label="用户名">{{ userStore.userInfo?.username }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ userStore.userInfo?.phone || '未绑定' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ userStore.userInfo?.email || '未绑定' }}</el-descriptions-item>
              <el-descriptions-item label="学号">{{ userStore.userInfo?.studentId || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ userStore.userInfo?.createTime }}</el-descriptions-item>
            </template>
            <template v-else>
              <el-descriptions-item label="用户名">{{ profileUser?.username }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ profileUser?.createTime }}</el-descriptions-item>
            </template>
          </el-descriptions>
          <el-button v-if="isSelf" type="danger" plain round class="logout-btn" @click="handleLogout">退出登录</el-button>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="16">
        <template v-if="isSelf">
          <div class="stats-grid">
            <div class="stat-card" @click="$router.push('/my-goods')">
              <div class="stat-icon stat-icon--purple">📦</div>
              <div class="stat-value">{{ stats.publishedGoods }}</div>
              <div class="stat-label">发布商品</div>
            </div>
            <div class="stat-card" @click="$router.push('/my-goods?status=ONLINE')">
              <div class="stat-icon stat-icon--green">🛍️</div>
              <div class="stat-value">{{ stats.onlineGoods }}</div>
              <div class="stat-label">在售商品</div>
            </div>
            <div class="stat-card" @click="$router.push('/order?tab=buyer')">
              <div class="stat-icon stat-icon--amber">🛒</div>
              <div class="stat-value">{{ stats.buyerOrders }}</div>
              <div class="stat-label">我的订单</div>
            </div>
            <div class="stat-card" @click="$router.push('/order?tab=seller')">
              <div class="stat-icon stat-icon--violet">💰</div>
              <div class="stat-value">{{ stats.sellerOrders }}</div>
              <div class="stat-label">出售商品</div>
            </div>
            <div class="stat-card" @click="$router.push('/order?tab=buyer&status=FINISHED')">
              <div class="stat-icon stat-icon--cyan">✅</div>
              <div class="stat-value">{{ stats.finishedOrders }}</div>
              <div class="stat-label">完成购物</div>
            </div>
            <div class="stat-card" @click="$router.push('/address')">
              <div class="stat-icon stat-icon--pink">📍</div>
              <div class="stat-label">收货地址</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon--red">💳</div>
              <div class="stat-value">¥{{ stats.totalSpent || 0 }}</div>
              <div class="stat-label">累计消费</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon stat-icon--emerald">💵</div>
              <div class="stat-value">¥{{ stats.totalEarned || 0 }}</div>
              <div class="stat-label">累计收入</div>
            </div>
          </div>
          <el-card class="edit-card">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="编辑资料" name="info">
                <el-form :model="infoForm" :rules="infoRules" ref="infoFormRef" label-width="80px" class="profile-form">
                  <el-form-item label="昵称" prop="nickname"><el-input v-model="infoForm.nickname" placeholder="请输入昵称" /></el-form-item>
                  <el-form-item label="手机号" prop="phone"><el-input v-model="infoForm.phone" placeholder="请输入手机号" /></el-form-item>
                  <el-form-item label="邮箱" prop="email"><el-input v-model="infoForm.email" placeholder="请输入邮箱" /></el-form-item>
                  <el-form-item><el-button type="primary" @click="handleUpdateInfo" :loading="infoLoading" round>保存修改</el-button></el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="修改密码" name="password">
                <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" class="profile-form">
                  <el-form-item label="当前密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" /></el-form-item>
                  <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-20位密码" /></el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" /></el-form-item>
                  <el-form-item><el-button type="primary" @click="handleUpdatePwd" :loading="pwdLoading" round>修改密码</el-button></el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="实名认证" name="verify" v-if="userStore.userInfo?.realVerified !== 1">
                <el-form :model="verifyForm" :rules="verifyRules" ref="verifyFormRef" label-width="80px" class="profile-form">
                  <el-form-item label="真实姓名" prop="realName"><el-input v-model="verifyForm.realName" placeholder="请输入真实姓名" /></el-form-item>
                  <el-form-item label="学号" prop="studentId"><el-input v-model="verifyForm.studentId" placeholder="请输入学号" /></el-form-item>
                  <el-form-item><el-button type="primary" @click="handleVerify" :loading="verifyLoading" round>提交认证</el-button></el-form-item>
                </el-form>
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </template>
        <template v-else>
          <el-card>
            <h3 class="section-title">在售商品</h3>
            <el-row :gutter="16">
              <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
                <GoodsCard :goods="item" />
              </el-col>
            </el-row>
            <el-empty v-if="goodsList.length === 0 && !goodsLoading" description="暂无在售商品" />
            <el-pagination v-if="goodsTotal > goodsPageSize" v-model:current-page="goodsPageNum" :page-size="goodsPageSize" :total="goodsTotal" layout="prev, pager, next" @current-change="loadOtherUserGoods" class="goods-pagination" />
          </el-card>
        </template>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, updatePassword, realNameVerify, uploadAvatar, getUserPublicInfo, getUserStats, type UserStatsVO } from '@/api/user'
import { getGoodsList } from '@/api/goods'
import { getFollowCounts, toggleFollow, isFollowing } from '@/api/follow'
import { getAverageRating } from '@/api/rating'
import GoodsCard from '@/components/GoodsCard.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { UserVO } from '@/api/user'
import type { GoodsVO } from '@/api/goods'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('info')

const isSelf = computed(() => !route.params.id || Number(route.params.id) === userStore.userInfo?.id)

const profileUser = ref<UserVO | null>(null)
const followCounts = ref<{ following: number; followers: number }>({ following: 0, followers: 0 })
const avgRating = ref(0)
const isFollowed = ref(false)
const followLoading = ref(false)
const goodsList = ref<GoodsVO[]>([])
const goodsLoading = ref(false)
const goodsPageNum = ref(1)
const goodsPageSize = ref(12)
const goodsTotal = ref(0)
const stats = ref<UserStatsVO>({ publishedGoods: 0, onlineGoods: 0, buyerOrders: 0, sellerOrders: 0, finishedOrders: 0, totalSpent: 0, totalEarned: 0 })
const selfFollowCounts = ref<{ following: number; followers: number }>({ following: 0, followers: 0 })
const selfAvgRating = ref(0)

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

const loadOtherUser = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  try { profileUser.value = await getUserPublicInfo(userId) } catch (e) { console.error(e) }
  try { followCounts.value = await getFollowCounts(userId) } catch (e) { console.error(e) }
  try { avgRating.value = await getAverageRating(userId) } catch (e) { console.error(e) }
  if (userStore.token && !isSelf.value) {
    try { isFollowed.value = await isFollowing(userId) } catch (e) { console.error(e) }
  }
  goodsPageNum.value = 1
  loadOtherUserGoods()
}

const loadOtherUserGoods = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  goodsLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: goodsPageNum.value, pageSize: goodsPageSize.value, userId, status: 'ONLINE' })
    goodsList.value = res.list || []
    goodsTotal.value = res.total || 0
  } catch (e) { console.error(e) } finally { goodsLoading.value = false }
}

const handleToggleFollow = async () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  followLoading.value = true
  try {
    await toggleFollow(Number(route.params.id))
    isFollowed.value = !isFollowed.value
    followCounts.value.followers += isFollowed.value ? 1 : -1
    ElMessage.success(isFollowed.value ? '已关注' : '已取消关注')
  } finally { followLoading.value = false }
}

const handleUpdateInfo = async () => {
  if (!infoFormRef.value) return
  await infoFormRef.value.validate()
  infoLoading.value = true
  try {
    await updateUserInfo(infoForm)
    await userStore.fetchUserInfo()
    ElMessage.success('更新成功')
  } finally { infoLoading.value = false }
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
  } finally { pwdLoading.value = false }
}

const handleVerify = async () => {
  if (!verifyFormRef.value) return
  await verifyFormRef.value.validate()
  verifyLoading.value = true
  try {
    await realNameVerify(verifyForm.realName, verifyForm.studentId)
    await userStore.fetchUserInfo()
    ElMessage.success('认证申请已提交')
  } finally { verifyLoading.value = false }
}

const handleLogout = async () => {
  await ElMessageBox.confirm('确认退出登录？', '退出确认', { type: 'warning' })
  await userStore.logout()
  router.push('/login')
}

watch(() => route.params.id, () => {
  if (route.params.id && !isSelf.value) loadOtherUser()
})

onMounted(() => {
  if (userStore.userInfo) {
    infoForm.nickname = userStore.userInfo.nickname || ''
    infoForm.phone = userStore.userInfo.phone || ''
    infoForm.email = userStore.userInfo.email || ''
  }
  if (isSelf.value && userStore.token) {
    getUserStats().then(s => { stats.value = s }).catch((e) => { console.error(e) })
    if (userStore.userInfo?.id) {
      getFollowCounts(userStore.userInfo.id).then(c => { selfFollowCounts.value = c }).catch((e) => { console.error(e) })
      getAverageRating(userStore.userInfo.id).then(r => { selfAvgRating.value = r }).catch((e) => { console.error(e) })
    }
  }
  if (route.params.id && !isSelf.value) loadOtherUser()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 20px;

  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid rgba(99, 102, 241, 0.08);
    box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
  }
}
.profile-card { text-align: center; }
.profile-name { font-size: 20px; font-weight: 700; letter-spacing: -0.3px; }
.stats-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
  @media (max-width: 768px) { grid-template-columns: repeat(2, 1fr); }
}
.stat-card {
  background: var(--bg-card); border-radius: 16px; padding: 20px 14px;
  border: 1px solid var(--border); text-align: center; cursor: pointer;
  transition: var(--transition);
  &:hover { border-color: var(--primary-light); box-shadow: 0 6px 20px rgba(99, 102, 241, 0.1); transform: translateY(-3px); }
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; margin: 0 auto 10px;
  &--purple { background: linear-gradient(135deg, #6366f1, #818cf8); }
  &--green { background: linear-gradient(135deg, #10b981, #34d399); }
  &--amber { background: linear-gradient(135deg, #f59e0b, #fbbf24); }
  &--violet { background: linear-gradient(135deg, #8b5cf6, #a78bfa); }
  &--cyan { background: linear-gradient(135deg, #06b6d4, #22d3ee); }
  &--pink { background: linear-gradient(135deg, #ec4899, #f472b6); }
  &--red { background: linear-gradient(135deg, #ef4444, #f87171); }
  &--emerald { background: linear-gradient(135deg, #22c55e, #4ade80); }
}
.stat-value { font-size: 28px; font-weight: 800; color: var(--primary); letter-spacing: -0.5px; }
.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 4px; font-weight: 500; }
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
.profile-stats { color: var(--text-secondary); font-size: 14px; display: flex; align-items: center; gap: 6px; }
.rate-inline { vertical-align: middle; }
.text-muted-sm { font-size: 12px; color: var(--text-muted); }
.follow-btn { margin-top: 8px; }
.profile-desc { margin-top: 20px; }
.edit-card { margin-top: 20px; }
.profile-form { max-width: 500px; }
.section-title { margin: 0 0 16px; }
.logout-btn { margin-top: 16px; width: 100%; }
.goods-pagination { margin-top: 16px; justify-content: center; }
</style>

<template>
  <div class="profile-page">
    <el-row :gutter="20">
      <el-col :span="8">
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
                <el-rate :model-value="selfAvgRating" disabled size="small" style="vertical-align: middle" />
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
                  <el-rate :model-value="avgRating" disabled size="small" style="vertical-align: middle" />
                </template>
                <template v-else>
                  <span>·</span>
                  <span style="font-size: 12px; color: var(--text-muted)">暂无评价</span>
                </template>
              </div>
              <el-button v-if="userStore.token" :type="isFollowed ? 'warning' : 'default'" @click="handleToggleFollow" :loading="followLoading" round style="margin-top: 8px">
                {{ isFollowed ? '已关注' : '关注' }}
              </el-button>
            </template>
          </div>
          <el-descriptions :column="1" border style="margin-top: 20px" class="profile-desc">
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
        </el-card>
      </el-col>
      <el-col :span="16">
        <template v-if="isSelf">
          <div class="stats-grid">
            <div class="stat-card" @click="$router.push('/my-goods')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #6366f1, #818cf8)">📦</div>
              <div class="stat-value">{{ stats.publishedGoods }}</div>
              <div class="stat-label">发布商品</div>
            </div>
            <div class="stat-card" @click="$router.push('/my-goods?status=ONLINE')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #10b981, #34d399)">🛍️</div>
              <div class="stat-value">{{ stats.onlineGoods }}</div>
              <div class="stat-label">在售商品</div>
            </div>
            <div class="stat-card" @click="$router.push('/order?tab=buyer')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b, #fbbf24)">🛒</div>
              <div class="stat-value">{{ stats.buyerOrders }}</div>
              <div class="stat-label">我的订单</div>
            </div>
            <div class="stat-card" @click="$router.push('/order?tab=seller')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6, #a78bfa)">💰</div>
              <div class="stat-value">{{ stats.sellerOrders }}</div>
              <div class="stat-label">出售商品</div>
            </div>
            <div class="stat-card" @click="$router.push('/order?tab=buyer&status=FINISHED')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #06b6d4, #22d3ee)">✅</div>
              <div class="stat-value">{{ stats.finishedOrders }}</div>
              <div class="stat-label">完成购物</div>
            </div>
            <div class="stat-card" @click="$router.push('/address')">
              <div class="stat-icon" style="background: linear-gradient(135deg, #ec4899, #f472b6)">📍</div>
              <div class="stat-label">收货地址</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, #ef4444, #f87171)">💳</div>
              <div class="stat-value">¥{{ stats.totalSpent || 0 }}</div>
              <div class="stat-label">累计消费</div>
            </div>
            <div class="stat-card">
              <div class="stat-icon" style="background: linear-gradient(135deg, #22c55e, #4ade80)">💵</div>
              <div class="stat-value">¥{{ stats.totalEarned || 0 }}</div>
              <div class="stat-label">累计收入</div>
            </div>
          </div>
          <el-card style="margin-top: 20px" class="edit-card">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="编辑资料" name="info">
                <el-form :model="infoForm" :rules="infoRules" ref="infoFormRef" label-width="80px" style="max-width: 500px">
                  <el-form-item label="昵称" prop="nickname"><el-input v-model="infoForm.nickname" placeholder="请输入昵称" /></el-form-item>
                  <el-form-item label="手机号" prop="phone"><el-input v-model="infoForm.phone" placeholder="请输入手机号" /></el-form-item>
                  <el-form-item label="邮箱" prop="email"><el-input v-model="infoForm.email" placeholder="请输入邮箱" /></el-form-item>
                  <el-form-item><el-button type="primary" @click="handleUpdateInfo" :loading="infoLoading" round>保存修改</el-button></el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="修改密码" name="password">
                <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="100px" style="max-width: 500px">
                  <el-form-item label="当前密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" /></el-form-item>
                  <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="8-20位密码" /></el-form-item>
                  <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" /></el-form-item>
                  <el-form-item><el-button type="primary" @click="handleUpdatePwd" :loading="pwdLoading" round>修改密码</el-button></el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane label="实名认证" name="verify" v-if="userStore.userInfo?.realVerified !== 1">
                <el-form :model="verifyForm" :rules="verifyRules" ref="verifyFormRef" label-width="80px" style="max-width: 500px">
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
            <h3 style="margin: 0 0 16px">在售商品</h3>
            <el-row :gutter="16">
              <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
                <div class="goods-card" @click="$router.push(`/goods/${item.id}`)">
                  <div class="goods-img-wrap">
                    <img :src="item.coverImage || '/default-cover.svg'" class="goods-img" loading="lazy" />
                    <span class="goods-category-tag">{{ item.categoryName }}</span>
                  </div>
                  <div class="goods-info">
                    <div class="goods-title">{{ item.title }}</div>
                    <div class="goods-bottom">
                      <span class="price-text">¥{{ item.price }}</span>
                      <span class="goods-views">{{ item.viewCount }} 浏览</span>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
            <el-empty v-if="goodsList.length === 0 && !goodsLoading" description="暂无在售商品" />
          </el-card>
        </template>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, updatePassword, realNameVerify, uploadAvatar, getUserPublicInfo, getUserStats, type UserStatsVO } from '@/api/user'
import { getGoodsList } from '@/api/goods'
import { getFollowCounts, toggleFollow, isFollowing } from '@/api/follow'
import { getAverageRating } from '@/api/rating'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { UserVO } from '@/api/user'
import type { GoodsVO } from '@/api/goods'

const route = useRoute()
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
  goodsLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: 1, pageSize: 50, userId, status: 'ONLINE' })
    goodsList.value = res.list || []
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
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 50%, #f0fdf4 100%);
  min-height: calc(100vh - 60px);
  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid rgba(99, 102, 241, 0.08);
    box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
  }
}
.profile-card { text-align: center; }
.profile-name { font-size: 20px; font-weight: 700; letter-spacing: -0.3px; }
.stats-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px;
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
.edit-card {
  :deep(.el-tabs__item.is-active) { color: var(--primary); font-weight: 600; }
  :deep(.el-tabs__active-bar) { background: var(--primary); }
}

.goods-card {
  background: var(--bg-card); border-radius: 14px; overflow: hidden; cursor: pointer;
  transition: var(--transition); border: 1px solid var(--border); margin-bottom: 16px;
  &:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); border-color: var(--primary-lighter); }
}
.goods-img-wrap { position: relative; padding-top: 75%; overflow: hidden; background: #f1f5f9; }
.goods-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s ease; .goods-card:hover & { transform: scale(1.05); } }
.goods-category-tag { position: absolute; top: 8px; left: 8px; background: rgba(0,0,0,0.5); color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.goods-info { padding: 12px; }
.goods-title { font-size: 14px; font-weight: 500; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.price-text { color: #f56c6c; font-weight: 700; font-size: 16px; }
.goods-views { font-size: 12px; color: var(--text-muted); }
</style>

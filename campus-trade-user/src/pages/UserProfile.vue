<template>
  <div class="user-profile-page page-container">
    <el-card v-if="profileUser" class="profile-header-card">
      <div class="profile-header">
        <el-avatar :size="80" :src="profileUser.avatar || '/default-avatar.svg'" />
        <div class="profile-meta">
          <h2 class="profile-nickname">{{ profileUser.nickname || profileUser.username }}</h2>
          <div class="profile-stats">
            <span>{{ followCounts.following }} 关注</span>
            <span>·</span>
            <span>{{ followCounts.followers }} 粉丝</span>
            <template v-if="avgRating > 0">
              <span>·</span>
              <span><el-rate :model-value="avgRating" disabled size="small" style="vertical-align: middle" /></span>
            </template>
          </div>
        </div>
        <el-button v-if="isSelf" type="primary" @click="$router.push('/profile')" round>编辑资料</el-button>
        <el-button v-else-if="userStore.token" :type="isFollowed ? 'warning' : 'default'" @click="handleToggleFollow" :loading="followLoading" round>
          {{ isFollowed ? '已关注' : '关注' }}
        </el-button>
      </div>
    </el-card>

    <section style="margin-top: 24px">
      <h3 class="section-title">在售商品</h3>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
          <div class="goods-card" @click="$router.push(`/goods/${item.id}`)">
            <div class="goods-img-wrap">
              <img :src="item.coverImage || '/default-cover.svg'" class="goods-img" />
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
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getUserPublicInfo } from '@/api/user'
import { getGoodsList } from '@/api/goods'
import { getFollowCounts, toggleFollow, isFollowing } from '@/api/follow'
import { getAverageRating } from '@/api/rating'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { UserVO } from '@/api/user'
import type { GoodsVO } from '@/api/goods'

const route = useRoute()
const userStore = useUserStore()

const profileUser = ref<UserVO | null>(null)
const followCounts = ref<{ following: number; followers: number }>({ following: 0, followers: 0 })
const avgRating = ref(0)
const isFollowed = ref(false)
const followLoading = ref(false)
const goodsList = ref<GoodsVO[]>([])
const goodsLoading = ref(false)

const isSelf = computed(() => userStore.userInfo?.id === Number(route.params.id))

const loadData = async () => {
  const userId = Number(route.params.id)
  if (!userId) return
  try {
    profileUser.value = await getUserPublicInfo(userId)
  } catch { /* ignore */ }
  try {
    followCounts.value = await getFollowCounts(userId)
  } catch { /* ignore */ }
  try {
    avgRating.value = await getAverageRating(userId)
  } catch { /* ignore */ }
  if (userStore.token && !isSelf.value) {
    try { isFollowed.value = await isFollowing(userId) } catch { /* ignore */ }
  }
  goodsLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: 1, pageSize: 50, userId, status: 'ONLINE' })
    goodsList.value = res.list || []
  } catch { /* ignore */ } finally { goodsLoading.value = false }
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

watch(() => route.params.id, () => { loadData() })
onMounted(loadData)
</script>

<style scoped lang="scss">
.profile-header-card { margin-bottom: 24px; }
.profile-header { display: flex; align-items: center; gap: 20px; }
.profile-nickname { font-size: 22px; font-weight: 700; margin: 0 0 6px; }
.profile-stats { color: var(--text-secondary); font-size: 14px; display: flex; align-items: center; gap: 6px; }

.goods-card {
  background: var(--bg-card); border-radius: var(--radius-md); overflow: hidden; cursor: pointer;
  transition: var(--transition); border: 1px solid var(--border); margin-bottom: 16px;
  &:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
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
<template>
  <div class="following-page page-bg">
    <div class="following-inner">
      <div class="following-header">
        <h3 class="following-title">我的关注</h3>
        <el-input v-model="searchKeyword" placeholder="搜索用户名" clearable class="search-input" prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
      </div>
      <div v-if="filteredFollowing.length === 0 && !loading">
        <EmptyState icon="👥" title="暂无关注" description="去发现有趣的卖家，关注他们获取最新动态" action-text="去逛逛" @action="$router.push('/goods')" />
      </div>
      <div v-else class="following-list" v-loading="loading">
        <div v-for="item in filteredFollowing" :key="item.id" class="following-item">
          <el-avatar :size="56" :src="item.avatar || '/default-avatar.svg'" @click="$router.push(`/profile/${item.id}`)" class="following-avatar" />
          <div class="following-info" @click="$router.push(`/profile/${item.id}`)">
            <div class="following-name">{{ item.nickname || item.username }}</div>
            <div class="following-stats">
              <span class="stat-item"><span class="stat-num">{{ item.followersCount ?? 0 }}</span> 粉丝</span>
              <span class="stat-divider">·</span>
              <span class="stat-item"><span class="stat-num">{{ item.goodsCount ?? 0 }}</span> 在售</span>
              <span class="stat-divider">·</span>
              <span class="stat-item"><span class="stat-num">{{ item.soldCount ?? 0 }}</span> 已售</span>
              <template v-if="item.avgRating && item.avgRating > 0">
                <span class="stat-divider">·</span>
                <span class="stat-item"><el-rate :model-value="item.avgRating" disabled size="small" class="inline-rate" /></span>
              </template>
            </div>
          </div>
          <el-button class="unfollow-btn" size="small" @click="handleUnfollow(item.id)" :loading="unfollowing === item.id" round>取消关注</el-button>
        </div>
      </div>
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getFollowingList, toggleFollow } from '@/api/follow'
import EmptyState from '@/components/EmptyState.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface FollowingUser {
  id: number
  username: string
  nickname: string
  avatar: string
  followingCount?: number
  followersCount?: number
  goodsCount?: number
  soldCount?: number
  avgRating?: number
}

const followingList = ref<FollowingUser[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const unfollowing = ref<number | null>(null)
const searchKeyword = ref('')

const filteredFollowing = computed(() => {
  if (!searchKeyword.value) return followingList.value
  const kw = searchKeyword.value.toLowerCase()
  return followingList.value.filter(u =>
    (u.nickname || '').toLowerCase().includes(kw) ||
    u.username.toLowerCase().includes(kw)
  )
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFollowingList({ pageNum: pageNum.value, pageSize: pageSize.value })
    followingList.value = res.list || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const handleSearch = () => { pageNum.value = 1; loadData() }

const handleUnfollow = async (userId: number) => {
  await ElMessageBox.confirm('确认取消关注？', '取消关注')
  unfollowing.value = userId
  try {
    await toggleFollow(userId)
    ElMessage.success('已取消关注')
    loadData()
  } finally { unfollowing.value = null }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.following-page { padding: 20px; }
.following-inner {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}
.following-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.following-title { margin: 0; }
.search-input { width: 240px; }
.following-list { max-height: 600px; overflow-y: auto; }
.following-item {
  display: flex; align-items: center; gap: 16px;
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  margin-bottom: 12px;
  transition: var(--transition-fast);
  &:hover { background: var(--bg-hover); border-color: var(--primary-lighter); box-shadow: 0 4px 16px rgba(99, 102, 241, 0.08); }
}
.following-avatar { cursor: pointer; flex-shrink: 0; }
.following-info { flex: 1; cursor: pointer; min-width: 0; }
.following-name { font-weight: 700; font-size: 15px; color: var(--text-primary); }
.following-stats {
  font-size: 13px; color: var(--text-secondary); margin-top: 6px;
  display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
}
.stat-num { font-weight: 700; color: var(--primary); }
.stat-divider { color: var(--text-muted); margin: 0 2px; }
.inline-rate { vertical-align: middle; }
.unfollow-btn {
  flex-shrink: 0;
  border: 1px solid #f59e0b;
  color: #f59e0b;
  background: transparent;
  font-weight: 600;
  transition: all 0.25s;
  &:hover { background: #f59e0b; color: #fff; transform: translateY(-1px); box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3); }
}
.list-pagination { margin-top: 20px; justify-content: center; }

@media (max-width: 576px) {
  .following-page { padding: 12px; }
  .following-inner { padding: 16px; }
  .following-header { flex-direction: column; align-items: flex-start; }
  .search-input { width: 100%; }
  .following-item { padding: 14px 16px; gap: 12px; }
  .unfollow-btn { font-size: 12px; }
}
</style>

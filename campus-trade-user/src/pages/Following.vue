<template>
  <div class="following-page page-bg">
    <el-card>
      <template #header>
        <h3 style="margin: 0">我的关注</h3>
      </template>
      <el-empty v-if="followingList.length === 0 && !loading" description="暂无关注" />
      <div v-else class="following-list" v-loading="loading">
        <div v-for="item in followingList" :key="item.id" class="following-item">
          <el-avatar :size="56" :src="item.avatar || '/default-avatar.svg'" @click="$router.push(`/profile/${item.id}`)" style="cursor: pointer; flex-shrink: 0" />
          <div class="following-info" @click="$router.push(`/profile/${item.id}`)" style="cursor: pointer">
            <div class="following-name">{{ item.nickname || item.username }}</div>
            <div class="following-stats">
              <span class="stat-item"><span class="stat-num">{{ item.followersCount ?? 0 }}</span> 粉丝</span>
              <span class="stat-divider">·</span>
              <span class="stat-item"><span class="stat-num">{{ item.goodsCount ?? 0 }}</span> 在售</span>
              <span class="stat-divider">·</span>
              <span class="stat-item"><span class="stat-num">{{ item.soldCount ?? 0 }}</span> 已售</span>
              <template v-if="item.avgRating && item.avgRating > 0">
                <span class="stat-divider">·</span>
                <span class="stat-item"><el-rate :model-value="item.avgRating" disabled size="small" style="vertical-align: middle" /></span>
              </template>
            </div>
          </div>
          <el-button class="unfollow-btn" size="small" @click="handleUnfollow(item.id)" :loading="unfollowing === item.id" round>取消关注</el-button>
        </div>
      </div>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFollowingList, toggleFollow } from '@/api/follow'
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

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFollowingList({ pageNum: pageNum.value, pageSize: pageSize.value })
    followingList.value = res.list || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

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
.following-page {
  padding: 20px;

  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid rgba(99, 102, 241, 0.08);
    box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
  }
}
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
.following-info { flex: 1; cursor: pointer; min-width: 0; }
.following-name { font-weight: 700; font-size: 15px; color: var(--text-primary); }
.following-stats {
  font-size: 13px; color: var(--text-secondary); margin-top: 6px;
  display: flex; align-items: center; gap: 4px; flex-wrap: wrap;
}
.stat-num { font-weight: 700; color: var(--primary); }
.stat-divider { color: var(--text-muted); margin: 0 2px; }
.unfollow-btn {
  flex-shrink: 0;
  border: 1px solid #f59e0b;
  color: #f59e0b;
  background: transparent;
  font-weight: 600;
  transition: all 0.25s;
  &:hover { background: #f59e0b; color: #fff; transform: translateY(-1px); box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3); }
}
</style>

<template>
  <div class="following-page">
    <el-card>
      <template #header>
        <h3 style="margin: 0">我的关注</h3>
      </template>
      <el-empty v-if="followingList.length === 0 && !loading" description="暂无关注" />
      <div v-else class="following-list" v-loading="loading">
        <div v-for="item in followingList" :key="item.id" class="following-item">
          <el-avatar :size="52" :src="item.avatar || '/default-avatar.svg'" @click="$router.push(`/profile/${item.id}`)" style="cursor: pointer" />
          <div class="following-info" @click="$router.push(`/profile/${item.id}`)" style="cursor: pointer">
            <div class="following-name">{{ item.nickname || item.username }}</div>
            <div v-if="item.bio" class="following-bio">{{ item.bio }}</div>
            <div class="following-meta">
              <span v-if="item.followingCount !== undefined">{{ item.followingCount }} 关注</span>
              <span v-if="item.followersCount !== undefined">{{ item.followersCount }} 粉丝</span>
              <span v-if="item.goodsCount !== undefined">{{ item.goodsCount }} 商品</span>
            </div>
            <div v-if="item.followTime" class="following-time">关注于 {{ item.followTime }}</div>
          </div>
          <el-button type="warning" size="small" @click="handleUnfollow(item.id)" :loading="unfollowing === item.id" round>取消关注</el-button>
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
  bio: string
  followingCount?: number
  followersCount?: number
  goodsCount?: number
  followTime: string
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
  background: linear-gradient(135deg, #f0f4ff 0%, #faf5ff 50%, #f0fdf4 100%);
  min-height: calc(100vh - 60px);
  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid rgba(99, 102, 241, 0.08);
    box-shadow: 0 4px 24px rgba(99, 102, 241, 0.06);
  }
}
.following-list { max-height: 600px; overflow-y: auto; }
.following-item {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 18px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 10px;
  transition: var(--transition-fast);
  &:hover { background: var(--bg-hover); border-color: var(--primary-lighter); box-shadow: 0 2px 12px rgba(99, 102, 241, 0.06); }
}
.following-info { flex: 1; cursor: pointer; }
.following-name { font-weight: 600; font-size: 15px; }
.following-bio { font-size: 13px; color: var(--text-secondary); margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 300px; }
.following-meta { font-size: 12px; color: var(--text-muted); margin-top: 4px; display: flex; gap: 12px; }
.following-time { font-size: 11px; color: var(--text-muted); margin-top: 3px; opacity: 0.7; }
</style>

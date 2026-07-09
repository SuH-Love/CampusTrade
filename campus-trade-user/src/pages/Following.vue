<template>
  <div class="following-page">
    <el-card>
      <template #header>
        <h3 style="margin: 0">我的关注</h3>
      </template>
      <el-empty v-if="followingList.length === 0 && !loading" description="暂无关注" />
      <div v-else class="following-list" v-loading="loading">
        <div v-for="item in followingList" :key="item.id" class="following-item">
          <el-avatar :size="48" :src="item.avatar || '/default-avatar.svg'" @click="$router.push(`/profile/${item.id}`)" style="cursor: pointer" />
          <div class="following-info" @click="$router.push(`/profile/${item.id}`)" style="cursor: pointer">
            <div class="following-name">{{ item.nickname || item.username }}</div>
          </div>
          <el-button type="warning" size="small" @click="handleUnfollow(item.id)" :loading="unfollowing === item.id" round>取消关注</el-button>
        </div>
      </div>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
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
.following-page { padding: 20px; }
.following-list { max-height: 600px; overflow-y: auto; }
.following-item {
  display: flex; align-items: center; gap: 12px;
  padding: 16px; border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }
  &:hover { background: #fafafa; }
}
.following-info { flex: 1; cursor: pointer; }
.following-name { font-weight: 500; font-size: 15px; }
</style>
<template>
  <div class="blacklist-page page-bg">
    <div class="blacklist-inner page-container">
      <div class="page-header">
        <h2>黑名单管理</h2>
      </div>
      <div class="blacklist-list" v-if="list.length > 0">
        <div v-for="item in list" :key="item.id" class="blacklist-card">
          <el-avatar :size="44" :src="item.blockedAvatar || '/default-avatar.svg'" />
          <div class="blacklist-info">
            <div class="blacklist-name">{{ item.blockedName }}</div>
            <div class="blacklist-time">屏蔽于 {{ formatTime(item.createTime) }}</div>
          </div>
          <el-button type="warning" plain round size="small" @click="handleUnblock(item)">解除屏蔽</el-button>
        </div>
      </div>
      <EmptyState v-else icon="🛡️" title="暂无屏蔽用户" description="屏蔽的用户将无法给你发送消息" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBlacklist, unblockUser, type BlacklistItem } from '@/api/blacklist'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref<BlacklistItem[]>([])

const loadList = async () => {
  try {
    const res = await getBlacklist()
    list.value = (Array.isArray(res) ? res : []) as BlacklistItem[]
  } catch (e) { console.error(e) }
}

const handleUnblock = async (item: BlacklistItem) => {
  try {
    await ElMessageBox.confirm(`确定解除屏蔽「${item.blockedName}」？解除后对方可以给你发消息`, '解除屏蔽', { type: 'info' })
    await unblockUser(item.blockedId)
    ElMessage.success('已解除屏蔽')
    list.value = list.value.filter(b => b.id !== item.id)
  } catch (e) { console.error(e) }
}

const formatTime = (t: string | null | undefined) => {
  if (!t) return ''
  const d = t.includes('T') ? new Date(t) : new Date(t.replace(' ', 'T'))
  if (isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.blacklist-page { min-height: 100vh; }
.blacklist-inner { max-width: 720px; margin: 0 auto; padding: 32px 24px; }
.page-header { margin-bottom: 24px; h2 { font-size: 22px; font-weight: 700; } }
.blacklist-list { display: flex; flex-direction: column; gap: 12px; }
.blacklist-card {
  display: flex; align-items: center; gap: 16px; padding: 16px 20px;
  background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border);
  transition: var(--transition-fast);
  &:hover { box-shadow: var(--shadow-sm); }
}
.blacklist-info { flex: 1; min-width: 0; }
.blacklist-name { font-weight: 600; font-size: 15px; }
.blacklist-time { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
</style>
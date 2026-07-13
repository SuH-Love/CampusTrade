<template>
  <div class="seller-card" v-if="sellerInfo">
    <el-avatar :size="44" :src="sellerInfo.userAvatar || '/default-avatar.svg'" class="seller-avatar" @click="emit('viewProfile')" />
    <div class="seller-info" @click="emit('viewProfile')">
      <div class="seller-name">
        {{ sellerInfo.username }}
        <el-tag v-if="sellerInfo.sellerRealVerified === 1" type="success" effect="dark" size="small" round class="seller-verified-tag">已认证</el-tag>
      </div>
      <div class="seller-action">
        <span>查看主页</span>
        <el-rate v-if="sellerRating > 0" :model-value="sellerRating" disabled size="small" class="seller-rate" />
        <span v-else class="seller-no-rating">暂无评价</span>
      </div>
      <div class="seller-meta">
        <span v-if="sellerPublicInfo" class="seller-meta-item">
          <el-icon><Star /></el-icon>
          信誉 {{ sellerRating > 0 ? sellerRating.toFixed(1) : '-' }}
        </span>
        <span v-if="sellerPublicInfo && sellerPublicInfo.goodsCount !== undefined" class="seller-meta-item">
          <el-icon><Goods /></el-icon>
          在售 {{ sellerPublicInfo.goodsCount ?? 0 }}
        </span>
        <span v-if="followCounts" class="seller-meta-item">
          <el-icon><User /></el-icon>
          {{ followCounts.followers }} 粉丝
        </span>
      </div>
    </div>
    <div class="seller-actions">
      <el-button size="small" @click.stop="emit('chat')" round>聊天</el-button>
      <el-button size="small" type="primary" @click.stop="emit('chat')" round plain>咨询商品</el-button>
      <el-button :type="isFollowed ? 'warning' : 'default'" size="small" @click.stop="emit('follow')" :loading="followLoading" round>
        {{ isFollowed ? '已关注' : '关注' }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Star, Goods, User } from '@element-plus/icons-vue'
import type { GoodsVO } from '@/api/goods'
import type { UserVO } from '@/api/user'

defineProps<{
  sellerInfo: Pick<GoodsVO, 'username' | 'userAvatar' | 'sellerRealVerified'> | null
  sellerRating: number
  sellerPublicInfo: UserVO | null
  followCounts: { following: number; followers: number } | null
  isFollowed: boolean
  followLoading: boolean
}>()

const emit = defineEmits<{
  follow: []
  chat: []
  viewProfile: []
}>()
</script>

<style scoped lang="scss">
.seller-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--bg-glass);
  backdrop-filter: blur(8px);
  transition: var(--transition);
  margin-bottom: 24px;
  &:hover { background: var(--primary-lighter); border-color: var(--primary-light); }
}

.seller-avatar { cursor: pointer; }

.seller-info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.seller-name { font-weight: 600; font-size: 15px; }

.seller-verified-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.seller-action {
  font-size: 12px;
  color: var(--primary);
  margin-top: 2px;
  display: flex;
  align-items: center;
}

.seller-rate {
  margin-left: 8px;
  vertical-align: middle;
}

.seller-no-rating {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.seller-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.seller-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.seller-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .seller-card { flex-wrap: wrap; }
  .seller-actions { width: 100%; justify-content: flex-end; }
}
</style>
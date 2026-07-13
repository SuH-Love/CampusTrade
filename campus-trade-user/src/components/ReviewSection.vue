<template>
  <div class="reviews-section">
    <div class="rating-summary" v-if="ratingDistribution">
      <div class="rating-score">
        <span class="score-num">{{ ratingDistribution.avgRating }}</span>
        <el-rate :model-value="ratingDistribution.avgRating" disabled allow-half size="large" />
        <span class="score-total">{{ ratingDistribution.totalCount }} 条评价</span>
      </div>
      <div class="rating-bars">
        <div v-for="s in [5,4,3,2,1]" :key="s" class="rating-bar-row">
          <span class="bar-label">{{ s }}星</span>
          <div class="bar-track"><div class="bar-fill" :style="{ width: getBarWidth(s) + '%' }" /></div>
          <span class="bar-count">{{ ratingDistribution.distribution[s] || 0 }}</span>
        </div>
      </div>
    </div>
    <div class="review-filter" v-if="ratingDistribution && ratingDistribution.totalCount > 0">
      <el-radio-group v-model="currentFilter" size="small" @change="handleFilterChange">
        <el-radio-button value="all">全部 ({{ ratingDistribution.totalCount }})</el-radio-button>
        <el-radio-button value="good">好评 ({{ ratingDistribution.distribution[5] || 0 }})</el-radio-button>
        <el-radio-button value="medium">中评 ({{ (ratingDistribution.distribution[3] || 0) + (ratingDistribution.distribution[4] || 0) }})</el-radio-button>
        <el-radio-button value="bad">差评 ({{ (ratingDistribution.distribution[1] || 0) + (ratingDistribution.distribution[2] || 0) }})</el-radio-button>
      </el-radio-group>
    </div>
    <div class="review-list" v-if="filteredReviews.length > 0">
      <div v-for="r in filteredReviews" :key="r.id" class="review-item">
        <el-avatar :size="36" :src="r.buyerAvatar || '/default-avatar.svg'" class="reviewer-avatar" @click="$router.push(`/profile/${r.buyerId}`)" />
        <div class="review-body">
          <div class="review-header">
            <span class="reviewer-name" @click="$router.push(`/profile/${r.buyerId}`)">{{ r.buyerName }}</span>
            <span v-if="r.goodsTitle" class="review-goods">购买了「{{ r.goodsTitle }}」</span>
            <el-rate :model-value="r.rating" disabled size="small" />
            <span class="review-time">{{ formatReviewTime(r.createTime) }}</span>
          </div>
          <div class="review-comment" v-if="r.comment">{{ r.comment }}</div>
          <div class="review-images" v-if="r.images">
            <el-image v-for="(img, imgIdx) in parseReviewImages(r.images)" :key="imgIdx" :src="img" fit="cover" class="review-img-thumb" :preview-src-list="parseReviewImages(r.images)" :initial-index="imgIdx" hide-on-click-modal />
          </div>
        </div>
      </div>
      <div class="review-pagination" v-if="ratingTotal > reviewPageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="reviewPageSize"
          :total="ratingTotal"
          layout="prev, pager, next"
          small
          @current-change="handlePageChange"
        />
      </div>
    </div>
    <EmptyState v-else icon="💬" title="暂无评价" description="该卖家还没有收到评价" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import type { SellerRatingVO, RatingDistribution } from '@/api/rating'

const props = defineProps<{
  ratingDistribution: RatingDistribution | null
  reviews: SellerRatingVO[]
  ratingTotal: number
  loading?: boolean
}>()

const emit = defineEmits<{
  loadMore: [page: number]
  filterChange: [filter: 'all' | 'good' | 'medium' | 'bad']
}>()


const currentFilter = ref<'all' | 'good' | 'medium' | 'bad'>('all')
const currentPage = ref(1)
const reviewPageSize = 10

const filteredReviews = computed(() => {
  if (currentFilter.value === 'all') return props.reviews
  return props.reviews.filter((r: SellerRatingVO) => {
    if (currentFilter.value === 'good') return r.rating >= 5
    if (currentFilter.value === 'medium') return r.rating >= 3 && r.rating <= 4
    if (currentFilter.value === 'bad') return r.rating <= 2
    return true
  })
})

const getBarWidth = (star: number) => {
  if (!props.ratingDistribution || props.ratingDistribution.totalCount === 0) return 0
  return ((props.ratingDistribution.distribution[star] || 0) / props.ratingDistribution.totalCount) * 100
}

const formatReviewTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + '天前'
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

const parseReviewImages = (images: string): string[] => {
  if (!images) return []
  return images.split(',').map(s => s.trim()).filter(Boolean)
}

const handleFilterChange = () => {
  currentPage.value = 1
  emit('filterChange', currentFilter.value)
}

const handlePageChange = (page: number) => {
  emit('loadMore', page)
}
</script>

<style scoped lang="scss">
.reviews-section { padding: 8px 0; }
.rating-summary {
  display: flex;
  gap: 32px;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--color-announcement-from), var(--color-announcement-to));
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  align-items: center;
}
.rating-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 120px;
  .score-num { font-size: 42px; font-weight: 800; color: var(--primary); line-height: 1; }
  .score-total { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
}
.rating-bars { flex: 1; }
.rating-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  .bar-label { font-size: 12px; color: var(--text-muted); width: 28px; text-align: right; }
  .bar-track { flex: 1; height: 8px; background: var(--border); border-radius: 4px; overflow: hidden; }
  .bar-fill { height: 100%; background: var(--primary-gradient); border-radius: 4px; transition: width 0.4s ease; }
  .bar-count { font-size: 12px; color: var(--text-muted); width: 24px; }
}

.review-filter { margin-bottom: 16px; }

.reviewer-avatar { cursor: pointer; }

.review-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  &:last-child { border-bottom: none; }
}
.review-body { flex: 1; }
.review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
  .reviewer-name { font-weight: 600; font-size: 14px; cursor: pointer; }
  .review-goods { font-size: 12px; color: var(--primary); background: var(--primary-lighter); padding: 1px 8px; border-radius: 10px; }
  .review-time { font-size: 12px; color: var(--text-muted); margin-left: auto; }
}
.review-comment { font-size: 14px; color: var(--text-secondary); line-height: 1.6; }
.review-images { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.review-img-thumb { width: 72px; height: 72px; border-radius: 8px; cursor: pointer; border: 1px solid var(--border); }
.review-pagination { margin-top: 16px; display: flex; justify-content: center; }

@media (max-width: 768px) {
  .rating-summary { flex-direction: column; gap: 16px; }
}
</style>
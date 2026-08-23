<template>
  <div
    class="goods-card"
    :class="{ 'is-sold': isSold, 'is-offline': isOffline, 'is-clickable': clickable }"
    @click="handleClick"
  >
    <div class="goods-img-wrap" :class="{ 'no-click': !clickable }">
      <img :src="goods.coverImage || '/default-cover.svg'" class="goods-img" loading="lazy" alt="商品图片" />
      <div class="goods-tags">
        <span v-if="goods.categoryName" class="goods-category-tag">{{ goods.categoryName }}</span>
        <span v-if="goods.condition" class="goods-condition-tag">{{ goods.condition }}</span>
        <span v-if="goods.originalPrice && goods.originalPrice > goods.price" class="goods-discount-tag">折扣</span>
      </div>
      <div class="goods-seller" v-if="goods.userAvatar || goods.username">
        <el-avatar v-if="goods.userAvatar" :size="24" :src="goods.userAvatar" class="goods-seller-avatar" />
        <span v-if="goods.username" class="goods-seller-name">{{ goods.username }}</span>
      </div>
      <div v-if="isSold" class="sold-overlay">
        <el-tag type="info" effect="dark" size="large">已售出</el-tag>
      </div>
      <div v-else-if="isOffline" class="sold-overlay">
        <el-tag type="warning" effect="dark" size="large">已下架</el-tag>
      </div>
    </div>
    <div class="goods-info">
      <div class="goods-title">{{ goods.title }}</div>
      <div class="goods-desc" v-if="showDesc && goods.description">{{ goods.description }}</div>
      <div class="goods-meta" v-if="showMeta">
        <span class="goods-views">{{ goods.viewCount }} 浏览</span>
        <span v-if="goods.favoriteCount" class="goods-favs">{{ goods.favoriteCount }} 收藏</span>
      </div>
      <div class="goods-bottom">
        <div class="goods-price-row">
          <span class="price-text">¥{{ goods.price }}</span>
          <span v-if="goods.originalPrice && goods.originalPrice > goods.price" class="original-price">¥{{ goods.originalPrice }}</span>
        </div>
        <button
          v-if="showUnfav"
          class="unfav-btn"
          @click.stop="$emit('unfavorite', goods.id)"
          title="取消收藏"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
        </button>
        <span v-if="!showUnfav" class="goods-views-inline">{{ goods.viewCount }} 浏览</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { GoodsVO } from '@/api/goods'

const props = withDefaults(defineProps<{
  goods: GoodsVO
  clickable?: boolean
  showDesc?: boolean
  showMeta?: boolean
  showUnfav?: boolean
}>(), {
  clickable: true,
  showDesc: false,
  showMeta: false,
  showUnfav: false
})

defineEmits<{
  unfavorite: [id: number]
}>()

const router = useRouter()

const isSold = computed(() => props.goods.status === 'SOLD')
const isOffline = computed(() => props.goods.status === 'OFFLINE')

const handleClick = () => {
  if (!props.clickable) return
  if (isSold.value || isOffline.value) return
  router.push(`/goods/${props.goods.id}`)
}
</script>

<style scoped lang="scss">
.goods-card {
  background: var(--bg-card);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border);
  contain: layout style;
  transition: var(--transition);
  &.is-clickable { cursor: pointer; }
  &.is-clickable:not(.is-sold):not(.is-offline):hover {
    transform: translateY(-6px);
    box-shadow: var(--shadow-lg);
    border-color: var(--primary-lighter);
  }
  &.is-sold, &.is-offline { opacity: 0.75; }
}

.goods-img-wrap {
  position: relative;
  padding-top: 75%;
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-img-placeholder-from), var(--color-img-placeholder-to));
  &.no-click { cursor: not-allowed; }
}

.goods-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: brightness(var(--img-brightness));
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  .goods-card.is-clickable:not(.is-sold):not(.is-offline):hover & {
    transform: scale(1.08);
  }
}

.goods-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  z-index: 2;
}

.goods-category-tag {
  background: var(--color-tag-bg);

  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
  letter-spacing: 0.3px;
}

.goods-condition-tag {
  background: var(--color-condition-bg);

  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
}

.goods-discount-tag {
  background: var(--color-discount-bg);

  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 10px;
}

.goods-seller {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  max-width: 70%;
  padding: 2px 8px 2px 2px;
  background: rgba(0, 0, 0, 0.45);
  border-radius: 14px;
  backdrop-filter: blur(4px);
}
.goods-seller-avatar { border: 1px solid rgba(255, 255, 255, 0.6); flex-shrink: 0; }
.goods-seller-name {
  font-size: 11px;
  color: #fff;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sold-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);

  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-info { padding: 14px 14px 16px; }

.goods-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-meta {
  margin-top: 6px;
  display: flex;
  gap: 10px;
}

.goods-views, .goods-favs {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.goods-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.goods-price-row { display: flex; align-items: baseline; gap: 6px; }

.original-price {
  font-size: 12px;
  color: var(--text-muted);
  text-decoration: line-through;
}

.goods-views-inline {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.unfav-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-unfav);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  &:hover {
    background: var(--color-unfav);
    color: #fff;
    transform: scale(1.15);
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
  }
}
</style>
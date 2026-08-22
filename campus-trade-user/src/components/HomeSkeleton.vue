<template>
  <div class="home-skeleton">
    <div class="bento-grid">
      <div class="bento-hero"><Skeleton variant="image" height="100%" radius="20" /></div>
      <div class="bento-search"><Skeleton variant="rect" height="48" radius="24" /></div>
      <div class="bento-categories">
        <Skeleton v-for="i in 6" :key="i" variant="rect" width="80" height="32" radius="16" />
      </div>
      <div class="bento-hot">
        <div class="bento-section-head"><Skeleton variant="text" width="120" /></div>
        <div class="bento-goods-row">
          <GoodsCardSkeleton v-for="i in 5" :key="i" />
        </div>
      </div>
      <div class="bento-new">
        <div class="bento-section-head"><Skeleton variant="text" width="120" /></div>
        <div class="bento-goods-row">
          <GoodsCardSkeleton v-for="i in 5" :key="i" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Skeleton from './Skeleton.vue'
import GoodsCardSkeleton from './GoodsCardSkeleton.vue'
</script>

<style scoped lang="scss">
.home-skeleton { padding: var(--spacing-lg); }
.bento-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.bento-hero { grid-column: span 4; height: 280px; }
.bento-search { grid-column: span 4; }
.bento-categories { grid-column: span 4; display: flex; gap: 8px; justify-content: center; }
.bento-hot, .bento-new { grid-column: span 4; }
.bento-section-head { margin-bottom: 12px; }
.bento-goods-row { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
@media (max-width: 1200px) { .bento-goods-row { grid-template-columns: repeat(4, 1fr); } }
@media (max-width: 900px) { .bento-goods-row { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 600px) { .bento-goods-row { grid-template-columns: repeat(2, 1fr); } }
</style>
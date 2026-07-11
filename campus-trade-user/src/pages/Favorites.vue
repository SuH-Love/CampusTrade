<template>
  <div class="favorites-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
          <h3 style="margin: 0">我的收藏</h3>
          <div class="filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索商品标题" clearable style="width: 200px" @keyup.enter="handleSearch" @clear="handleSearch" />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" style="width: 140px">
              <el-option label="在售" value="ONLINE" />
              <el-option label="已下架" value="OFFLINE" />
              <el-option label="已售出" value="SOLD" />
            </el-select>
          </div>
        </div>
      </template>
      <el-row :gutter="16" v-loading="loading">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in filteredGoods" :key="item.id">
          <div class="fav-card" :class="{ sold: item.status === 'SOLD' || item.status === 'OFFLINE' }">
            <div class="fav-img-wrap" :class="{ 'no-click': item.status !== 'ONLINE' }" @click="item.status === 'ONLINE' && $router.push(`/goods/${item.id}`)">
              <img :src="item.coverImage || '/default-cover.svg'" class="fav-img" loading="lazy" />
              <div class="fav-tags">
                <span v-if="item.condition" class="fav-condition-tag">{{ item.condition }}</span>
                <span v-if="item.originalPrice && item.originalPrice > item.price" class="fav-discount-tag">折扣</span>
              </div>
              <div v-if="item.status === 'SOLD'" class="sold-overlay">
                <el-tag type="info" effect="dark" size="large">已售出</el-tag>
              </div>
              <div v-else-if="item.status === 'OFFLINE'" class="sold-overlay">
                <el-tag type="warning" effect="dark" size="large">已下架</el-tag>
              </div>
            </div>
            <div class="fav-info">
              <div class="fav-title">{{ item.title }}</div>
              <div class="fav-meta">
                <span class="fav-views">{{ item.viewCount }} 浏览</span>
                <span v-if="item.favoriteCount" class="fav-favs">{{ item.favoriteCount }} 收藏</span>
              </div>
              <div class="fav-bottom">
                <div class="fav-price-row">
                  <span class="price-text">¥{{ item.price }}</span>
                  <span v-if="item.originalPrice && item.originalPrice > item.price" class="fav-original-price">¥{{ item.originalPrice }}</span>
                </div>
                <button class="unfav-btn" @click.stop="handleUnfavorite(item.id)" title="取消收藏">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                </button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
      <el-empty v-if="filteredGoods.length === 0 && !loading" description="暂无收藏" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getFavoriteList, unfavoriteGoods } from '@/api/goods'
import { ElMessage } from 'element-plus'
import type { GoodsVO } from '@/api/goods'
import type { GoodsQueryParams } from '@/types'

const goodsList = ref<GoodsVO[]>([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')

const filteredGoods = computed(() => {
  let list = goodsList.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(g => g.title.toLowerCase().includes(kw))
  }
  return list
})

const loadData = async () => {
  loading.value = true
  try {
    const params: GoodsQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getFavoriteList(params)
    goodsList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleUnfavorite = async (id: number) => {
  await unfavoriteGoods(id)
  ElMessage.success('已取消收藏')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.favorites-page { padding: 20px; }
.filter-bar { display: flex; gap: 12px; align-items: center; }
.fav-card {
  background: var(--bg-card);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border);
  margin-bottom: 16px;
  transition: var(--transition);
  &:not(.sold):hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); border-color: var(--primary-lighter); }
  &.sold { opacity: 0.75; }
}
.fav-img-wrap { position: relative; padding-top: 75%; background: linear-gradient(135deg, #f1f5f9, #e2e8f0); cursor: pointer; overflow: hidden; &.no-click { cursor: not-allowed; } }
.fav-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1); .fav-card:not(.sold):hover & { transform: scale(1.08); } }
.fav-tags {
  position: absolute; top: 8px; left: 8px;
  display: flex; flex-wrap: wrap; gap: 4px; z-index: 2;
}
.fav-condition-tag {
  background: rgba(234, 179, 8, 0.85); backdrop-filter: blur(6px);
  color: #fff; font-size: 11px; font-weight: 600;
  padding: 2px 8px; border-radius: 8px;
}
.fav-discount-tag {
  background: rgba(239, 68, 68, 0.85); backdrop-filter: blur(6px);
  color: #fff; font-size: 11px; font-weight: 600;
  padding: 2px 8px; border-radius: 8px;
}
.sold-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.4); backdrop-filter: blur(2px);
  display: flex; align-items: center; justify-content: center;
}
.fav-info { padding: 14px 14px 16px; }
.fav-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; font-size: 14px; }
.fav-meta { margin-top: 6px; display: flex; gap: 10px; }
.fav-views, .fav-favs { font-size: 12px; color: var(--text-muted); font-weight: 500; }
.fav-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.fav-price-row { display: flex; align-items: baseline; gap: 6px; }
.fav-original-price { font-size: 12px; color: var(--text-muted); text-decoration: line-through; }
.unfav-btn {
  width: 32px; height: 32px; border-radius: 50%;
  border: none; background: rgba(239, 68, 68, 0.1);
  color: #ef4444; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.25s;
  &:hover { background: #ef4444; color: #fff; transform: scale(1.15); box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3); }
}
</style>

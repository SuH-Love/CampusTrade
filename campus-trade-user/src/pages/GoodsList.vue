<template>
  <div class="goods-list-page">
    <div class="list-layout">
      <aside class="filter-sidebar">
        <div class="filter-section">
          <h4 class="filter-title">搜索</h4>
          <el-input v-model="keyword" placeholder="输入关键词..." clearable @keyup.enter="handleSearch" prefix-icon="Search" />
        </div>
        <div class="filter-section">
          <h4 class="filter-title">分类</h4>
          <div class="filter-categories">
            <div class="filter-cat-item" :class="{ active: !categoryId }" @click="setCategory(undefined)">全部</div>
            <div v-for="cat in categories" :key="cat.id" class="filter-cat-item" :class="{ active: categoryId === cat.id }" @click="setCategory(cat.id)">{{ cat.categoryName }}</div>
          </div>
        </div>
        <div class="filter-section">
          <h4 class="filter-title">价格区间</h4>
          <div class="price-range">
            <el-input-number v-model="priceMin" :min="0" :controls="false" placeholder="最低" size="small" />
            <span class="price-sep">—</span>
            <el-input-number v-model="priceMax" :min="0" :controls="false" placeholder="最高" size="small" />
          </div>
        </div>
        <div class="filter-section">
          <h4 class="filter-title">排序方式</h4>
          <el-radio-group v-model="sortBy" @change="handleSearch" class="sort-group">
            <el-radio value="latest">最新发布</el-radio>
            <el-radio value="price_asc">价格最低</el-radio>
            <el-radio value="price_desc">价格最高</el-radio>
            <el-radio value="views">最多浏览</el-radio>
          </el-radio-group>
        </div>
        <el-button type="primary" round class="publish-btn" @click="$router.push('/goods/publish')">
          <el-icon><Plus /></el-icon> 发布商品
        </el-button>
      </aside>

      <main class="goods-main">
        <div class="goods-toolbar">
          <span class="result-count">共 <strong>{{ total }}</strong> 件商品</span>
          <div class="view-toggle">
            <el-icon class="view-btn" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'"><Grid /></el-icon>
            <el-icon class="view-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'"><List /></el-icon>
          </div>
        </div>

        <div v-if="goodsList.length > 0" :class="['goods-grid', `mode-${viewMode}`]">
          <div v-for="(item, idx) in goodsList" :key="item.id" class="goods-grid-item" :style="{ animationDelay: `${idx * 0.04}s` }">
            <GoodsCard :goods="item" :show-desc="true" />
          </div>
        </div>
        <div v-else-if="loading" :class="['goods-grid', `mode-${viewMode}`]">
          <div v-for="i in 12" :key="'sk'+i" class="goods-grid-item">
            <GoodsCardSkeleton />
          </div>
        </div>
        <EmptyState v-else icon="📦" title="暂无商品" description="还没有人发布商品，快来成为第一个吧" action-text="发布商品" @action="$router.push('/goods/publish')" />

        <div class="load-more-trigger" ref="loadMoreRef" v-if="goodsList.length > 0 && goodsList.length < total">
          <el-icon class="loading-icon" v-if="loadingMore"><Loading /></el-icon>
          <span v-else>下拉加载更多</span>
        </div>
        <div class="list-end" v-else-if="goodsList.length > 0 && goodsList.length >= total">
          <span>— 已经到底啦 —</span>
        </div>
      </main>
    </div>
    <BackToTop />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsList } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import { Plus, Grid, Loading } from '@element-plus/icons-vue'
import GoodsCard from '@/components/GoodsCard.vue'
import GoodsCardSkeleton from '@/components/GoodsCardSkeleton.vue'
import BackToTop from '@/components/BackToTop.vue'
import EmptyState from '@/components/EmptyState.vue'
import type { GoodsVO } from '@/api/goods'
import type { GoodsCategory } from '@/api/category'
import type { GoodsQueryParams } from '@/types'

const route = useRoute()
const goodsList = ref<GoodsVO[]>([])
const categories = ref<GoodsCategory[]>([])
const keyword = ref('')
const categoryId = ref<number | undefined>(undefined)
const sortBy = ref('latest')
const priceMin = ref<number | undefined>(undefined)
const priceMax = ref<number | undefined>(undefined)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const loadMoreRef = ref<HTMLElement>()
let observer: IntersectionObserver | null = null

const loadData = async (append = false) => {
  if (append) loadingMore.value = true
  else loading.value = true
  try {
    const params: GoodsQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value, status: 'ONLINE' }
    if (keyword.value) params.keyword = keyword.value
    if (categoryId.value) params.categoryId = categoryId.value
    if (sortBy.value) params.sortBy = sortBy.value
    const res = await getGoodsList(params)
    if (append) goodsList.value = [...goodsList.value, ...(res.list || [])]
    else goodsList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const handleSearch = () => { pageNum.value = 1; loadData() }

const setCategory = (id: number | undefined) => {
  categoryId.value = id
  handleSearch()
}

const loadMore = async () => {
  if (loadingMore.value || goodsList.value.length >= total.value) return
  pageNum.value++
  await loadData(true)
}

const setupObserver = () => {
  if (observer) observer.disconnect()
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) loadMore()
  }, { rootMargin: '200px' })
  nextTick(() => {
    if (loadMoreRef.value) observer?.observe(loadMoreRef.value)
  })
}

const loadCategories = async () => { try { categories.value = (await getCategoryList()) || [] } catch (e) { console.error(e) } }

onMounted(() => {
  if (route.query.keyword) keyword.value = route.query.keyword as string
  if (route.query.categoryId) categoryId.value = Number(route.query.categoryId)
  loadData(); loadCategories()
  setupObserver()
})

onUnmounted(() => { observer?.disconnect() })
</script>

<style scoped lang="scss">
.goods-list-page { padding: var(--spacing-lg); max-width: 1280px; margin: 0 auto; }

.list-layout { display: grid; grid-template-columns: 220px 1fr; gap: 20px; align-items: start; }

.filter-sidebar {
  position: sticky; top: 80px;
  background: var(--bg-glass);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  display: flex; flex-direction: column; gap: 20px;
}

.filter-section { display: flex; flex-direction: column; gap: 10px; }
.filter-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }

.filter-categories { display: flex; flex-direction: column; gap: 4px; }
.filter-cat-item {
  padding: 8px 12px; border-radius: var(--radius-sm);
  font-size: 14px; color: var(--text-secondary); cursor: pointer;
  transition: var(--transition-fast);
  &:hover { background: var(--primary-lighter); color: var(--primary); }
  &.active { background: var(--primary-gradient); color: #fff; font-weight: 600; }
}

.price-range { display: flex; align-items: center; gap: 8px; }
.price-sep { color: var(--text-muted); }
:deep(.price-range .el-input-number) { width: 80px; }

.sort-group { display: flex; flex-direction: column; gap: 8px; }
:deep(.sort-group .el-radio) { margin-right: 0; height: auto; }

.publish-btn { margin-top: 8px; }

.goods-main {
  background: var(--bg-glass);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  min-height: 400px;
}

.goods-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--border);
}
.result-count { font-size: 14px; color: var(--text-secondary); strong { color: var(--primary); font-size: 16px; } }
.view-toggle { display: flex; gap: 4px; }
.view-btn {
  width: 32px; height: 32px; border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); cursor: pointer; transition: var(--transition-fast);
  &:hover { color: var(--primary); background: var(--primary-lighter); }
  &.active { color: var(--primary); background: var(--primary-lighter); }
}

.goods-grid { display: grid; gap: 16px; }
.goods-grid.mode-grid { grid-template-columns: repeat(4, 1fr); }
.goods-grid.mode-list { grid-template-columns: 1fr; }
.goods-grid-item { min-width: 0; animation: fadeInUp 0.4s ease-out backwards; }

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.load-more-trigger {
  text-align: center; padding: 24px; color: var(--text-muted); font-size: 14px;
  .loading-icon { animation: spin 1s linear infinite; font-size: 20px; color: var(--primary); }
}
@keyframes spin { from { transform: rotate(0); } to { transform: rotate(360deg); } }

.list-end { text-align: center; padding: 20px; color: var(--text-muted); font-size: 13px; }

@media (max-width: 900px) {
  .list-layout { grid-template-columns: 1fr; }
  .filter-sidebar { position: static; flex-direction: row; flex-wrap: wrap; gap: 12px; }
  .filter-section { flex: 1; min-width: 140px; }
  .goods-grid.mode-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 600px) {
  .goods-list-page { padding: var(--spacing-md); }
  .goods-grid.mode-grid { grid-template-columns: repeat(2, 1fr); }
  .filter-sidebar { flex-direction: column; }
  .filter-section { flex: none; }
}
</style>

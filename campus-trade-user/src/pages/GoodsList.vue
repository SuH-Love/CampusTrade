<template>
  <div class="goods-list-page page-bg">
    <div class="mobile-filter-bar">
      <el-input v-model="keyword" placeholder="搜索商品..." clearable @keyup.enter="handleSearch" @input="handleSearchInput" @clear="handleSearch" prefix-icon="Search" size="small" class="mobile-search" />
      <el-button @click="showFilters = !showFilters" size="small" round>
        筛选<el-icon style="margin-left:4px"><Filter /></el-icon>
      </el-button>
    </div>
    <div class="list-layout">
      <aside class="filter-sidebar" :class="{ 'mobile-show': showFilters }">
        <div class="filter-section">
          <h4 class="filter-title">搜索</h4>
          <el-input v-model="keyword" placeholder="输入关键词..." clearable @keyup.enter="handleSearch" @input="handleSearchInput" @clear="handleSearch" prefix-icon="Search" />
        </div>
        <div class="filter-section filter-section-category">
          <h4 class="filter-title">分类</h4>
          <div class="filter-categories">
            <div class="filter-cat-item" :class="{ active: !categoryId }" @click="setCategory(undefined)">全部</div>
            <div v-for="cat in categories" :key="cat.id" class="filter-cat-item" :class="{ active: categoryId === cat.id, 'cat-empty': !cat.goodsCount }" @click="setCategory(cat.id)">
              {{ cat.categoryName }}
              <span v-if="cat.goodsCount" class="cat-count">{{ cat.goodsCount }}</span>
            </div>
          </div>
        </div>
        <div class="filter-section">
          <h4 class="filter-title">价格区间</h4>
          <div class="price-range">
            <el-input-number v-model="priceMin" :min="0" :controls="false" placeholder="最低" size="small" @change="handleSearch" />
            <span class="price-sep">—</span>
            <el-input-number v-model="priceMax" :min="0" :controls="false" placeholder="最高" size="small" @change="handleSearch" />
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
        <el-button round class="reset-btn" @click="resetFilters">
          <el-icon><RefreshLeft /></el-icon> 重置筛选
        </el-button>
        <el-button type="primary" round class="publish-btn" @click="$router.push('/goods/publish')">
          <el-icon><Plus /></el-icon> 发布商品
        </el-button>
        <el-button class="mobile-close-btn" @click="showFilters = false">收起筛选</el-button>
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
          <div v-for="(item, idx) in goodsList" :key="item.id" class="goods-grid-item" :style="idx < 12 ? { animationDelay: `${idx * 0.04}s` } : undefined">
            <GoodsCard :goods="item" :show-desc="true" />
          </div>
        </div>
        <div v-else-if="loading" :class="['goods-grid', `mode-${viewMode}`]">
          <div v-for="i in 12" :key="'sk'+i" class="goods-grid-item">
            <GoodsCardSkeleton />
          </div>
        </div>
        <EmptyState v-else-if="categoryId" icon="📦" title="该分类暂无商品" description="换个分类看看吧，或者查看全部商品" action-text="查看全部商品" @action="setCategory(undefined)" />
        <template v-else>
          <EmptyState icon="🔍" :title="searchEmptyTitle" :description="keyword ? '换个关键词试试，或者看看下面推荐' : '还没有人发布商品，快来成为第一个吧'" :action-text="keyword ? '查看全部商品' : '发布商品'" @action="keyword ? setCategory(undefined) : $router.push('/goods/publish')" />
          <div v-if="keyword && recommendGoods.length > 0" class="guess-section">
            <h4 class="guess-title">猜你想找</h4>
            <div class="goods-grid mode-grid">
              <div v-for="item in recommendGoods" :key="item.id" class="goods-grid-item">
                <GoodsCard :goods="item" />
              </div>
            </div>
          </div>
        </template>

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
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsList, getRecommendGoods } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import { Plus, Grid, Loading, Filter, RefreshLeft, List } from '@element-plus/icons-vue'
import GoodsCard from '@/components/GoodsCard.vue'
import GoodsCardSkeleton from '@/components/GoodsCardSkeleton.vue'
import BackToTop from '@/components/BackToTop.vue'
import EmptyState from '@/components/EmptyState.vue'
import { debounce } from '@/utils/labels'
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
const recommendGoods = ref<GoodsVO[]>([])
const searchEmptyTitle = computed(() =>
  keyword.value ? '没有找到「' + keyword.value + '」相关商品' : '暂无商品'
)
const viewMode = ref<'grid' | 'list'>('grid')
const showFilters = ref(false)
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
    if (priceMin.value != null && priceMin.value > 0) params.minPrice = priceMin.value
    if (priceMax.value != null && priceMax.value > 0) params.maxPrice = priceMax.value
    const res = await getGoodsList(params)
    if (append) goodsList.value = [...goodsList.value, ...(res.list || [])]
    else goodsList.value = res.list || []
    total.value = res.total || 0
    if (!append && goodsList.value.length === 0 && keyword.value) {
      try { const rec = await getRecommendGoods(); recommendGoods.value = (rec.list || []).slice(0, 10) } catch (e) { console.error(e) }
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const handleSearch = () => { pageNum.value = 1; loadData() }
const handleSearchInput = debounce(() => { pageNum.value = 1; loadData() }, 500)

let priceTimer: ReturnType<typeof setTimeout> | null = null
watch([priceMin, priceMax], () => {
  if (priceTimer) clearTimeout(priceTimer)
  priceTimer = setTimeout(handleSearch, 500)
})

const resetFilters = () => {
  keyword.value = ''
  categoryId.value = undefined
  priceMin.value = undefined
  priceMax.value = undefined
  sortBy.value = 'latest'
  handleSearch()
}

const setCategory = (id: number | undefined) => {
  categoryId.value = id
  showFilters.value = false
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

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    const list = res || []
    const withCount = list.filter(c => c.goodsCount).sort((a, b) => (b.goodsCount || 0) - (a.goodsCount || 0))
    const withoutCount = list.filter(c => !c.goodsCount).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    categories.value = [...withCount, ...withoutCount]
  } catch (e) { console.error(e) }
}

onMounted(() => {
  const searchKw = sessionStorage.getItem('goodsSearchKeyword')
  if (searchKw) { keyword.value = searchKw; sessionStorage.removeItem('goodsSearchKeyword') }
  if (route.query.categoryId) categoryId.value = Number(route.query.categoryId)
  loadData(); loadCategories()
  setupObserver()
})

onUnmounted(() => { observer?.disconnect() })
</script>

<style scoped lang="scss">
.goods-list-page { padding: 20px; }

.mobile-filter-bar { display: none; }

.list-layout { display: grid; grid-template-columns: 220px 1fr; gap: 20px; align-items: start; }

.filter-sidebar {
  position: sticky; top: 84px;
  height: calc(100vh - 104px);
  overflow: hidden;
  background: var(--bg-glass);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  display: flex; flex-direction: column; gap: 20px;
}

.filter-section { display: flex; flex-direction: column; gap: 10px; flex-shrink: 0; }
.filter-section-category { flex: 1; min-height: 0; flex-shrink: 1; }
.filter-title { font-size: 13px; font-weight: 600; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; flex-shrink: 0; }

.filter-categories {
  display: flex; flex-direction: column; gap: 4px;
  flex: 1; min-height: 0; max-height: 440px; overflow-y: auto; padding-right: 4px;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: var(--border); border-radius: 2px; }
  &::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }
}
.filter-cat-item {
  padding: 8px 12px; border-radius: var(--radius-sm);
  font-size: 14px; color: var(--text-secondary); cursor: pointer;
  transition: var(--transition-fast);
  display: flex; align-items: center; gap: 4px;
  &:hover { background: var(--primary-lighter); color: var(--primary); }
  &.active { background: var(--primary-gradient); color: #fff; font-weight: 600; .cat-count { background: rgba(255,255,255,0.3); color: #fff; } }
  &.cat-empty { opacity: 0.5; }
}
.cat-count { font-size: 11px; background: var(--bg-hover); color: var(--text-muted); padding: 1px 6px; border-radius: 10px; font-weight: 600; }

.price-range { display: flex; align-items: center; gap: 8px; }
.price-sep { color: var(--text-muted); }
:deep(.price-range .el-input-number) { width: 80px; }

.sort-group { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
:deep(.sort-group .el-radio) { margin-right: 0; height: auto; }

.reset-btn { width: 100%; margin-top: 8px; margin-left: 0 !important; flex-shrink: 0; }
.publish-btn { width: 100%; margin-top: 8px; margin-left: 0 !important; flex-shrink: 0; }
.mobile-close-btn { display: none; }

.goods-main {
  background: var(--bg-glass);

  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: var(--shadow-sm);
  height: calc(100vh - 104px);
  overflow-y: auto;
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }
  &::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }
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
.goods-grid-item { min-width: 0; animation: fadeInUp 0.4s ease-out backwards; contain: layout style; }

.goods-grid.mode-list :deep(.goods-card) { display: flex; flex-direction: row; }
.goods-grid.mode-list :deep(.goods-img-wrap) { width: 200px; padding-top: 0; height: 160px; flex-shrink: 0; }
.goods-grid.mode-list :deep(.goods-info) { flex: 1; padding: 16px; }
.goods-grid.mode-list :deep(.goods-desc) { -webkit-line-clamp: 3; }

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

.guess-section { margin-top: 24px; }
.guess-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin-bottom: 16px; }

@media (max-width: 900px) {
  .mobile-filter-bar {
    display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
    .mobile-search { flex: 1; }
  }
  .list-layout { grid-template-columns: 1fr; gap: 16px; }
  .filter-sidebar {
    position: static; max-height: none; overflow-y: visible;
    display: none;
    &.mobile-show { display: flex; flex-direction: column; gap: 16px; }
  }
  .mobile-close-btn { display: flex; margin-top: 4px; }
  .filter-section { flex: none; }
  .filter-categories { max-height: 240px; overflow-y: auto; }
  .goods-grid.mode-grid { grid-template-columns: repeat(3, 1fr); }
  .goods-main { height: auto; max-height: none; overflow-y: visible; }
}
@media (max-width: 600px) {
  .goods-list-page { padding: var(--spacing-md); }
  .goods-grid.mode-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

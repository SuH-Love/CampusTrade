<template>
  <div class="favorites-page page-bg">
    <el-card>
      <template #header>
        <div class="favorites-header">
          <h3 class="favorites-title">我的收藏</h3>
          <div class="filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索商品标题" clearable class="search-input" @clear="handleSearch" />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="status-select">
              <el-option label="在售" value="ONLINE" />
              <el-option label="已下架" value="OFFLINE" />
              <el-option label="已售出" value="SOLD" />
            </el-select>
          </div>
        </div>
      </template>
      <TransitionGroup name="list" tag="div" class="favorites-grid" v-loading="loading" @before-enter="onBeforeEnter" @enter="onEnter">
        <div v-for="(item, idx) in goodsList" :key="item.id" class="favorites-grid-item" :data-idx="idx">
          <GoodsCard :goods="item" :clickable="item.status === 'ONLINE'" :show-meta="true" :show-unfav="true" @unfavorite="handleUnfavorite" />
        </div>
      </TransitionGroup>
      <EmptyState v-if="goodsList.length === 0 && !loading" icon="❤️" title="暂无收藏" description="去逛逛商品列表，收藏你喜欢的宝贝吧" action-text="去逛逛" @action="$router.push('/goods')" />
      <el-pagination v-if="total > pageSize" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getFavoriteList, unfavoriteGoods } from '@/api/goods'
import GoodsCard from '@/components/GoodsCard.vue'
import EmptyState from '@/components/EmptyState.vue'
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

const loadData = async () => {
  loading.value = true
  try {
    const params: GoodsQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getFavoriteList(params)
    goodsList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchKeyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { pageNum.value = 1; loadData() }, 300)
})

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleUnfavorite = async (id: number) => {
  await unfavoriteGoods(id)
  ElMessage.success('已取消收藏')
  loadData()
}

const onBeforeEnter = (el: Element) => {
  ;(el as HTMLElement).style.opacity = '0'
  ;(el as HTMLElement).style.transform = 'translateY(20px)'
}
const onEnter = (el: Element, done: () => void) => {
  const htmlEl = el as HTMLElement
  const idx = Number(htmlEl.dataset.idx || 0)
  const delay = Math.min(idx * 50, 400)
  setTimeout(() => {
    htmlEl.style.transition = 'all 0.35s ease-out'
    htmlEl.style.opacity = '1'
    htmlEl.style.transform = 'translateY(0)'
    setTimeout(done, 350)
  }, delay)
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.favorites-page {
  padding: 20px;

  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid var(--border);
    box-shadow: var(--shadow-sm);
  }
}
.favorites-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.favorites-title { margin: 0; font-size: 18px; font-weight: 700; color: var(--text-primary); }
.filter-bar { display: flex; gap: 12px; align-items: center; }
.search-input { width: 200px; }
.status-select { width: 140px; }
.favorites-grid { margin-top: 8px; display: grid; grid-template-columns: repeat(6, 1fr); gap: 16px; }
.favorites-grid-item { min-width: 0; }
.list-pagination { margin-top: 20px; justify-content: center; }

@media (max-width: 1200px) { .favorites-grid { grid-template-columns: repeat(5, 1fr); } }
@media (max-width: 900px) { .favorites-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 600px) { .favorites-grid { grid-template-columns: repeat(2, 1fr); } }


@media (max-width: 576px) {
  .favorites-page { padding: 12px; }

  .favorites-header { flex-direction: column; align-items: flex-start; }
  .filter-bar { width: 100%; flex-wrap: wrap; }
  .search-input { width: 100%; }
  .status-select { width: 100%; }
}
</style>

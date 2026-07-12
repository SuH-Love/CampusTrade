<template>
  <div class="favorites-page page-bg">
    <div class="favorites-inner">
      <div class="favorites-header">
        <h3 class="favorites-title">我的收藏</h3>
        <div class="filter-bar">
          <el-input v-model="searchKeyword" placeholder="搜索商品标题" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="status-select">
            <el-option label="在售" value="ONLINE" />
            <el-option label="已下架" value="OFFLINE" />
            <el-option label="已售出" value="SOLD" />
          </el-select>
        </div>
      </div>
      <el-row :gutter="16" v-loading="loading" class="favorites-grid">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in filteredGoods" :key="item.id">
          <GoodsCard :goods="item" :clickable="item.status === 'ONLINE'" :show-meta="true" :show-unfav="true" @unfavorite="handleUnfavorite" />
        </el-col>
      </el-row>
      <EmptyState v-if="filteredGoods.length === 0 && !loading" icon="❤️" title="暂无收藏" description="去逛逛商品列表，收藏你喜欢的宝贝吧" action-text="去逛逛" @action="$router.push('/goods')" />
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
.favorites-inner {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}
.favorites-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.favorites-title { margin: 0; }
.filter-bar { display: flex; gap: 12px; align-items: center; }
.search-input { width: 200px; }
.status-select { width: 140px; }
.favorites-grid { margin-top: 8px; }
.list-pagination { margin-top: 20px; justify-content: center; }

@media (max-width: 576px) {
  .favorites-page { padding: 12px; }
  .favorites-inner { padding: 16px; }
  .favorites-header { flex-direction: column; align-items: flex-start; }
  .filter-bar { width: 100%; flex-wrap: wrap; }
  .search-input { width: 100%; }
  .status-select { width: 100%; }
}
</style>

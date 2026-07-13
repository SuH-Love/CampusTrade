<template>
  <div class="goods-list">
    <div class="goods-list-inner">
    <div class="search-bar">
      <el-row :gutter="12" align="middle">
        <el-col :xs="24" :sm="10" :md="8">
          <el-input v-model="keyword" placeholder="搜索你想要的宝贝..." clearable @keyup.enter="handleSearch" size="large" prefix-icon="Search" />
        </el-col>
        <el-col :xs="12" :sm="6" :md="5">
          <el-select v-model="categoryId" placeholder="全部分类" clearable @change="handleSearch" size="large" class="full-width">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="4" :md="4">
          <el-select v-model="sortBy" placeholder="排序" @change="handleSearch" size="large" class="full-width">
            <el-option label="最新发布" value="latest" />
            <el-option label="价格最低" value="price_asc" />
            <el-option label="价格最高" value="price_desc" />
            <el-option label="最多浏览" value="views" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="4" :md="7" class="publish-col">
          <el-button type="primary" size="large" round @click="$router.push('/goods/publish')">发布商品</el-button>
        </el-col>
      </el-row>
    </div>
    <el-row :gutter="16" v-if="goodsList.length > 0" class="goods-grid">
      <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
        <GoodsCard :goods="item" :show-desc="true" />
      </el-col>
    </el-row>
    <el-row :gutter="16" v-else-if="loading" class="goods-grid">
      <el-col :xs="12" :sm="8" :md="6" v-for="i in 12" :key="'sk'+i">
        <GoodsCardSkeleton />
      </el-col>
    </el-row>
    <EmptyState v-else icon="📦" title="暂无商品" description="还没有人发布商品，快来成为第一个吧" action-text="发布商品" @action="$router.push('/goods/publish')" />
    <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </div>
    <BackToTop />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsList } from '@/api/goods'
import { getCategoryList } from '@/api/category'
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
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const params: GoodsQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value, status: 'ONLINE' }
    if (keyword.value) params.keyword = keyword.value
    if (categoryId.value) params.categoryId = categoryId.value
    if (sortBy.value) params.sortBy = sortBy.value
    const res = await getGoodsList(params)
    goodsList.value = res.list || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const loadCategories = async () => { try { categories.value = (await getCategoryList()) || [] } catch (e) { console.error(e) } }
const handleSearch = () => { pageNum.value = 1; loadData() }
onMounted(() => {
  if (route.query.keyword) keyword.value = route.query.keyword as string
  if (route.query.categoryId) categoryId.value = Number(route.query.categoryId)
  loadData(); loadCategories()
})
</script>

<style scoped lang="scss">
.goods-list { padding: 20px; }
.goods-list-inner {
  background: var(--bg-glass);

  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}
.full-width { width: 100%; }
.publish-col { text-align: right; }
.goods-grid { margin-top: 20px; }
.list-pagination { margin-top: 20px; justify-content: center; }

@media (max-width: 576px) {
  .goods-list { padding: 12px; }
  .goods-list-inner { padding: 16px; }
  .publish-col { text-align: left; margin-top: 8px; }
}
</style>

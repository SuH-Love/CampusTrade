<template>
  <div class="goods-list page-container">
    <div class="search-bar">
      <el-row :gutter="12" align="middle">
        <el-col :xs="24" :sm="10" :md="8">
          <el-input v-model="keyword" placeholder="搜索你想要的宝贝..." clearable @keyup.enter="handleSearch" size="large" prefix-icon="Search" />
        </el-col>
        <el-col :xs="12" :sm="6" :md="5">
          <el-select v-model="categoryId" placeholder="全部分类" clearable @change="handleSearch" size="large" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </el-col>
        <el-col :xs="12" :sm="4" :md="4">
          <el-select v-model="sortBy" placeholder="排序" @change="handleSearch" size="large" style="width: 100%">
            <el-option label="最新发布" value="latest" />
            <el-option label="价格最低" value="price_asc" />
            <el-option label="价格最高" value="price_desc" />
            <el-option label="最多浏览" value="views" />
          </el-select>
        </el-col>
        <el-col :xs="24" :sm="4" :md="7" style="text-align: right">
          <el-button type="primary" size="large" round @click="$router.push('/goods/publish')">发布商品</el-button>
        </el-col>
      </el-row>
    </div>
    <el-row :gutter="16" v-loading="loading" style="margin-top: 20px">
      <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
        <div class="goods-card" @click="$router.push(`/goods/${item.id}`)">
          <div class="goods-img-wrap">
            <img :src="item.coverImage || '/placeholder.png'" class="goods-img" />
            <span class="goods-category-tag">{{ item.categoryName || getCategoryName(item.categoryId) }}</span>
          </div>
          <div class="goods-info">
            <div class="goods-title">{{ item.title }}</div>
            <div class="goods-price-row">
              <span class="price-text">¥{{ item.price }}</span>
              <span v-if="item.originalPrice" class="original-price">¥{{ item.originalPrice }}</span>
            </div>
            <div class="goods-meta">{{ item.viewCount }} 浏览</div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
    <div style="text-align: center; margin-top: 24px">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGoodsList } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import type { GoodsVO } from '@/api/goods'
import type { GoodsCategory } from '@/api/category'
import type { GoodsQueryParams } from '@/types'

const goodsList = ref<GoodsVO[]>([])
const categories = ref<GoodsCategory[]>([])
const keyword = ref('')
const categoryId = ref<number | undefined>(undefined)
const sortBy = ref('latest')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)

const getCategoryName = (id: number) => { const cat = categories.value.find(c => c.id === id); return cat ? cat.categoryName : '' }

const loadData = async () => {
  loading.value = true
  try {
    const params: GoodsQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value, status: 'ONLINE' }
    if (keyword.value) params.keyword = keyword.value
    if (categoryId.value) params.categoryId = categoryId.value
    const res = await getGoodsList(params)
    goodsList.value = res.list || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const loadCategories = async () => { try { categories.value = (await getCategoryList()) || [] } catch { /* ignore */ } }
const handleSearch = () => { pageNum.value = 1; loadData() }
onMounted(() => { loadData(); loadCategories() })
</script>

<style scoped lang="scss">
.search-bar {
  background: var(--bg-card);
  padding: 20px 24px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
}

.goods-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden; cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border);
  margin-bottom: 16px;
  &:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
}

.goods-img-wrap { position: relative; padding-top: 75%; overflow: hidden; background: #f1f5f9; }
.goods-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s ease; .goods-card:hover & { transform: scale(1.05); } }
.goods-category-tag { position: absolute; top: 8px; left: 8px; background: rgba(0,0,0,0.5); color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.goods-info { padding: 12px; }
.goods-title { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-price-row { display: flex; align-items: baseline; gap: 8px; margin-top: 8px; }
.original-price { font-size: 12px; color: var(--text-muted); text-decoration: line-through; }
.goods-meta { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
</style>

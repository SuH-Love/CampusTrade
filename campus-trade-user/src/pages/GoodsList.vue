<template>
  <div class="goods-list">
    <div class="goods-list-inner">
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
            <img :src="item.coverImage || '/default-cover.svg'" class="goods-img" />
            <div class="goods-tags">
              <span class="goods-category-tag">{{ item.categoryName || getCategoryName(item.categoryId) }}</span>
              <span v-if="item.condition" class="goods-condition-tag">{{ item.condition }}</span>
              <span v-if="item.originalPrice && item.originalPrice > item.price" class="goods-discount-tag">折扣</span>
            </div>
            <el-avatar v-if="item.userAvatar" :size="28" :src="item.userAvatar" class="goods-seller-avatar" />
          </div>
          <div class="goods-info">
            <div class="goods-title">{{ item.title }}</div>
            <div class="goods-desc" v-if="item.description">{{ item.description }}</div>
            <div class="goods-bottom">
              <div class="goods-price-row">
                <span class="price-text">¥{{ item.price }}</span>
                <span v-if="item.originalPrice && item.originalPrice > item.price" class="original-price">¥{{ item.originalPrice }}</span>
              </div>
              <span class="goods-views">{{ item.viewCount }} 浏览</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
    <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getGoodsList } from '@/api/goods'
import { getCategoryList } from '@/api/category'
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
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}

.goods-card {
  background: var(--bg-card);
  border-radius: 14px;
  overflow: hidden; cursor: pointer;
  transition: var(--transition);
  border: 1px solid var(--border);
  margin-bottom: 16px;
  &:hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); border-color: var(--primary-lighter); }
}

.goods-img-wrap { position: relative; padding-top: 75%; overflow: hidden; background: linear-gradient(135deg, #f1f5f9, #e2e8f0); }
.goods-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1); .goods-card:hover & { transform: scale(1.08); } }
.goods-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  z-index: 2;
}
.goods-category-tag { background: rgba(0,0,0,0.55); backdrop-filter: blur(8px); color: #fff; font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 10px; }
.goods-condition-tag {
  background: rgba(234, 179, 8, 0.85); backdrop-filter: blur(6px);
  color: #fff; font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 10px;
}
.goods-discount-tag {
  background: rgba(239, 68, 68, 0.85); backdrop-filter: blur(6px);
  color: #fff; font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 10px;
}
.goods-seller-avatar {
  position: absolute; bottom: 10px; right: 10px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.goods-info { padding: 14px 14px 16px; }
.goods-title { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-desc { font-size: 12px; color: var(--text-muted); margin-top: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.goods-price-row { display: flex; align-items: baseline; gap: 8px; }
.original-price { font-size: 12px; color: var(--text-muted); text-decoration: line-through; }
.goods-views { font-size: 12px; color: var(--text-muted); font-weight: 500; }
</style>

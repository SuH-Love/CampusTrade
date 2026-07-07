<template>
  <div class="goods-list">
    <el-card style="margin-bottom: 16px">
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索商品..." clearable @keyup.enter="handleSearch" />
        </el-col>
        <el-col :span="6">
          <el-select v-model="categoryId" placeholder="全部分类" clearable @change="handleSearch" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="sortBy" placeholder="排序" @change="handleSearch" style="width: 100%">
            <el-option label="最新发布" value="latest" />
            <el-option label="价格最低" value="price_asc" />
            <el-option label="价格最高" value="price_desc" />
            <el-option label="最多浏览" value="views" />
          </el-select>
        </el-col>
        <el-col :span="2">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </el-col>
        <el-col :span="4" style="text-align: right">
          <el-button type="success" @click="$router.push('/goods/publish')">发布商品</el-button>
        </el-col>
      </el-row>
    </el-card>
    <el-row :gutter="16">
      <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
        <el-card shadow="hover" @click="$router.push(`/goods/${item.id}`)" style="margin-bottom: 16px; cursor: pointer">
          <img :src="item.coverImage || '/placeholder.png'" style="width: 100%; height: 160px; object-fit: cover; border-radius: 4px" />
          <div style="padding: 8px 0">
            <div class="title">{{ item.title }}</div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px">
              <span style="color: #f56c6c; font-weight: bold; font-size: 18px">¥{{ item.price }}</span>
              <span v-if="item.originalPrice" style="text-decoration: line-through; color: #999; font-size: 12px">¥{{ item.originalPrice }}</span>
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px; color: #999; font-size: 12px">
              <span>{{ item.categoryName }}</span>
              <span>{{ item.viewCount }}次浏览</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="goodsList.length === 0" description="暂无商品" />
    <div style="text-align: center; margin-top: 16px">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGoodsList } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import type { GoodsVO } from '@/api/goods'

const goodsList = ref<GoodsVO[]>([])
const categories = ref<any[]>([])
const keyword = ref('')
const categoryId = ref<number | undefined>(undefined)
const sortBy = ref('latest')
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

const loadData = async () => {
  const params: any = { pageNum: pageNum.value, pageSize: pageSize.value, status: 'ONLINE' }
  if (keyword.value) params.keyword = keyword.value
  if (categoryId.value) params.categoryId = categoryId.value
  if (sortBy.value) params.sortBy = sortBy.value
  const res = await getGoodsList(params)
  goodsList.value = res.list || []
  total.value = res.total || 0
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res || []
  } catch { /* ignore */ }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

onMounted(() => { loadData(); loadCategories() })
</script>

<style scoped lang="scss">
.goods-list { padding: 20px; }
.title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
</style>

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
              <img :src="item.coverImage || '/default-cover.svg'" class="fav-img" />
              <div v-if="item.status === 'SOLD'" class="sold-overlay">
                <el-tag type="info" effect="dark" size="large">已售出</el-tag>
              </div>
              <div v-else-if="item.status === 'OFFLINE'" class="sold-overlay">
                <el-tag type="warning" effect="dark" size="large">已下架</el-tag>
              </div>
            </div>
            <div class="fav-info">
              <div class="fav-title">{{ item.title }}</div>
              <div class="fav-bottom">
                <span class="price-text">¥{{ item.price }}</span>
                <el-button type="warning" size="small" text @click.stop="handleUnfavorite(item.id)">取消收藏</el-button>
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
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border);
  margin-bottom: 16px;
  transition: var(--transition);
  &:not(.sold):hover { transform: translateY(-6px); box-shadow: var(--shadow-lg); border-color: var(--primary-lighter); }
  &.sold { opacity: 0.75; }
}
.fav-img-wrap { position: relative; padding-top: 75%; background: linear-gradient(135deg, #f1f5f9, #e2e8f0); cursor: pointer; &.no-click { cursor: not-allowed; } }
.fav-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; }
.sold-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(2px);
  display: flex; align-items: center; justify-content: center;
}
.fav-info { padding: 14px 14px 16px; }
.fav-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 600; font-size: 14px; }
.fav-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
</style>

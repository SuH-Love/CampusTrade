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
          <GoodsCard :goods="item" :clickable="item.status === 'ONLINE'" :show-meta="true" :show-unfav="true" @unfavorite="handleUnfavorite" />
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
import GoodsCard from '@/components/GoodsCard.vue'
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

</style>

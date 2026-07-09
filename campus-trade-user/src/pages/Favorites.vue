<template>
  <div class="favorites-page">
    <el-card>
      <template #header><h3 style="margin: 0">我的收藏</h3></template>
      <el-row :gutter="16" v-loading="loading">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
          <div class="fav-card" :class="{ sold: item.status === 'SOLD' }">
            <div class="fav-img-wrap" @click="item.status !== 'SOLD' && $router.push(`/goods/${item.id}`)">
              <img :src="item.coverImage || '/placeholder.png'" class="fav-img" />
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
      <el-empty v-if="goodsList.length === 0" description="暂无收藏" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFavoriteList, unfavoriteGoods } from '@/api/goods'
import { ElMessage } from 'element-plus'
import type { GoodsVO } from '@/api/goods'

const goodsList = ref<GoodsVO[]>([])
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFavoriteList({ pageNum: pageNum.value, pageSize: pageSize.value })
    goodsList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
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
.fav-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border);
  margin-bottom: 16px;
  transition: var(--transition);
  &:not(.sold):hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }
  &.sold { opacity: 0.75; }
}
.fav-img-wrap { position: relative; padding-top: 75%; background: #f1f5f9; cursor: pointer; }
.fav-img { position: absolute; top: 0; left: 0; width: 100%; height: 100%; object-fit: cover; }
.sold-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
}
.fav-info { padding: 12px; }
.fav-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; font-size: 14px; }
.fav-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
</style>

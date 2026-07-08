<template>
  <div class="favorites-page">
    <el-card>
      <template #header><h3 style="margin: 0">我的收藏</h3></template>
      <el-row :gutter="16">
        <el-col :xs="12" :sm="8" :md="6" v-for="item in goodsList" :key="item.id">
          <el-card shadow="hover" @click="$router.push(`/goods/${item.id}`)" style="margin-bottom: 16px; cursor: pointer">
            <img :src="item.coverImage || '/placeholder.png'" style="width: 100%; height: 160px; object-fit: cover; border-radius: 4px" />
            <div style="padding: 8px 0">
              <div class="title">{{ item.title }}</div>
              <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 6px">
                <span style="color: #f56c6c; font-weight: bold; font-size: 18px">¥{{ item.price }}</span>
                <el-button type="warning" size="small" text @click.stop="handleUnfavorite(item.id)">取消收藏</el-button>
              </div>
            </div>
          </el-card>
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

const loadData = async () => {
  const res = await getFavoriteList({ pageNum: pageNum.value, pageSize: pageSize.value })
  goodsList.value = res.list || []
  total.value = res.total || 0
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
.title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
</style>
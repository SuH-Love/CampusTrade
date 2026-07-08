<template>
  <div class="home-page">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-carousel height="300px">
          <el-carousel-item v-for="i in 3" :key="i">
            <div class="banner" :style="{ background: `hsl(${i * 120}, 70%, 60%)` }">
              <h2>CampusTrade 校园二手交易平台</h2>
              <p>安全、便捷、值得信赖</p>
            </div>
          </el-carousel-item>
        </el-carousel>
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
          <h3 style="margin: 0">热门商品</h3>
          <el-select v-model="selectedCategoryId" placeholder="全部分类" clearable @change="loadHotGoods" style="width: 180px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
          </el-select>
        </div>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in hotGoods" :key="item.id">
            <el-card shadow="hover" @click="$router.push(`/goods/${item.id}`)" style="margin-bottom: 16px; cursor: pointer">
              <img :src="item.coverImage || '/placeholder.png'" style="width: 100%; height: 160px; object-fit: cover" />
              <div style="padding: 8px 0">
                <div class="goods-title">{{ item.title }}</div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 4px">
                  <span style="color: #f56c6c; font-weight: bold">¥{{ item.price }}</span>
                  <span style="color: #999; font-size: 12px">{{ item.categoryName }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="hotGoods.length === 0" description="暂无热门商品" />
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <h3>推荐商品</h3>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in recommendGoods" :key="item.id">
            <el-card shadow="hover" @click="$router.push(`/goods/${item.id}`)" style="margin-bottom: 16px; cursor: pointer">
              <img :src="item.coverImage || '/placeholder.png'" style="width: 100%; height: 160px; object-fit: cover" />
              <div style="padding: 8px 0">
                <div class="goods-title">{{ item.title }}</div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 4px">
                  <span style="color: #f56c6c; font-weight: bold">¥{{ item.price }}</span>
                  <span style="color: #999; font-size: 12px">{{ item.categoryName }}</span>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHotGoods, getRecommendGoods } from '@/api/goods'
import { getCategoryList } from '@/api/category'
import type { GoodsVO } from '@/api/goods'
import type { GoodsCategory } from '@/api/category'

const hotGoods = ref<GoodsVO[]>([])
const recommendGoods = ref<GoodsVO[]>([])
const categories = ref<GoodsCategory[]>([])
const selectedCategoryId = ref<number | undefined>(undefined)

const loadHotGoods = async () => {
  try {
    const res = await getHotGoods()
    let list = res.list || []
    if (selectedCategoryId.value) {
      list = list.filter((g: GoodsVO) => g.categoryId === selectedCategoryId.value)
    }
    hotGoods.value = list
  } catch { /* ignore */ }
}

const loadRecommendGoods = async () => {
  try {
    const res = await getRecommendGoods()
    recommendGoods.value = res.list || []
  } catch { /* ignore */ }
}

const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res || []
  } catch { /* ignore */ }
}

onMounted(() => { loadHotGoods(); loadRecommendGoods(); loadCategories() })
</script>

<style scoped lang="scss">
.banner {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  h2 { font-size: 28px; margin-bottom: 10px; }
  p { font-size: 16px; }
}
.goods-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
</style>

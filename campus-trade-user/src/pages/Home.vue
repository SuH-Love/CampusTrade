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
        <h3>热门商品</h3>
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6" v-for="item in hotGoods" :key="item.id">
            <el-card shadow="hover" @click="$router.push(`/goods/${item.id}`)" style="margin-bottom: 16px; cursor: pointer">
              <img :src="item.coverImage || '/placeholder.png'" style="width: 100%; height: 160px; object-fit: cover" />
              <div style="padding: 8px 0">
                <div class="goods-title">{{ item.title }}</div>
                <div style="color: #f56c6c; font-weight: bold">¥{{ item.price }}</div>
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
import { getHotGoods } from '@/api/goods'
import type { GoodsVO } from '@/api/goods'

const hotGoods = ref<GoodsVO[]>([])

onMounted(async () => {
  const res = await getHotGoods()
  hotGoods.value = res.list || []
})
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
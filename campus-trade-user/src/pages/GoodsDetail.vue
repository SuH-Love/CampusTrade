<template>
  <div v-if="goods" class="goods-detail">
    <el-row :gutter="24">
      <el-col :span="12">
        <el-carousel height="400px" indicator-position="outside" v-if="imageList.length > 0">
          <el-carousel-item v-for="(img, idx) in imageList" :key="idx">
            <el-image :src="img" fit="cover" style="width: 100%; height: 100%; border-radius: 8px" />
          </el-carousel-item>
        </el-carousel>
        <el-image v-else :src="goods.coverImage || '/placeholder.png'" fit="cover" style="width: 100%; height: 400px; border-radius: 8px" />
      </el-col>
      <el-col :span="12">
        <h2>{{ goods.title }}</h2>
        <div style="display: flex; align-items: center; gap: 8px; margin: 8px 0">
          <el-tag>{{ goods.categoryName }}</el-tag>
          <span style="color: #999; font-size: 13px">{{ goods.viewCount }}次浏览 · {{ goods.favoriteCount }}人收藏</span>
        </div>
        <div style="margin: 20px 0; padding: 16px; background: #fff8f0; border-radius: 8px">
          <span style="font-size: 32px; color: #f56c6c; font-weight: bold">¥{{ goods.price }}</span>
          <span v-if="goods.originalPrice" style="text-decoration: line-through; color: #999; margin-left: 12px; font-size: 16px">¥{{ goods.originalPrice}}</span>
          <el-tag v-if="goods.originalPrice > goods.price" type="danger" style="margin-left: 8px">{{ discount }}折</el-tag>
        </div>
        <el-divider />
        <p style="line-height: 1.8; color: #333">{{ goods.description }}</p>
        <el-divider />
        <div class="seller-info" @click="handleChat">
          <el-avatar :size="40" :src="goods.userAvatar" />
          <div style="margin-left: 10px">
            <div style="font-weight: 500">{{ goods.username }}</div>
            <div style="color: #999; font-size: 12px">点击联系卖家</div>
          </div>
        </div>
        <div style="margin-top: 24px; display: flex; gap: 12px">
          <el-button type="primary" size="large" @click="handleBuy" :disabled="!userStore.token || goods.userId === userStore.userInfo?.id">立即购买</el-button>
          <el-button size="large" :type="goods.isFavorited ? 'warning' : 'default'" @click="handleFavorite">
            <el-icon><Star /></el-icon>
            {{ goods.isFavorited ? '已收藏' : '收藏' }}
          </el-button>
          <el-button size="large" @click="handleReport" v-if="userStore.token && goods.userId !== userStore.userInfo?.id">举报</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
  <el-empty v-else description="商品不存在" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, favoriteGoods, unfavoriteGoods } from '@/api/goods'
import { createOrder } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { GoodsVO } from '@/api/goods'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const goods = ref<GoodsVO | null>(null)

const imageList = computed(() => {
  if (!goods.value) return []
  const imgs = goods.value.images ? goods.value.images.split(',').filter(Boolean) : []
  return goods.value.coverImage ? [goods.value.coverImage, ...imgs] : imgs
})

const discount = computed(() => {
  if (!goods.value?.originalPrice || goods.value.originalPrice === 0) return ''
  return (goods.value.price / goods.value.originalPrice * 10).toFixed(1)
})

const loadData = async () => {
  const data = await getGoodsDetail(Number(route.params.id))
  goods.value = data
}

const handleBuy = async () => {
  if (!goods.value) return
  await ElMessageBox.confirm(`确认购买「${goods.value.title}」？价格 ¥${goods.value.price}`, '确认购买')
  await createOrder({ goodsId: goods.value.id })
  ElMessage.success('下单成功')
  router.push('/order')
}

const handleFavorite = async () => {
  if (!goods.value || !userStore.token) { ElMessage.warning('请先登录'); return }
  try {
    if (goods.value.isFavorited) {
      await unfavoriteGoods(goods.value.id)
      goods.value.isFavorited = false
      goods.value.favoriteCount = Math.max(0, goods.value.favoriteCount - 1)
      ElMessage.success('已取消收藏')
    } else {
      await favoriteGoods(goods.value.id)
      goods.value.isFavorited = true
      goods.value.favoriteCount++
      ElMessage.success('已收藏')
    }
  } catch (e: any) {
    if (e?.message?.includes('已收藏')) {
      goods.value.isFavorited = true
    }
  }
}

const handleChat = () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (!goods.value) return
  router.push({ path: '/chat', query: { targetUserId: String(goods.value.userId), name: goods.value.username } })
}

const handleReport = () => {
  router.push({ path: '/report', query: { targetType: '1', targetId: String(route.params.id) } })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.goods-detail { padding: 20px; }
.seller-info { display: flex; align-items: center; cursor: pointer; padding: 12px; border-radius: 8px; &:hover { background: #f5f7fa; } }
</style>

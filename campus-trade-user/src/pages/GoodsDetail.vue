<template>
  <div v-if="goods" class="goods-detail page-container">
    <el-row :gutter="32">
      <el-col :xs="24" :md="12">
        <div class="detail-gallery">
          <el-carousel height="420px" indicator-position="outside" v-if="imageList.length > 1">
            <el-carousel-item v-for="(img, idx) in imageList" :key="idx">
              <el-image :src="img" fit="cover" class="gallery-img" />
            </el-carousel-item>
          </el-carousel>
          <el-image v-else :src="goods.coverImage || '/default-cover.svg'" fit="cover" class="gallery-img single" />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="detail-info">
          <div class="detail-status">
            <el-tag :type="statusTagType(goods.status)" effect="dark" round>{{ statusLabel(goods.status) }}</el-tag>
            <el-tag round>{{ goods.categoryName }}</el-tag>
            <el-tag v-if="goods.condition" type="warning" round>{{ goods.condition }}</el-tag>
          </div>
          <h1 class="detail-title">{{ goods.title }}</h1>
          <div class="detail-stats">
            <span>{{ goods.viewCount }} 浏览</span>
            <span>·</span>
            <span>{{ goods.favoriteCount }} 收藏</span>
          </div>
          <div class="price-box">
            <span class="price-current">¥{{ goods.price }}</span>
            <span v-if="goods.originalPrice && goods.originalPrice > goods.price" class="price-original">¥{{ goods.originalPrice }}</span>
            <el-tag v-if="goods.originalPrice > goods.price" type="danger" effect="dark" round size="small">{{ discount }}折</el-tag>
          </div>
          <div class="detail-desc">
            <h3>商品描述</h3>
            <p>{{ goods.description || '暂无描述' }}</p>
          </div>
          <div class="seller-card" v-if="userStore.token && goods.userId !== userStore.userInfo?.id">
            <el-avatar :size="44" :src="goods.userAvatar" @click="$router.push(`/profile/${goods.userId}`)" style="cursor: pointer" />
            <div class="seller-info" @click="$router.push(`/profile/${goods.userId}`)" style="cursor: pointer">
              <div class="seller-name">{{ goods.username }}</div>
              <div class="seller-action">
                <span>查看主页</span>
                <el-rate v-if="sellerRating > 0" :model-value="sellerRating" disabled size="small" style="margin-left: 8px; vertical-align: middle" />
                <span v-else style="margin-left: 8px; font-size: 12px; color: var(--text-muted)">暂无评价</span>
              </div>
            </div>
            <el-button size="small" @click.stop="handleChat" round>聊天</el-button>
            <el-button :type="isFollowed ? 'warning' : 'default'" size="small" @click.stop="handleToggleFollow" :loading="followLoading" round>
              {{ isFollowed ? '已关注' : '关注' }}
            </el-button>
          </div>
          <div class="action-bar">
            <el-button type="primary" size="large" @click="handleBuy" :loading="buying" :disabled="!userStore.token || goods.userId === userStore.userInfo?.id" round>
              立即购买
            </el-button>
            <el-button size="large" :type="isInCart ? 'success' : 'default'" @click="handleAddToCart" :loading="addingToCart" :disabled="!userStore.token || goods.userId === userStore.userInfo?.id" round>
              {{ isInCart ? '已在购物车' : '加入购物车' }}
            </el-button>
            <el-button size="large" :type="goods.isFavorited ? 'warning' : 'default'" @click="handleFavorite" :loading="favoriting" round>
              <el-icon><Star /></el-icon> {{ goods.isFavorited ? '已收藏' : '收藏' }}
            </el-button>
            <el-button size="large" @click="handleReport" v-if="userStore.token && goods.userId !== userStore.userInfo?.id" round>举报</el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
  <div v-else class="page-container"><el-empty description="商品不存在" /></div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, favoriteGoods, unfavoriteGoods } from '@/api/goods'
import { createOrder } from '@/api/order'
import { addToCart, getCartList, type CartVO } from '@/api/cart'
import { toggleFollow, isFollowing } from '@/api/follow'
import { getAverageRating } from '@/api/rating'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage, ElMessageBox } from 'element-plus'
import { h } from 'vue'
import type { GoodsVO } from '@/api/goods'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const goods = ref<GoodsVO | null>(null)
const buying = ref(false)
const favoriting = ref(false)
const addingToCart = ref(false)
const isFollowed = ref(false)
const followLoading = ref(false)
const sellerRating = ref(0)
const isInCart = ref(false)

const imageList = computed(() => {
  if (!goods.value) return []
  const imgs = goods.value.images ? goods.value.images.split(',').filter(Boolean) : []
  return goods.value.coverImage ? [goods.value.coverImage, ...imgs] : imgs
})

const discount = computed(() => {
  if (!goods.value?.originalPrice || goods.value.originalPrice === 0) return ''
  return (goods.value.price / goods.value.originalPrice * 10).toFixed(1)
})

const statusLabel = (status: string) => {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING: '待审核', APPROVED: '审核通过', REJECTED: '已拒绝', ONLINE: '在售', OFFLINE: '已下架', SOLD: '已售出' }
  return map[status] || status
}

const statusTagType = (status: string) => {
  const map: Record<string, string> = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: 'success', OFFLINE: 'info', SOLD: 'warning' }
  return map[status] || 'info'
}

const loadData = async () => {
  goods.value = await getGoodsDetail(Number(route.params.id))
  if (goods.value && userStore.token && goods.value.userId !== userStore.userInfo?.id) {
    try { isFollowed.value = await isFollowing(goods.value.userId) } catch { /* ignore */ }
    try { sellerRating.value = await getAverageRating(goods.value.userId) } catch { /* ignore */ }
  }
  if (goods.value && userStore.token) {
    try {
      const cartList = await getCartList()
      isInCart.value = cartList ? cartList.some((c: CartVO) => c.goodsId === goods.value!.id) : false
    } catch { /* ignore */ }
  }
}

const handleBuy = async () => {
  if (!goods.value) return
  const deliveryMethod = ref('PICKUP')
  const deliveryAddress = ref('')
  try {
    await ElMessageBox({
      title: '确认购买',
      message: () => h('div', null, [
        h('p', { style: 'margin-bottom: 12px' }, `确认购买「${goods.value!.title}」？价格 ¥${goods.value!.price}`),
        h('div', { style: 'margin-bottom: 12px' }, [
          h('span', { style: 'margin-right: 12px' }, '配送方式：'),
          h('input', {
            type: 'radio', name: 'delivery', value: 'PICKUP', checked: deliveryMethod.value === 'PICKUP',
            style: 'margin-right: 4px', onChange: (e: Event) => { deliveryMethod.value = (e.target as HTMLInputElement).value; deliveryAddress.value = '' }
          }),
          h('span', { style: 'margin-right: 16px' }, '自取'),
          h('input', {
            type: 'radio', name: 'delivery', value: 'DELIVERY', checked: deliveryMethod.value === 'DELIVERY',
            style: 'margin-right: 4px', onChange: (e: Event) => { deliveryMethod.value = (e.target as HTMLInputElement).value }
          }),
          h('span', null, '配送')
        ]),
        deliveryMethod.value === 'DELIVERY' ? h('input', {
          type: 'text', placeholder: '请输入配送地址',
          style: 'width: 100%; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px; box-sizing: border-box;',
          onInput: (e: Event) => { deliveryAddress.value = (e.target as HTMLInputElement).value }
        }) : null
      ]),
      showCancelButton: true,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      beforeClose: (action: string, _instance: unknown, done: () => void) => {
        if (action === 'confirm' && deliveryMethod.value === 'DELIVERY' && !deliveryAddress.value.trim()) {
          ElMessage.error('请输入配送地址')
          return
        }
        done()
      }
    })
    buying.value = true
    const data: { goodsId: number; remark?: string; deliveryMethod?: string; deliveryAddress?: string } = { goodsId: goods.value.id }
    if (deliveryMethod.value === 'DELIVERY') {
      data.deliveryMethod = 'DELIVERY'
      data.deliveryAddress = deliveryAddress.value
    } else {
      data.deliveryMethod = 'PICKUP'
    }
    await createOrder(data)
    ElMessage.success('下单成功')
    router.push('/order')
  } catch { /* cancel */ } finally { buying.value = false }
}

const handleFavorite = async () => {
  if (!goods.value || !userStore.token) { ElMessage.warning('请先登录'); return }
  favoriting.value = true
  try {
    if (goods.value.isFavorited) {
      await unfavoriteGoods(goods.value.id); goods.value.isFavorited = false; goods.value.favoriteCount = Math.max(0, goods.value.favoriteCount - 1); ElMessage.success('已取消收藏')
    } else {
      await favoriteGoods(goods.value.id); goods.value.isFavorited = true; goods.value.favoriteCount++; ElMessage.success('已收藏')
    }
  } catch { /* ignore */ } finally { favoriting.value = false }
}

const handleChat = () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (!goods.value) return
  router.push({ path: '/chat', query: { targetUserId: String(goods.value.userId), name: goods.value.username } })
}

const handleReport = () => { router.push({ path: '/report', query: { targetType: '1', targetId: String(route.params.id) } }) }

const handleAddToCart = async () => {
  if (!goods.value || !userStore.token) { ElMessage.warning('请先登录'); return }
  addingToCart.value = true
  try {
    await addToCart(goods.value.id)
    isInCart.value = true
    cartStore.fetchCartCount()
    ElMessage.success('已加入购物车')
  } finally { addingToCart.value = false }
}

const handleToggleFollow = async () => {
  if (!goods.value || !userStore.token) { ElMessage.warning('请先登录'); return }
  followLoading.value = true
  try {
    await toggleFollow(goods.value.userId)
    isFollowed.value = !isFollowed.value
    ElMessage.success(isFollowed.value ? '已关注' : '已取消关注')
  } finally { followLoading.value = false }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.detail-gallery { border-radius: var(--radius-lg); overflow: hidden; background: #f1f5f9; }
.gallery-img { width: 100%; height: 420px; border-radius: var(--radius-lg); &.single { display: block; } }

.detail-info { padding-top: 8px; }
.detail-status { display: flex; gap: 8px; margin-bottom: 12px; }
.detail-title { font-size: 24px; font-weight: 700; color: var(--text-primary); line-height: 1.4; margin-bottom: 8px; }
.detail-stats { color: var(--text-muted); font-size: 13px; display: flex; gap: 6px; margin-bottom: 20px; }

.price-box {
  background: linear-gradient(135deg, #fef2f2, #fff7ed);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  display: flex; align-items: baseline; gap: 12px;
  margin-bottom: 24px;
}
.price-current { font-size: 36px; font-weight: 800; color: var(--danger); }
.price-original { font-size: 16px; color: var(--text-muted); text-decoration: line-through; }

.detail-desc {
  margin-bottom: 24px;
  h3 { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
  p { font-size: 14px; color: var(--text-secondary); line-height: 1.8; }
}

.seller-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; border-radius: var(--radius-md);
  border: 1px solid var(--border); cursor: pointer;
  transition: var(--transition); margin-bottom: 24px;
  &:hover { background: var(--bg-hover); border-color: var(--primary-lighter); }
}
.seller-name { font-weight: 600; font-size: 15px; }
.seller-action { font-size: 12px; color: var(--primary); margin-top: 2px; }

.action-bar { display: flex; gap: 12px; flex-wrap: wrap; }
</style>

<template>
  <div v-if="pageLoading" class="goods-detail-skeleton">
    <el-row :gutter="32">
      <el-col :xs="24" :md="12">
        <div class="skeleton-gallery"></div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="skeleton-info">
          <div class="skeleton-tags"></div>
          <div class="skeleton-title"></div>
          <div class="skeleton-stats"></div>
          <div class="skeleton-price"></div>
          <div class="skeleton-seller"></div>
          <div class="skeleton-actions"></div>
        </div>
      </el-col>
    </el-row>
  </div>
  <div v-else-if="goods" class="goods-detail">
    <el-row :gutter="32">
      <el-col :xs="24" :md="12">
        <div class="detail-gallery">
          <el-carousel height="420px" indicator-position="outside" v-if="imageList.length > 1">
            <el-carousel-item v-for="(img, idx) in imageList" :key="idx">
              <el-image :src="img" fit="cover" class="gallery-img" :preview-src-list="imageList" :initial-index="idx" />
            </el-carousel-item>
          </el-carousel>
          <el-image v-else :src="goods.coverImage || '/default-cover.svg'" fit="cover" class="gallery-img single" :preview-src-list="imageList" :initial-index="0" />
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="detail-info">
          <div class="detail-status">
            <el-tag :type="goodsStatusTagType(goods.status)" effect="dark" round>{{ goodsStatusLabel(goods.status) }}</el-tag>
            <el-tag round>{{ goods.categoryName }}</el-tag>
            <el-tag v-if="goods.condition" type="warning" round>{{ goods.condition }}</el-tag>
          </div>
          <h1 class="detail-title">{{ goods.title }}</h1>
          <div class="detail-stats">
            <span>{{ goods.viewCount }} 浏览</span>
            <span class="stat-dot">·</span>
            <span>{{ goods.favoriteCount }} 收藏</span>
            <span class="stat-dot">·</span>
            <span>库存 {{ goods.stock || 1 }} 件</span>
          </div>
          <div class="price-box">
            <span class="price-current">¥{{ goods.price }}</span>
            <span v-if="goods.originalPrice && goods.originalPrice > goods.price" class="price-original">¥{{ goods.originalPrice }}</span>
            <el-tag v-if="goods.originalPrice > goods.price" type="danger" effect="dark" round size="small">{{ discount }}折</el-tag>
          </div>

          <SellerCard
            v-if="userStore.token && goods.userId !== userStore.userInfo?.id"
            :seller-info="goods"
            :seller-rating="sellerRating"
            :seller-public-info="sellerPublicInfo"
            :follow-counts="sellerFollowCounts"
            :is-followed="isFollowed"
            :follow-loading="followLoading"
            @follow="handleToggleFollow"
            @chat="handleConsult"
            @view-profile="$router.push(`/profile/${goods.userId}`)"
          />

          <div class="action-bar-sticky">
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
            <el-button size="large" @click="handleShare" round>分享</el-button>
          </div>
        </div>
      </el-col>
     </el-row>
     <div class="detail-tabs">
       <el-tabs v-model="activeTab">
         <el-tab-pane label="商品描述" name="desc">
           <div class="desc-content">{{ goods.description || '暂无描述' }}</div>
         </el-tab-pane>
         <el-tab-pane name="reviews">
           <template #label>卖家评价<el-badge v-if="ratingTotal > 0" :value="ratingTotal" type="primary" class="review-badge" /></template>
           <ReviewSection
             :rating-distribution="ratingDist"
             :reviews="reviewList"
             :rating-total="ratingTotal"
             @load-more="handleLoadReviews"
             @filter-change="handleReviewFilterChange"
           />
         </el-tab-pane>
       </el-tabs>
     </div>
     <div class="similar-section" v-if="similarGoods.length > 0">
       <h3 class="similar-title">相似商品推荐</h3>
       <div class="similar-scroll">
         <GoodsCard v-for="g in similarGoods" :key="g.id" :goods="g" class="similar-card" />
       </div>
     </div>
     <div class="similar-section" v-else-if="similarLoading">
       <h3 class="similar-title">相似商品推荐</h3>
       <div class="similar-scroll">
         <GoodsCardSkeleton v-for="i in 4" :key="i" class="similar-card" />
       </div>
     </div>
  </div>
  <div v-else class="goods-not-found">
    <EmptyState icon="🔍" title="商品不存在" description="该商品可能已下架或被删除" action-text="返回首页" @action="$router.push('/')" />
  </div>

  <BuyDialog
    v-model:visible="buyDialogVisible"
    :goods="goods"
    :addresses="buyAddressList"
    @confirm="handleConfirmBuy"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, getGoodsList, favoriteGoods, unfavoriteGoods } from '@/api/goods'
import { createOrder } from '@/api/order'
import { addToCart, getCartList, type CartVO } from '@/api/cart'
import { toggleFollow, isFollowing, getFollowCounts } from '@/api/follow'
import { getAverageRating, getRatingList, getRatingDistribution, type SellerRatingVO, type RatingDistribution } from '@/api/rating'
import { getAddressList, type DeliveryAddressVO } from '@/api/address'
import { getUserPublicInfo, type UserVO } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import type { GoodsVO } from '@/api/goods'
import GoodsCard from '@/components/GoodsCard.vue'
import GoodsCardSkeleton from '@/components/GoodsCardSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import SellerCard from '@/components/SellerCard.vue'
import ReviewSection from '@/components/ReviewSection.vue'
import BuyDialog from '@/components/BuyDialog.vue'
import { goodsStatusLabel, goodsStatusTagType } from '@/utils/labels'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const goods = ref<GoodsVO | null>(null)
const pageLoading = ref(true)
const buying = ref(false)
const favoriting = ref(false)
const addingToCart = ref(false)
const isFollowed = ref(false)
const followLoading = ref(false)
const sellerRating = ref(0)
const isInCart = ref(false)
const activeTab = ref('desc')
const ratingDist = ref<RatingDistribution | null>(null)
const reviewList = ref<SellerRatingVO[]>([])
const reviewPage = ref(1)
const reviewPageSize = 10
const ratingTotal = ref(0)
const reviewFilter = ref<'all' | 'good' | 'medium' | 'bad'>('all')
const sellerPublicInfo = ref<UserVO | null>(null)
const sellerFollowCounts = ref<{ following: number; followers: number } | null>(null)
const similarGoods = ref<GoodsVO[]>([])
const similarLoading = ref(false)

const buyDialogVisible = ref(false)
const buyAddressList = ref<DeliveryAddressVO[]>([])

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
  pageLoading.value = true
  try {
    goods.value = await getGoodsDetail(Number(route.params.id))
    if (goods.value && userStore.token && goods.value.userId !== userStore.userInfo?.id) {
      try { isFollowed.value = await isFollowing(goods.value.userId) } catch (e) { console.error(e) }
      try { sellerRating.value = await getAverageRating(goods.value.userId) } catch (e) { console.error(e) }
      try { sellerPublicInfo.value = await getUserPublicInfo(goods.value.userId) } catch (e) { console.error(e) }
      try { sellerFollowCounts.value = await getFollowCounts(goods.value.userId) } catch (e) { console.error(e) }
    }
    if (goods.value && userStore.token) {
      try {
        const cartList = await getCartList()
        isInCart.value = cartList ? cartList.some((c: CartVO) => c.goodsId === goods.value!.id) : false
      } catch (e) { console.error(e) }
    }
    if (goods.value) {
      try { ratingDist.value = await getRatingDistribution(goods.value.userId) } catch (e) { console.error(e) }
      loadReviews()
      loadSimilarGoods()
    }
  } finally {
    pageLoading.value = false
  }
}

const loadSimilarGoods = async () => {
  if (!goods.value) return
  similarLoading.value = true
  try {
    const res = await getGoodsList({ pageNum: 1, pageSize: 10, categoryId: goods.value.categoryId })
    similarGoods.value = (res.list || []).filter((g: GoodsVO) => g.id !== goods.value!.id && g.status === 'ONLINE').slice(0, 8)
  } catch (e) {
    console.error(e)
  } finally {
    similarLoading.value = false
  }
}

const handleBuy = async () => {
  if (!goods.value) return
  try { buyAddressList.value = await getAddressList() } catch (e) { console.error(e) }
  buyDialogVisible.value = true
}

const handleConfirmBuy = async (data: { goodsId: number; quantity: number; remark?: string; deliveryMethod?: string; deliveryAddress?: string }) => {
  buying.value = true
  try {
    await createOrder(data)
    cartStore.fetchCartCount()
    loadData()
    ElMessage.success('下单成功')
    buyDialogVisible.value = false
    router.push('/order')
  } catch (e) { console.error(e) } finally { buying.value = false }
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
  } catch (e) { console.error(e) } finally { favoriting.value = false }
}

const handleConsult = () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (!goods.value) return
  const goodsInfo = JSON.stringify({ goodsId: goods.value.id, title: goods.value.title, price: goods.value.price })
  router.push({ path: `/chat/${goods.value.userId}`, query: { consult: goodsInfo } })
}

const handleReport = () => { router.push({ path: '/report', query: { targetType: '1', targetId: String(route.params.id) } }) }

const handleShare = () => {
  if (!goods.value) return
  const text = `【${goods.value.title}】仅需 ¥${goods.value.price}！快来看看这个校园好物 → ${window.location.href}`
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => {
      ElMessage.success('分享链接已复制到剪贴板')
    }).catch(() => {
      fallbackCopy(text)
    })
  } else {
    fallbackCopy(text)
  }
}

const fallbackCopy = (text: string) => {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    ElMessage.success('分享链接已复制到剪贴板')
  } catch {
    ElMessage.info('请手动复制分享链接')
  }
  document.body.removeChild(textarea)
}

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

const handleReviewFilterChange = (filter: 'all' | 'good' | 'medium' | 'bad') => {
  reviewFilter.value = filter
  reviewPage.value = 1
  loadReviews()
}

const handleLoadReviews = (page: number) => {
  reviewPage.value = page
  loadReviews()
}

const loadReviews = async () => {
  if (!goods.value) return
  try {
    const res = await getRatingList(goods.value.userId, { pageNum: reviewPage.value, pageSize: reviewPageSize })
    reviewList.value = res.list
    ratingTotal.value = res.total
  } catch (e) { console.error(e) }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.goods-detail {
  padding: var(--spacing-lg);
  padding-bottom: 100px;
}

.goods-detail-skeleton {
  padding: var(--spacing-lg);
}

.skeleton-gallery {
  width: 100%;
  height: 420px;
  border-radius: var(--radius-lg);
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info {
  padding-top: 8px;
}

.skeleton-tags {
  height: 24px;
  width: 40%;
  border-radius: 12px;
  margin-bottom: 14px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-title {
  height: 32px;
  width: 80%;
  border-radius: 6px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-stats {
  height: 16px;
  width: 50%;
  border-radius: 4px;
  margin-bottom: 22px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-price {
  height: 48px;
  width: 100%;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-seller {
  height: 72px;
  width: 100%;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-actions {
  height: 48px;
  width: 100%;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, var(--color-img-placeholder-from) 25%, var(--color-img-placeholder-to) 50%, var(--color-img-placeholder-from) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.detail-gallery {
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: linear-gradient(135deg, var(--color-img-placeholder-from), var(--color-img-placeholder-to));
  box-shadow: var(--shadow-md);
}
.gallery-img {
  width: 100%;
  height: 420px;
  border-radius: var(--radius-lg);
  filter: brightness(var(--img-brightness));
  &.single { display: block; }
}

.detail-info { padding-top: 8px; }
.detail-status { display: flex; gap: 8px; margin-bottom: 14px; }
.detail-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.35;
  margin-bottom: 10px;
  letter-spacing: -0.3px;
}
.detail-stats {
  color: var(--text-muted);
  font-size: 13px;
  display: flex;
  gap: 6px;
  margin-bottom: 22px;
  font-weight: 500;
}
.stat-dot { opacity: 0.5; }

.price-box {
  background: linear-gradient(135deg, var(--color-price-box-from), var(--color-price-box-to));
  border-radius: var(--radius-md);
  padding: 20px 24px;
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
  border: 1px solid var(--color-price-box-border);
}
.price-current { font-size: 36px; font-weight: 800; color: var(--danger); letter-spacing: -0.5px; }
.price-original { font-size: 16px; color: var(--text-muted); text-decoration: line-through; }

.action-bar-sticky {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: var(--spacing-md) 0;

  margin-top: 8px;
}

.detail-tabs {
  background: var(--bg-glass);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);
  margin-top: 28px;
}
.desc-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.8;
  padding: 8px 0;
  white-space: pre-wrap;
}

.review-badge { margin-left: 6px; }

.similar-section {
  margin-top: 32px;
}

.similar-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-left: 4px;
}

.similar-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  &::-webkit-scrollbar { height: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: var(--text-muted); border-radius: 3px; }
}

.similar-card {
  flex: 0 0 220px;
  min-width: 220px;
  margin-bottom: 0;
}

.goods-not-found {
  padding: var(--spacing-xl);
}

@media (max-width: 768px) {
  .goods-detail {
    padding: var(--spacing-md);
    padding-bottom: 90px;
  }

  .gallery-img { height: 280px; }

  .detail-title { font-size: 20px; }

  .price-current { font-size: 28px; }

  .action-bar-sticky {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    background: transparent;
    border-top: 1px solid var(--border);
    box-shadow: 0 -2px 12px rgba(0,0,0,0.08);
    padding: var(--spacing-sm) var(--spacing-md);
    margin-top: 0;
    z-index: 100;
  }

  .similar-card {
    flex: 0 0 170px;
    min-width: 170px;
  }
}
</style>

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

          <div class="seller-card" v-if="userStore.token && goods.userId !== userStore.userInfo?.id">
            <el-avatar :size="44" :src="goods.userAvatar || '/default-avatar.svg'" class="seller-avatar" @click="$router.push(`/profile/${goods.userId}`)" />
            <div class="seller-info" @click="$router.push(`/profile/${goods.userId}`)">
              <div class="seller-name">
                {{ goods.username }}
                <el-tag v-if="goods.sellerRealVerified === 1" type="success" effect="dark" size="small" round class="seller-verified-tag">已认证</el-tag>
              </div>
              <div class="seller-action">
                <span>查看主页</span>
                <el-rate v-if="sellerRating > 0" :model-value="sellerRating" disabled size="small" class="seller-rate" />
                <span v-else class="seller-no-rating">暂无评价</span>
              </div>
              <div class="seller-meta">
                <span v-if="sellerPublicInfo" class="seller-meta-item">
                  <el-icon><Star /></el-icon>
                  信誉 {{ sellerRating > 0 ? sellerRating.toFixed(1) : '-' }}
                </span>
                <span v-if="sellerPublicInfo && sellerPublicInfo.goodsCount !== undefined" class="seller-meta-item">
                  <el-icon><Goods /></el-icon>
                  在售 {{ sellerPublicInfo.goodsCount ?? 0 }}
                </span>
                <span v-if="sellerFollowCounts" class="seller-meta-item">
                  <el-icon><User /></el-icon>
                  {{ sellerFollowCounts.followers }} 粉丝
                </span>
              </div>
            </div>
            <div class="seller-actions">
              <el-button size="small" @click.stop="handleChat" round>聊天</el-button>
              <el-button size="small" type="primary" @click.stop="handleConsult" round plain>咨询商品</el-button>
              <el-button :type="isFollowed ? 'warning' : 'default'" size="small" @click.stop="handleToggleFollow" :loading="followLoading" round>
                {{ isFollowed ? '已关注' : '关注' }}
              </el-button>
            </div>
          </div>
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
           <div class="reviews-section">
             <div class="rating-summary" v-if="ratingDist">
               <div class="rating-score">
                 <span class="score-num">{{ ratingDist.avgRating }}</span>
                 <el-rate :model-value="ratingDist.avgRating" disabled allow-half size="large" />
                 <span class="score-total">{{ ratingDist.totalCount }} 条评价</span>
               </div>
               <div class="rating-bars">
                 <div v-for="s in [5,4,3,2,1]" :key="s" class="rating-bar-row">
                   <span class="bar-label">{{ s }}星</span>
                   <div class="bar-track"><div class="bar-fill" :style="{ width: getBarWidth(s) + '%' }" /></div>
                   <span class="bar-count">{{ ratingDist.distribution[s] || 0 }}</span>
                 </div>
               </div>
             </div>
             <div class="review-filter" v-if="ratingDist && ratingDist.totalCount > 0">
               <el-radio-group v-model="reviewFilter" size="small" @change="handleReviewFilterChange">
                 <el-radio-button value="all">全部 ({{ ratingDist.totalCount }})</el-radio-button>
                 <el-radio-button value="good">好评 ({{ ratingDist.distribution[5] || 0 }})</el-radio-button>
                 <el-radio-button value="medium">中评 ({{ (ratingDist.distribution[3] || 0) + (ratingDist.distribution[4] || 0) }})</el-radio-button>
                 <el-radio-button value="bad">差评 ({{ (ratingDist.distribution[1] || 0) + (ratingDist.distribution[2] || 0) }})</el-radio-button>
               </el-radio-group>
             </div>
             <div class="review-list" v-if="filteredReviews.length > 0">
                <div v-for="r in filteredReviews" :key="r.id" class="review-item">
                  <el-avatar :size="36" :src="r.buyerAvatar || '/default-avatar.svg'" class="reviewer-avatar" @click="$router.push(`/profile/${r.buyerId}`)" />
                  <div class="review-body">
                    <div class="review-header">
                      <span class="reviewer-name" @click="$router.push(`/profile/${r.buyerId}`)">{{ r.buyerName }}</span>
                      <span v-if="r.goodsTitle" class="review-goods">购买了「{{ r.goodsTitle }}」</span>
                      <el-rate :model-value="r.rating" disabled size="small" />
                      <span class="review-time">{{ formatReviewTime(r.createTime) }}</span>
                    </div>
                    <div class="review-comment" v-if="r.comment">{{ r.comment }}</div>
                    <div class="review-images" v-if="r.images">
                      <el-image v-for="(img, imgIdx) in parseReviewImages(r.images)" :key="imgIdx" :src="img" fit="cover" class="review-img-thumb" :preview-src-list="parseReviewImages(r.images)" :initial-index="imgIdx" hide-on-click-modal />
                    </div>
                  </div>
                </div>
               <div class="review-pagination" v-if="ratingTotal > reviewPageSize">
                 <el-pagination
                   v-model:current-page="reviewPage"
                   :page-size="reviewPageSize"
                   :total="ratingTotal"
                   layout="prev, pager, next"
                   small
                   @current-change="loadReviews"
                 />
               </div>
             </div>
             <EmptyState v-else icon="💬" title="暂无评价" description="该卖家还没有收到评价" />
           </div>
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

  <el-dialog v-model="buyDialogVisible" title="确认购买" width="520px" destroy-on-close>
    <div v-if="goods">
      <p class="buy-confirm-text">确认购买「{{ goods.title }}」？单价 ¥{{ goods.price }}</p>
      <div class="buy-quantity-row">
        <span class="buy-label">购买数量：</span>
        <el-input-number v-model="buyQuantity" :min="1" :max="goods.stock || 1" size="small" />
        <span class="buy-stock-hint">（库存 {{ goods.stock || 1 }} 件）</span>
      </div>
      <div class="buy-delivery-row">
        <span class="buy-label">配送方式：</span>
        <el-radio-group v-model="buyDeliveryMethod">
          <el-radio value="PICKUP">自取</el-radio>
          <el-radio value="DELIVERY">配送</el-radio>
        </el-radio-group>
      </div>
      <template v-if="buyDeliveryMethod === 'DELIVERY'">
        <div v-if="buyAddressList.length > 0" class="buy-address-list">
          <div class="buy-address-label">选择已有地址：</div>
          <div
            v-for="addr in buyAddressList" :key="addr.id"
            class="buy-address-item"
            :class="{ active: buySelectedAddressId === addr.id }"
            @click="buySelectedAddressId = addr.id"
          >
            <div class="buy-address-name">{{ addr.receiverName }} {{ addr.receiverPhone }}</div>
            <div class="buy-address-detail">{{ [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') }}</div>
          </div>
        </div>
        <div class="buy-add-addr-toggle">
          <el-button type="primary" size="small" link @click="buyShowAddAddr = !buyShowAddAddr">{{ buyShowAddAddr ? '收起' : '新增收货地址' }}</el-button>
        </div>
        <div v-if="buyShowAddAddr" class="buy-add-addr-form">
          <el-form :model="buyAddrForm" label-width="90px" size="small">
            <el-row :gutter="8">
              <el-col :span="12"><el-form-item label="收货人"><el-input v-model="buyAddrForm.receiverName" placeholder="收货人" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="手机号"><el-input v-model="buyAddrForm.receiverPhone" placeholder="手机号" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="省/市/区">
              <el-cascader v-model="buyAreaValue" :options="areaOptions" :props="{ expandTrigger: 'hover' }" placeholder="请选择" class="buy-area-cascader" teleported />
            </el-form-item>
            <el-form-item label="详细地址"><el-input v-model="buyAddrForm.detailAddress" placeholder="街道、楼栋、门牌号" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleBuyAddAddress" :loading="buyAddrSaving">保存地址</el-button></el-form-item>
          </el-form>
        </div>
      </template>
      <div class="buy-remark-row">
        <span class="buy-label">订单备注：</span>
        <el-input v-model="buyRemark" placeholder="选填，如特殊要求等" size="small" class="buy-remark-input" />
      </div>
    </div>
    <template #footer>
      <el-button @click="buyDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleConfirmBuy" :loading="buying">确认购买</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, getGoodsList, favoriteGoods, unfavoriteGoods } from '@/api/goods'
import { createOrder } from '@/api/order'
import { addToCart, getCartList, type CartVO } from '@/api/cart'
import { toggleFollow, isFollowing, getFollowCounts } from '@/api/follow'
import { getAverageRating, getRatingList, getRatingDistribution, type SellerRatingVO, type RatingDistribution } from '@/api/rating'
import { getAddressList, addAddress, type DeliveryAddressVO } from '@/api/address'
import { getUserPublicInfo, type UserVO } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage } from 'element-plus'
import { Star, Goods, User } from '@element-plus/icons-vue'
import type { GoodsVO } from '@/api/goods'
import GoodsCard from '@/components/GoodsCard.vue'
import GoodsCardSkeleton from '@/components/GoodsCardSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import areaOptions from '@/data/area'
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
const buyQuantity = ref(1)
const buyDeliveryMethod = ref('PICKUP')
const buyAddressList = ref<DeliveryAddressVO[]>([])
const buySelectedAddressId = ref<number | null>(null)
const buyShowAddAddr = ref(false)
const buyAddrSaving = ref(false)
const buyAreaValue = ref<string[]>([])
const buyRemark = ref('')
const buyAddrForm = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })

const imageList = computed(() => {
  if (!goods.value) return []
  const imgs = goods.value.images ? goods.value.images.split(',').filter(Boolean) : []
  return goods.value.coverImage ? [goods.value.coverImage, ...imgs] : imgs
})

const discount = computed(() => {
  if (!goods.value?.originalPrice || goods.value.originalPrice === 0) return ''
  return (goods.value.price / goods.value.originalPrice * 10).toFixed(1)
})

const filteredReviews = computed(() => {
  if (reviewFilter.value === 'all') return reviewList.value
  return reviewList.value.filter((r: SellerRatingVO) => {
    if (reviewFilter.value === 'good') return r.rating >= 5
    if (reviewFilter.value === 'medium') return r.rating >= 3 && r.rating <= 4
    if (reviewFilter.value === 'bad') return r.rating <= 2
    return true
  })
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
  buyQuantity.value = 1
  buyDeliveryMethod.value = 'PICKUP'
  buySelectedAddressId.value = null
  buyShowAddAddr.value = false
  buyAreaValue.value = []
  buyRemark.value = ''
  buyAddrForm.receiverName = ''; buyAddrForm.receiverPhone = ''; buyAddrForm.province = ''; buyAddrForm.city = ''; buyAddrForm.district = ''; buyAddrForm.detailAddress = ''
  try { buyAddressList.value = await getAddressList() } catch (e) { console.error(e) }
  buyDialogVisible.value = true
}

const handleBuyAddAddress = async () => {
  if (!buyAddrForm.receiverName || !buyAddrForm.receiverPhone || !buyAddrForm.detailAddress) { ElMessage.error('请填写收货人、手机号和详细地址'); return }
  if (buyAreaValue.value.length === 3) {
    buyAddrForm.province = buyAreaValue.value[0]
    buyAddrForm.city = buyAreaValue.value[1]
    buyAddrForm.district = buyAreaValue.value[2]
  }
  buyAddrSaving.value = true
  try {
    await addAddress(buyAddrForm)
    buyAddressList.value = await getAddressList()
    const newest = buyAddressList.value[0]
    if (newest) buySelectedAddressId.value = newest.id
    buyShowAddAddr.value = false
    buyAddrForm.receiverName = ''; buyAddrForm.receiverPhone = ''; buyAddrForm.province = ''; buyAddrForm.city = ''; buyAddrForm.district = ''; buyAddrForm.detailAddress = ''
    buyAreaValue.value = []
    ElMessage.success('地址添加成功')
  } catch (e) { console.error(e); ElMessage.error('添加失败') } finally { buyAddrSaving.value = false }
}

const handleConfirmBuy = async () => {
  if (!goods.value) return
  if (buyDeliveryMethod.value === 'DELIVERY' && !buySelectedAddressId.value) {
    ElMessage.error('请选择配送地址')
    return
  }
  buying.value = true
  try {
    const data: { goodsId: number; quantity: number; remark?: string; deliveryMethod?: string; deliveryAddress?: string } = { goodsId: goods.value.id, quantity: buyQuantity.value }
    if (buyRemark.value.trim()) data.remark = buyRemark.value.trim()
    if (buyDeliveryMethod.value === 'DELIVERY') {
      data.deliveryMethod = 'DELIVERY'
      const addr = buyAddressList.value.find(a => a.id === buySelectedAddressId.value)
      data.deliveryAddress = addr ? [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') + ` (${addr.receiverName} ${addr.receiverPhone})` : ''
    } else {
      data.deliveryMethod = 'PICKUP'
    }
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

const handleChat = () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (!goods.value) return
  router.push(`/chat/${goods.value.userId}`)
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

const handleReviewFilterChange = () => {
  reviewPage.value = 1
}

const loadReviews = async () => {
  if (!goods.value) return
  try {
    const res = await getRatingList(goods.value.userId, { pageNum: reviewPage.value, pageSize: reviewPageSize })
    reviewList.value = res.list
    ratingTotal.value = res.total
  } catch (e) { console.error(e) }
}

const getBarWidth = (star: number) => {
  if (!ratingDist.value || ratingDist.value.totalCount === 0) return 0
  return ((ratingDist.value.distribution[star] || 0) / ratingDist.value.totalCount) * 100
}

const formatReviewTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + '天前'
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

const parseReviewImages = (images: string): string[] => {
  if (!images) return []
  return images.split(',').map(s => s.trim()).filter(Boolean)
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
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
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
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-title {
  height: 32px;
  width: 80%;
  border-radius: 6px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-stats {
  height: 16px;
  width: 50%;
  border-radius: 4px;
  margin-bottom: 22px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-price {
  height: 48px;
  width: 100%;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-seller {
  height: 72px;
  width: 100%;
  border-radius: var(--radius-md);
  margin-bottom: 24px;
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-actions {
  height: 48px;
  width: 100%;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
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
  background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
  box-shadow: var(--shadow-md);
}
.gallery-img {
  width: 100%;
  height: 420px;
  border-radius: var(--radius-lg);
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
  background: linear-gradient(135deg, #fef2f2, #fff7ed);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
  border: 1px solid #fecaca;
}
.price-current { font-size: 36px; font-weight: 800; color: var(--danger); letter-spacing: -0.5px; }
.price-original { font-size: 16px; color: var(--text-muted); text-decoration: line-through; }

.detail-desc {
  margin-bottom: 24px;
  h3 { font-size: 15px; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
  p { font-size: 14px; color: var(--text-secondary); line-height: 1.8; }
}

.seller-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--bg-glass);
  backdrop-filter: blur(8px);
  transition: var(--transition);
  margin-bottom: 24px;
  &:hover { background: var(--primary-lighter); border-color: var(--primary-light); }
}

.seller-avatar { cursor: pointer; }

.seller-info {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.seller-name { font-weight: 600; font-size: 15px; }

.seller-verified-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.seller-action {
  font-size: 12px;
  color: var(--primary);
  margin-top: 2px;
  display: flex;
  align-items: center;
}

.seller-rate {
  margin-left: 8px;
  vertical-align: middle;
}

.seller-no-rating {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.seller-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.seller-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.seller-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.action-bar-sticky {
  position: sticky;
  bottom: 0;
  z-index: 10;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: var(--spacing-md) 0;
  background: linear-gradient(to top, var(--bg-card) 70%, transparent);
  margin-top: 8px;
}

.buy-confirm-text { margin-bottom: 12px; }

.buy-quantity-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.buy-label { margin-right: 12px; }

.buy-stock-hint {
  margin-left: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.buy-delivery-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.buy-address-list { margin-bottom: 10px; }

.buy-address-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
}

.buy-address-item {
  padding: 10px 14px;
  margin-bottom: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: #6366f1; }
  &.active { border-color: #6366f1; background: rgba(99,102,241,0.06); }
}

.buy-address-name { font-size: 14px; font-weight: 500; }

.buy-address-detail {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.buy-add-addr-toggle { margin-bottom: 8px; }

.buy-add-addr-form {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
}

.buy-area-cascader { width: 100%; }

.buy-remark-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.buy-remark-input {
  width: 100%;
  margin-top: 4px;
}

.buy-address-item {
  padding: 10px 14px; margin-bottom: 6px;
  border: 1px solid #e2e8f0; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: #6366f1; }
  &.active { border-color: #6366f1; background: rgba(99,102,241,0.06); }
}

.detail-tabs {
  background: rgba(255,255,255,0.95);
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
.reviews-section { padding: 8px 0; }
.rating-summary {
  display: flex;
  gap: 32px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f5f3ff, #ede9fe);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  align-items: center;
}
.rating-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 120px;
  .score-num { font-size: 42px; font-weight: 800; color: var(--primary); line-height: 1; }
  .score-total { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
}
.rating-bars { flex: 1; }
.rating-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  .bar-label { font-size: 12px; color: var(--text-muted); width: 28px; text-align: right; }
  .bar-track { flex: 1; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
  .bar-fill { height: 100%; background: var(--primary-gradient); border-radius: 4px; transition: width 0.4s ease; }
  .bar-count { font-size: 12px; color: var(--text-muted); width: 24px; }
}

.review-filter {
  margin-bottom: 16px;
}

.review-badge { margin-left: 6px; }

.reviewer-avatar { cursor: pointer; }

.review-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  &:last-child { border-bottom: none; }
}
.review-body { flex: 1; }
.review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
  .reviewer-name { font-weight: 600; font-size: 14px; cursor: pointer; }
  .review-goods { font-size: 12px; color: var(--primary); background: var(--primary-lighter); padding: 1px 8px; border-radius: 10px; }
  .review-time { font-size: 12px; color: var(--text-muted); margin-left: auto; }
}
.review-comment { font-size: 14px; color: var(--text-secondary); line-height: 1.6; }
.review-images { display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.review-img-thumb { width: 72px; height: 72px; border-radius: 8px; cursor: pointer; border: 1px solid var(--border); }
.review-pagination { margin-top: 16px; display: flex; justify-content: center; }

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
  &::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 3px; }
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

  .seller-card { flex-wrap: wrap; }

  .seller-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .action-bar-sticky {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--bg-card);
    border-top: 1px solid var(--border);
    box-shadow: 0 -2px 12px rgba(0,0,0,0.08);
    padding: var(--spacing-sm) var(--spacing-md);
    margin-top: 0;
    z-index: 100;
    background: var(--bg-card);
  }

  .similar-card {
    flex: 0 0 170px;
    min-width: 170px;
  }

  .rating-summary {
    flex-direction: column;
    gap: 16px;
  }
}
</style>

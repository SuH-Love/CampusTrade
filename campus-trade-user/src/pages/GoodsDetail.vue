<template>
  <div v-if="goods" class="goods-detail">
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
          <div class="detail-desc">
            <h3>商品描述</h3>
            <p>{{ goods.description || '暂无描述' }}</p>
          </div>
          <div class="seller-card" v-if="userStore.token && goods.userId !== userStore.userInfo?.id">
            <el-avatar :size="44" :src="goods.userAvatar || '/default-avatar.svg'" @click="$router.push(`/profile/${goods.userId}`)" style="cursor: pointer" />
            <div class="seller-info" @click="$router.push(`/profile/${goods.userId}`)" style="cursor: pointer">
              <div class="seller-name">{{ goods.username }}</div>
              <div class="seller-action">
                <span>查看主页</span>
                <el-rate v-if="sellerRating > 0" :model-value="sellerRating" disabled size="small" style="margin-left: 8px; vertical-align: middle" />
                <span v-else style="margin-left: 8px; font-size: 12px; color: var(--text-muted)">暂无评价</span>
              </div>
            </div>
            <el-button size="small" @click.stop="handleChat" round>聊天</el-button>
            <el-button size="small" type="primary" @click.stop="handleConsult" round plain>咨询商品</el-button>
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
   <div v-else style="padding: 20px"><el-empty description="商品不存在" /></div>

  <el-dialog v-model="buyDialogVisible" title="确认购买" width="520px" destroy-on-close>
    <div v-if="goods">
      <p style="margin-bottom: 12px">确认购买「{{ goods.title }}」？单价 ¥{{ goods.price }}</p>
      <div style="margin-bottom: 12px">
        <span style="margin-right: 12px">购买数量：</span>
        <el-input-number v-model="buyQuantity" :min="1" :max="goods.stock || 1" size="small" />
        <span style="margin-left: 8px; color: #94a3b8; font-size: 13px">（库存 {{ goods.stock || 1 }} 件）</span>
      </div>
      <div style="margin-bottom: 12px">
        <span style="margin-right: 12px">配送方式：</span>
        <el-radio-group v-model="buyDeliveryMethod">
          <el-radio value="PICKUP">自取</el-radio>
          <el-radio value="DELIVERY">配送</el-radio>
        </el-radio-group>
      </div>
      <template v-if="buyDeliveryMethod === 'DELIVERY'">
        <div v-if="buyAddressList.length > 0" style="margin-bottom: 10px">
          <div style="font-size: 13px; color: #64748b; margin-bottom: 6px">选择已有地址：</div>
          <div
            v-for="addr in buyAddressList" :key="addr.id"
            class="buy-address-item"
            :class="{ active: buySelectedAddressId === addr.id }"
            @click="buySelectedAddressId = addr.id"
          >
            <div style="font-size: 14px; font-weight: 500">{{ addr.receiverName }} {{ addr.receiverPhone }}</div>
            <div style="font-size: 12px; color: #64748b; margin-top: 2px">{{ [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') }}</div>
          </div>
        </div>
        <div style="margin-bottom: 8px">
          <el-button type="primary" size="small" link @click="buyShowAddAddr = !buyShowAddAddr">{{ buyShowAddAddr ? '收起' : '新增收货地址' }}</el-button>
        </div>
        <div v-if="buyShowAddAddr" style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-bottom: 8px; background: #fafafa">
          <el-form :model="buyAddrForm" label-width="90px" size="small">
            <el-row :gutter="8">
              <el-col :span="12"><el-form-item label="收货人"><el-input v-model="buyAddrForm.receiverName" placeholder="收货人" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="手机号"><el-input v-model="buyAddrForm.receiverPhone" placeholder="手机号" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="省/市/区">
              <el-cascader v-model="buyAreaValue" :options="areaOptions" :props="{ expandTrigger: 'hover' }" placeholder="请选择" style="width: 100%" teleported />
            </el-form-item>
            <el-form-item label="详细地址"><el-input v-model="buyAddrForm.detailAddress" placeholder="街道、楼栋、门牌号" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleBuyAddAddress" :loading="buyAddrSaving">保存地址</el-button></el-form-item>
          </el-form>
        </div>
      </template>
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
import { getGoodsDetail, favoriteGoods, unfavoriteGoods } from '@/api/goods'
import { createOrder } from '@/api/order'
import { addToCart, getCartList, type CartVO } from '@/api/cart'
import { toggleFollow, isFollowing } from '@/api/follow'
import { getAverageRating } from '@/api/rating'
import { getAddressList, addAddress, type DeliveryAddressVO } from '@/api/address'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { ElMessage } from 'element-plus'
import type { GoodsVO } from '@/api/goods'
import areaOptions from '@/data/area'

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

const buyDialogVisible = ref(false)
const buyQuantity = ref(1)
const buyDeliveryMethod = ref('PICKUP')
const buyAddressList = ref<DeliveryAddressVO[]>([])
const buySelectedAddressId = ref<number | null>(null)
const buyShowAddAddr = ref(false)
const buyAddrSaving = ref(false)
const buyAreaValue = ref<string[]>([])
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
  buyQuantity.value = 1
  buyDeliveryMethod.value = 'PICKUP'
  buySelectedAddressId.value = null
  buyShowAddAddr.value = false
  buyAreaValue.value = []
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
  } catch { /* ignore */ } finally { favoriting.value = false }
}

const handleChat = () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (!goods.value) return
  router.push({ path: '/chat', query: { targetUserId: String(goods.value.userId), name: goods.value.username } })
}

const handleConsult = () => {
  if (!userStore.token) { ElMessage.warning('请先登录'); return }
  if (!goods.value) return
  const goodsInfo = JSON.stringify({ goodsId: goods.value.id, title: goods.value.title, price: goods.value.price })
  router.push({ path: '/chat', query: { targetUserId: String(goods.value.userId), name: goods.value.username, consult: goodsInfo } })
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
.goods-detail {
  padding: 20px;
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
.seller-name { font-weight: 600; font-size: 15px; }
.seller-action { font-size: 12px; color: var(--primary); margin-top: 2px; display: flex; align-items: center; }

.action-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.buy-address-item {
  padding: 10px 14px; margin-bottom: 6px;
  border: 1px solid #e2e8f0; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
  &:hover { border-color: #6366f1; }
  &.active { border-color: #6366f1; background: rgba(99,102,241,0.06); }
}
</style>

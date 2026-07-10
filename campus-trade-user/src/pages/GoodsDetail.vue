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
  const selectedAddressId = ref<number | null>(null)
  const customAddress = ref('')
  const buyQuantity = ref(1)
  let addressList: DeliveryAddressVO[] = []
  try { addressList = await getAddressList() } catch { /* ignore */ }
  const showAddAddr = ref(false)
  const addrForm = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
  try {
    await ElMessageBox({
      title: '确认购买',
      message: () => h('div', null, [
        h('p', { style: 'margin-bottom: 12px' }, `确认购买「${goods.value!.title}」？单价 ¥${goods.value!.price}`),
        h('div', { style: 'margin-bottom: 12px' }, [
          h('span', { style: 'margin-right: 12px' }, '购买数量：'),
          h('input', {
            type: 'number', value: buyQuantity.value, min: 1, max: goods.value!.stock || 1,
            style: 'width: 80px; padding: 6px 8px; border: 1px solid #dcdfe6; border-radius: 4px; text-align: center;',
            onInput: (e: Event) => {
              const v = parseInt((e.target as HTMLInputElement).value) || 1
              buyQuantity.value = Math.max(1, Math.min(v, goods.value!.stock || 1))
            }
          }),
          h('span', { style: 'margin-left: 8px; color: #94a3b8; font-size: 13px' }, `（库存 ${goods.value!.stock || 1} 件）`)
        ]),
        h('div', { style: 'margin-bottom: 12px' }, [
          h('span', { style: 'margin-right: 12px' }, '配送方式：'),
          h('input', {
            type: 'radio', name: 'delivery', value: 'PICKUP', checked: deliveryMethod.value === 'PICKUP',
            style: 'margin-right: 4px', onChange: (e: Event) => { deliveryMethod.value = (e.target as HTMLInputElement).value }
          }),
          h('span', { style: 'margin-right: 16px' }, '自取'),
          h('input', {
            type: 'radio', name: 'delivery', value: 'DELIVERY', checked: deliveryMethod.value === 'DELIVERY',
            style: 'margin-right: 4px', onChange: (e: Event) => { deliveryMethod.value = (e.target as HTMLInputElement).value }
          }),
          h('span', null, '配送')
        ]),
        deliveryMethod.value === 'DELIVERY' ? h('div', null, [
          addressList.length > 0 ? h('div', { style: 'margin-bottom: 10px' }, [
            h('div', { style: 'font-size: 13px; color: #64748b; margin-bottom: 6px' }, '选择已有地址：'),
            ...addressList.map(addr =>
              h('div', {
                style: `padding: 8px 12px; margin-bottom: 6px; border: 1px solid ${selectedAddressId.value === addr.id ? '#6366f1' : '#e2e8f0'}; border-radius: 6px; cursor: pointer; background: ${selectedAddressId.value === addr.id ? 'rgba(99,102,241,0.06)' : '#fff'}; transition: all 0.2s`,
                onClick: () => { selectedAddressId.value = addr.id; customAddress.value = '' }
              }, [
                h('div', { style: 'font-size: 14px; font-weight: 500' }, `${addr.receiverName} ${addr.receiverPhone}`),
                h('div', { style: 'font-size: 12px; color: #64748b; margin-top: 2px' }, [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' '))
              ])
            )
          ]) : null,
          h('div', { style: 'margin-bottom: 8px' }, [
            h('span', { style: 'font-size: 13px; color: #64748b; margin-right: 8px' }, '没有合适地址？'),
            h('button', {
              style: 'color: #6366f1; font-size: 13px; background: none; border: none; cursor: pointer; text-decoration: underline; font-weight: 500',
              onClick: () => { showAddAddr.value = true }
            }, '新增收货地址')
          ]),
          showAddAddr.value ? h('div', { style: 'border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-bottom: 8px; background: #fafafa' }, [
            h('div', { style: 'display: grid; grid-template-columns: 1fr 1fr; gap: 8px' }, [
              h('input', { placeholder: '收货人', value: addrForm.receiverName, style: 'padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px', onInput: (e: Event) => { addrForm.receiverName = (e.target as HTMLInputElement).value } }),
              h('input', { placeholder: '手机号', value: addrForm.receiverPhone, style: 'padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px', onInput: (e: Event) => { addrForm.receiverPhone = (e.target as HTMLInputElement).value } }),
              h('input', { placeholder: '省份', value: addrForm.province, style: 'padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px', onInput: (e: Event) => { addrForm.province = (e.target as HTMLInputElement).value } }),
              h('input', { placeholder: '城市', value: addrForm.city, style: 'padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px', onInput: (e: Event) => { addrForm.city = (e.target as HTMLInputElement).value } }),
              h('input', { placeholder: '区/县', value: addrForm.district, style: 'padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px', onInput: (e: Event) => { addrForm.district = (e.target as HTMLInputElement).value } })
            ]),
            h('input', { placeholder: '详细地址', value: addrForm.detailAddress, style: 'width: 100%; padding: 6px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 13px; margin-top: 8px; box-sizing: border-box', onInput: (e: Event) => { addrForm.detailAddress = (e.target as HTMLInputElement).value } }),
            h('button', {
              style: 'margin-top: 8px; padding: 6px 16px; background: #6366f1; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; font-weight: 500',
              onClick: async () => {
                if (!addrForm.receiverName || !addrForm.receiverPhone || !addrForm.detailAddress) { ElMessage.error('请填写收货人、手机号和详细地址'); return }
                try {
                  await addAddress(addrForm)
                  addressList = await getAddressList()
                  const newest = addressList[0]
                  if (newest) selectedAddressId.value = newest.id
                  showAddAddr.value = false
                  addrForm.receiverName = ''; addrForm.receiverPhone = ''; addrForm.province = ''; addrForm.city = ''; addrForm.district = ''; addrForm.detailAddress = ''
                  ElMessage.success('地址添加成功')
                } catch { ElMessage.error('添加失败') }
              }
            }, '保存地址')
          ]) : null
        ]) : null
      ]),
      showCancelButton: true,
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      beforeClose: (action: string, _instance: unknown, done: () => void) => {
        if (action === 'confirm' && deliveryMethod.value === 'DELIVERY' && !selectedAddressId.value && !customAddress.value.trim()) {
          ElMessage.error('请选择或输入配送地址')
          return
        }
        done()
      }
    })
    buying.value = true
    const data: { goodsId: number; quantity: number; remark?: string; deliveryMethod?: string; deliveryAddress?: string } = { goodsId: goods.value.id, quantity: buyQuantity.value }
    if (deliveryMethod.value === 'DELIVERY') {
      data.deliveryMethod = 'DELIVERY'
      if (selectedAddressId.value) {
        const addr = addressList.find(a => a.id === selectedAddressId.value)
        data.deliveryAddress = addr ? [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') + ` (${addr.receiverName} ${addr.receiverPhone})` : ''
      } else {
        data.deliveryAddress = customAddress.value
      }
    } else {
      data.deliveryMethod = 'PICKUP'
    }
    await createOrder(data)
    cartStore.fetchCartCount()
    loadData()
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
</style>

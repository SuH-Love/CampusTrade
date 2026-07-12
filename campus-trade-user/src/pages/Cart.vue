<template>
  <div class="cart-page">
    <el-card>
      <template #header>
        <div class="cart-header">
          <h3 class="cart-heading">购物车</h3>
          <div class="cart-header-actions">
            <el-checkbox :model-value="isAllSelected" :indeterminate="isIndeterminate" @change="handleSelectAll">全选</el-checkbox>
            <el-button type="danger" size="small" @click="handleClearCart" :disabled="cartList.length === 0">清空购物车</el-button>
          </div>
        </div>
      </template>
      <el-empty v-if="cartList.length === 0 && !loading" description="购物车是空的" />
      <div v-else class="cart-list" v-loading="loading">
        <div v-for="item in cartList" :key="item.id" class="cart-item" :class="{ 'cart-item--selected': selectedIds.includes(item.id) }">
          <el-checkbox :model-value="selectedIds.includes(item.id)" @change="(val: boolean | string | number) => toggleSelect(item.id, val)" />
          <el-image :src="item.coverImage || '/default-cover.svg'" class="cart-image" fit="cover" @click="$router.push(`/goods/${item.goodsId}`)" />
          <div class="cart-info" @click="$router.push(`/goods/${item.goodsId}`)">
            <div class="cart-title">{{ item.title }}</div>
            <div class="cart-price">¥{{ item.price }}</div>
          </div>
          <div class="cart-quantity">
            <el-input-number v-model="item.quantity" :min="1" :max="item.stock || 1" size="small" @change="handleUpdateQuantity(item)" :disabled="(item.stock || 1) <= 1" />
          </div>
          <div class="cart-actions">
            <el-button type="primary" size="small" @click.stop="handleCheckout(item)" :disabled="item.status !== 'ONLINE'" round>结算</el-button>
            <el-button type="danger" size="small" @click.stop="handleRemove(item.id)" round>删除</el-button>
          </div>
        </div>
      </div>
      <div v-if="cartList.length > 0" class="cart-footer">
        <span class="cart-total">合计：<span class="total-price">¥{{ selectedTotalPrice }}</span></span>
        <el-button type="primary" size="large" @click="handleBatchCheckout" :disabled="selectedIds.length === 0" round>批量结算({{ selectedIds.length }})</el-button>
      </div>
    </el-card>

    <el-dialog v-model="deliveryDialogVisible" :title="deliveryDialogTitle" width="520px" class="delivery-dialog">
      <el-form :model="deliveryForm" label-width="80px" class="delivery-form">
        <el-form-item label="商品价格">
          <span class="delivery-price">¥{{ deliveryForm.price }}</span>
        </el-form-item>
        <el-form-item label="配送方式">
          <el-radio-group v-model="deliveryForm.method">
            <el-radio label="PICKUP">自取</el-radio>
            <el-radio label="DELIVERY">配送</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="deliveryForm.method === 'DELIVERY'">
          <el-form-item label="选择地址">
            <div class="address-list" v-if="addressList.length > 0">
              <div v-for="addr in addressList" :key="addr.id" class="address-item" :class="{ 'address-item--active': deliveryForm.selectedAddressId === addr.id }" @click="selectAddress(addr.id)">
                <div class="address-name">{{ addr.receiverName }} {{ addr.receiverPhone }}</div>
                <div class="address-detail">{{ [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') }}</div>
              </div>
            </div>
            <el-button type="primary" link class="add-addr-trigger" @click="showAddAddrForm = true">新增收货地址</el-button>
          </el-form-item>
          <div v-if="showAddAddrForm" class="add-addr-form">
            <el-form :model="addrForm" label-width="80px" size="small">
              <el-form-item label="收货人" required><el-input v-model="addrForm.receiverName" placeholder="请输入收货人" /></el-form-item>
              <el-form-item label="手机号" required><el-input v-model="addrForm.receiverPhone" placeholder="请输入手机号" /></el-form-item>
              <el-form-item label="所在地区" required>
                <el-cascader v-model="areaValue" :options="areaData" placeholder="请选择省/市/区" clearable class="area-cascader" />
              </el-form-item>
              <el-form-item label="详细地址" required><el-input v-model="addrForm.detailAddress" placeholder="请输入详细地址" /></el-form-item>
              <el-form-item><el-button type="primary" @click="handleAddAddress">保存地址</el-button></el-form-item>
            </el-form>
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="deliveryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDelivery">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getCartList, updateCartQuantity, removeFromCart, clearCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import type { CartVO } from '@/api/cart'
import { getAddressList, addAddress, type DeliveryAddressVO } from '@/api/address'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { areaData } from '@/data/area'

const cartStore = useCartStore()

const cartList = ref<CartVO[]>([])
const loading = ref(false)
const selectedIds = ref<number[]>([])

const selectedTotalPrice = computed(() => {
  const ids = new Set(selectedIds.value)
  return cartList.value.filter(i => ids.has(i.id)).reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
})

const isAllSelected = computed(() => cartList.value.length > 0 && selectedIds.value.length === cartList.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < cartList.value.length)

const toggleSelect = (id: number, checked: boolean | string | number) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

const handleSelectAll = (checked: boolean | string | number) => {
  selectedIds.value = checked ? cartList.value.map(i => i.id) : []
}

const loadData = async () => {
  loading.value = true
  try { cartList.value = await getCartList() || [] } finally { loading.value = false }
}

watch(cartList, (newList) => {
  const validIds = new Set(newList.map(i => i.id))
  selectedIds.value = selectedIds.value.filter(id => validIds.has(id))
})

const handleUpdateQuantity = async (item: CartVO) => {
  try { await updateCartQuantity(item.id, item.quantity) } catch { loadData() }
}

const handleRemove = async (id: number) => {
  await ElMessageBox.confirm('确认从购物车移除？', '移除确认')
  await removeFromCart(id)
  ElMessage.success('已移除')
  cartStore.fetchCartCount()
  loadData()
}

const handleClearCart = async () => {
  await ElMessageBox.confirm('确认清空购物车？', '清空确认')
  await clearCart()
  ElMessage.success('已清空')
  cartStore.fetchCartCount()
  loadData()
}

const deliveryDialogVisible = ref(false)
const deliveryDialogTitle = ref('')
const deliveryForm = reactive({
  method: 'PICKUP' as string,
  selectedAddressId: null as number | null,
  price: 0
})
const checkoutItems = ref<CartVO[]>([])
const addressList = ref<DeliveryAddressVO[]>([])
const showAddAddrForm = ref(false)
const addrForm = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})
const areaValue = ref<string[]>([])

const openDeliveryDialog = async (items: CartVO[]) => {
  checkoutItems.value = items
  deliveryDialogTitle.value = items.length === 1 ? `确认购买「${items[0].title}」` : '批量结算'
  deliveryForm.method = 'PICKUP'
  deliveryForm.selectedAddressId = null
  deliveryForm.price = items.reduce((sum, i) => sum + i.price * i.quantity, 0)
  showAddAddrForm.value = false
  areaValue.value = []
  Object.assign(addrForm, { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
  try { addressList.value = await getAddressList() } catch { addressList.value = [] }
  deliveryDialogVisible.value = true
}

const selectAddress = (id: number) => {
  deliveryForm.selectedAddressId = id
}

const handleAddAddress = async () => {
  if (!addrForm.receiverName || !addrForm.receiverPhone || !addrForm.detailAddress) {
    ElMessage.error('请填写收货人、手机号和详细地址')
    return
  }
  if (areaValue.value.length < 3) {
    ElMessage.error('请选择完整的省/市/区')
    return
  }
  addrForm.province = areaValue.value[0]
  addrForm.city = areaValue.value[1]
  addrForm.district = areaValue.value[2]
  try {
    await addAddress(addrForm)
    addressList.value = await getAddressList()
    const newest = addressList.value[0]
    if (newest) deliveryForm.selectedAddressId = newest.id
    showAddAddrForm.value = false
    ElMessage.success('地址添加成功')
  } catch {
    ElMessage.error('添加失败')
  }
}

const confirmDelivery = async () => {
  if (deliveryForm.method === 'DELIVERY' && !deliveryForm.selectedAddressId) {
    ElMessage.error('请选择配送地址')
    return
  }
  let addr = ''
  if (deliveryForm.method === 'DELIVERY' && deliveryForm.selectedAddressId) {
    const found = addressList.value.find(a => a.id === deliveryForm.selectedAddressId)
    if (found) {
      addr = [found.province, found.city, found.district, found.detailAddress].filter(Boolean).join(' ') + ` (${found.receiverName} ${found.receiverPhone})`
    }
  }
  deliveryDialogVisible.value = false
  for (const item of checkoutItems.value) {
    try {
      const data: { goodsId: number; quantity: number; deliveryMethod: string; deliveryAddress?: string } = {
        goodsId: item.goodsId,
        quantity: item.quantity,
        deliveryMethod: deliveryForm.method
      }
      if (deliveryForm.method === 'DELIVERY') data.deliveryAddress = addr
      await createOrder(data)
      await removeFromCart(item.id)
    } catch { /* continue */ }
  }
  ElMessage.success(checkoutItems.value.length === 1 ? '下单成功' : '批量下单成功')
  cartStore.fetchCartCount()
  loadData()
}

const handleCheckout = (item: CartVO) => {
  openDeliveryDialog([item])
}

const handleBatchCheckout = () => {
  const items = cartList.value.filter(i => selectedIds.value.includes(i.id) && i.status === 'ONLINE')
  if (items.length === 0) {
    ElMessage.warning('请选择可结算的商品')
    return
  }
  openDeliveryDialog(items)
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.cart-page { padding: 20px; }
.cart-header { display: flex; justify-content: space-between; align-items: center; }
.cart-heading { margin: 0; }
.cart-header-actions { display: flex; align-items: center; gap: 12px; }
.cart-list { max-height: 600px; overflow-y: auto; }
.cart-item {
  display: flex; align-items: center; gap: 16px;
  padding: 18px; border-bottom: 1px solid var(--border-light);
  &:last-child { border-bottom: none; }
  &:hover { background: var(--bg-hover); border-radius: var(--radius-sm); }
  transition: var(--transition-fast);
  &--selected { background: rgba(99, 102, 241, 0.04); border-radius: var(--radius-sm); }
}
.cart-image { width: 80px; height: 80px; border-radius: var(--radius-md); flex-shrink: 0; }
.cart-info { flex: 1; cursor: pointer; }
.cart-title { font-weight: 600; font-size: 15px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cart-price { color: var(--danger); font-weight: 700; font-size: 17px; margin-top: 4px; }
.cart-quantity { flex-shrink: 0; }
.cart-actions { display: flex; gap: 8px; flex-shrink: 0; }
.cart-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 0 0; margin-top: 20px; border-top: 2px solid var(--border);
}
.cart-total { font-size: 16px; color: var(--text-primary); font-weight: 500; }
.total-price { font-size: 26px; font-weight: 800; color: var(--danger); letter-spacing: -0.5px; }
.delivery-dialog {
  :deep(.el-dialog__body) { max-height: 60vh; overflow-y: auto; }
}
.delivery-price { font-size: 20px; font-weight: 700; color: var(--danger); }
.address-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 8px; }
.address-item {
  padding: 10px 14px; border: 1px solid var(--border); border-radius: var(--radius-md);
  cursor: pointer; transition: var(--transition);
  &:hover { border-color: var(--primary-light); }
  &--active { border-color: var(--primary); background: rgba(99, 102, 241, 0.06); }
}
.address-name { font-size: 14px; font-weight: 500; }
.address-detail { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.add-addr-trigger { margin-bottom: 8px; }
.add-addr-form {
  border: 1px solid var(--border); border-radius: var(--radius-md);
  padding: 16px; background: var(--bg-card);
}
.area-cascader { width: 100%; }
</style>

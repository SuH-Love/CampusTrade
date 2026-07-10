<template>
  <div class="cart-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">购物车</h3>
          <el-button type="danger" size="small" @click="handleClearCart" :disabled="cartList.length === 0">清空购物车</el-button>
        </div>
      </template>
      <el-empty v-if="cartList.length === 0 && !loading" description="购物车是空的" />
      <div v-else class="cart-list" v-loading="loading">
        <div v-for="item in cartList" :key="item.id" class="cart-item">
          <el-image :src="item.coverImage || '/default-cover.svg'" style="width: 80px; height: 80px; border-radius: 8px; flex-shrink: 0" fit="cover" @click="$router.push(`/goods/${item.goodsId}`)" />
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
        <span class="cart-total">合计：<span class="total-price">¥{{ totalPrice }}</span></span>
        <el-button type="primary" size="large" @click="handleBatchCheckout" round>批量结算</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'

import { getCartList, updateCartQuantity, removeFromCart, clearCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import type { CartVO } from '@/api/cart'
import { getAddressList, addAddress, type DeliveryAddressVO } from '@/api/address'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCartStore } from '@/stores/cart'
import { h } from 'vue'

const cartStore = useCartStore()


const cartList = ref<CartVO[]>([])
const loading = ref(false)

const totalPrice = computed(() => cartList.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2))

const loadData = async () => {
  loading.value = true
  try { cartList.value = await getCartList() || [] } finally { loading.value = false }
}

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

const showDeliveryDialog = async (title: string, price: number): Promise<{ deliveryMethod: string; deliveryAddress: string }> => {
  const deliveryMethod = ref('PICKUP')
  const selectedAddressId = ref<number | null>(null)
  const customAddress = ref('')
  let addressList: DeliveryAddressVO[] = []
  try { addressList = await getAddressList() } catch { /* ignore */ }
  const showAddAddr = ref(false)
  const addrForm = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
  return new Promise((resolve, reject) => {
    ElMessageBox({
      title,
      message: () => h('div', null, [
        h('p', { style: 'margin-bottom: 12px' }, `价格 ¥${price}`),
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
                  ElMessage.success('地址添加成功')
                } catch { ElMessage.error('添加失败') }
              }
            }, '保存地址')
          ]) : null
        ]) : null
      ]),
      showCancelButton: true,
      confirmButtonText: '确认下单',
      cancelButtonText: '取消',
      beforeClose: (action: string, _instance: unknown, done: () => void) => {
        if (action === 'confirm' && deliveryMethod.value === 'DELIVERY' && !selectedAddressId.value && !customAddress.value.trim()) {
          ElMessage.error('请选择或输入配送地址')
          return
        }
        done()
      }
    }).then(() => {
      let addr = ''
      if (deliveryMethod.value === 'DELIVERY') {
        if (selectedAddressId.value) {
          const found = addressList.find(a => a.id === selectedAddressId.value)
          addr = found ? [found.province, found.city, found.district, found.detailAddress].filter(Boolean).join(' ') + ` (${found.receiverName} ${found.receiverPhone})` : ''
        } else {
          addr = customAddress.value
        }
      }
      resolve({ deliveryMethod: deliveryMethod.value, deliveryAddress: addr })
    }).catch(() => {
      reject(new Error('cancel'))
    })
  })
}

const handleCheckout = async (item: CartVO) => {
  try {
    const { deliveryMethod, deliveryAddress } = await showDeliveryDialog(`确认购买「${item.title}」`, item.price * item.quantity)
    const data: { goodsId: number; quantity: number; deliveryMethod: string; deliveryAddress?: string } = { goodsId: item.goodsId, quantity: item.quantity, deliveryMethod }
    if (deliveryMethod === 'DELIVERY') data.deliveryAddress = deliveryAddress
    await createOrder(data)
    await removeFromCart(item.id)
    ElMessage.success('下单成功')
    cartStore.fetchCartCount()
    loadData()
  } catch { /* cancel */ }
}

const handleBatchCheckout = async () => {
  const onlineItems = cartList.value.filter(item => item.status === 'ONLINE')
  if (onlineItems.length === 0) { ElMessage.warning('没有可结算的商品'); return }
  try {
    const { deliveryMethod, deliveryAddress } = await showDeliveryDialog('批量结算', Number(totalPrice.value))
    for (const item of onlineItems) {
      try {
        const data: { goodsId: number; quantity: number; deliveryMethod: string; deliveryAddress?: string } = { goodsId: item.goodsId, quantity: item.quantity, deliveryMethod }
        if (deliveryMethod === 'DELIVERY') data.deliveryAddress = deliveryAddress
        await createOrder(data)
        await removeFromCart(item.id)
      } catch { /* continue */ }
    }
    ElMessage.success('批量下单成功')
    cartStore.fetchCartCount()
    loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.cart-page { padding: 20px; }
.cart-list { max-height: 600px; overflow-y: auto; }
.cart-item {
  display: flex; align-items: center; gap: 16px;
  padding: 18px; border-bottom: 1px solid var(--border-light);
  &:last-child { border-bottom: none; }
  &:hover { background: var(--bg-hover); border-radius: var(--radius-sm); }
  transition: var(--transition-fast);
}
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
</style>
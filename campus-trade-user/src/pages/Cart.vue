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
import { ref, computed, onMounted } from 'vue'

import { getCartList, updateCartQuantity, removeFromCart, clearCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import type { CartVO } from '@/api/cart'
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

const showDeliveryDialog = (title: string, price: number): Promise<{ deliveryMethod: string; deliveryAddress: string }> => {
  const deliveryMethod = ref('PICKUP')
  const deliveryAddress = ref('')
  return new Promise((resolve, reject) => {
    ElMessageBox({
      title,
      message: () => h('div', null, [
        h('p', { style: 'margin-bottom: 12px' }, `价格 ¥${price}`),
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
      confirmButtonText: '确认下单',
      cancelButtonText: '取消',
      beforeClose: (action: string, _instance: unknown, done: () => void) => {
        if (action === 'confirm' && deliveryMethod.value === 'DELIVERY' && !deliveryAddress.value.trim()) {
          ElMessage.error('请输入配送地址')
          return
        }
        done()
      }
    }).then(() => {
      resolve({ deliveryMethod: deliveryMethod.value, deliveryAddress: deliveryAddress.value })
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
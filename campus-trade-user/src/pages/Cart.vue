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
            <el-input-number v-model="item.quantity" :min="1" :max="99" size="small" @change="handleUpdateQuantity(item)" />
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
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, removeFromCart, clearCart } from '@/api/cart'
import { createOrder } from '@/api/order'
import type { CartVO } from '@/api/cart'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
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
  loadData()
}

const handleClearCart = async () => {
  await ElMessageBox.confirm('确认清空购物车？', '清空确认')
  await clearCart()
  ElMessage.success('已清空')
  loadData()
}

const handleCheckout = async (item: CartVO) => {
  await ElMessageBox.confirm(`确认购买「${item.title}」？价格 ¥${item.price}`, '确认购买')
  try {
    await createOrder({ goodsId: item.goodsId })
    ElMessage.success('下单成功')
    loadData()
  } catch { /* ignore */ }
}

const handleBatchCheckout = async () => {
  const onlineItems = cartList.value.filter(item => item.status === 'ONLINE')
  if (onlineItems.length === 0) { ElMessage.warning('没有可结算的商品'); return }
  for (const item of onlineItems) {
    try { await createOrder({ goodsId: item.goodsId }) } catch { /* continue */ }
  }
  ElMessage.success('批量下单成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.cart-page { padding: 20px; }
.cart-list { max-height: 600px; overflow-y: auto; }
.cart-item {
  display: flex; align-items: center; gap: 16px;
  padding: 16px; border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }
  &:hover { background: #fafafa; }
}
.cart-info { flex: 1; cursor: pointer; }
.cart-title { font-weight: 500; font-size: 15px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cart-price { color: #f56c6c; font-weight: 700; font-size: 16px; margin-top: 4px; }
.cart-quantity { flex-shrink: 0; }
.cart-actions { display: flex; gap: 8px; flex-shrink: 0; }
.cart-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 0 0; margin-top: 16px; border-top: 1px solid #f0f0f0;
}
.cart-total { font-size: 16px; color: var(--text-primary); }
.total-price { font-size: 24px; font-weight: 800; color: #f56c6c; }
</style>
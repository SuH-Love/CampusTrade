<template>
  <div class="order-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="detail-header">
          <h3 class="m-0">订单详情</h3>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      <template v-if="order">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="orderStatusTagType(order.status)">{{ orderStatusLabel(order.status) }}</el-tag>
            <span v-if="order.status === 'PENDING_PAY' && countdownText" class="countdown-hint">{{ countdownText }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ order.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ order.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="金额">
            <span class="price-highlight">¥{{ order.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ order.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="配送方式">{{ order.deliveryMethod === 1 || order.deliveryMethod === 'DELIVERY' ? '配送' : '自取' }}</el-descriptions-item>
          <el-descriptions-item v-if="order.deliveryMethod === 1 || order.deliveryMethod === 'DELIVERY'" label="配送地址">{{ order.deliveryAddress || order.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="order.trackingNo" label="物流单号">{{ order.trackingNo }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ order.finishTime || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="order.cancelTime" label="取消时间">{{ order.cancelTime }}</el-descriptions-item>
          <el-descriptions-item v-if="order.cancelReason" label="取消原因">{{ order.cancelReason }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="detail-section-title">商品信息</h4>
        <el-table :data="order.items || []" stripe>
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div class="goods-item">
                <el-image v-if="row.goodsImage" :src="row.goodsImage" class="goods-item-img" fit="cover" alt="商品图片" />
                <span>{{ row.goodsTitle }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="成交价" min-width="100">
            <template #default="{ row }"><span class="price-bold">¥{{ row.price }}</span></template>
          </el-table-column>
        </el-table>

        <div class="action-bar" v-if="isBuyer || isSeller">
          <el-button v-if="order.status === 'PENDING_PAY' && isBuyer" type="primary" @click="handlePay">支付</el-button>
          <el-button v-if="order.status === 'PENDING_PAY' && isBuyer" @click="handleCancel">取消订单</el-button>
          <el-button v-if="order.status === 'PENDING_PAY' && isSeller" type="warning" @click="handleModifyPrice">改价</el-button>
          <el-button v-if="order.status === 'PAID' && isSeller" type="primary" @click="handleShip">发货</el-button>
          <el-button v-if="order.status === 'SHIPPING' && isBuyer" type="success" @click="handleFinish">确认收货</el-button>
          <el-button v-if="(order.status === 'PAID' || order.status === 'SHIPPING') && isBuyer" type="danger" @click="handleRefund">申请退款</el-button>
          <el-button v-if="order.status === 'REFUND' && isSeller" type="success" @click="handleApproveRefund">同意退款</el-button>
          <el-button v-if="order.status === 'REFUND' && isSeller" type="warning" @click="handleRejectRefund">拒绝退款</el-button>
        </div>

        <div v-if="order.status === 'PENDING_REVIEW' && isBuyer" class="rating-section">
          <h4 class="detail-section-title">评价卖家</h4>
          <el-form :model="ratingForm" label-width="80px">
            <el-form-item label="评分">
              <el-rate v-model="ratingForm.rating" />
            </el-form-item>
            <el-form-item label="评价">
              <el-input v-model="ratingForm.comment" type="textarea" :rows="3" placeholder="请输入评价内容（可选）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleRate" :loading="ratingLoading" :disabled="ratingForm.rating === 0">提交评价</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
      <EmptyState v-else-if="!loading" icon="📋" title="暂无评价" description="该订单暂无评价信息" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder, shipOrder, finishOrder, refundOrder, approveRefund, rejectRefund, rateOrder, modifyPrice } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { OrderVO } from '@/api/order'
import { orderStatusLabel, orderStatusTagType } from '@/utils/labels'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const userStore = useUserStore()
const order = ref<OrderVO | null>(null)
const loading = ref(false)
const ratingForm = reactive({ rating: 0, comment: '' })
const ratingLoading = ref(false)
const countdownText = ref('')
let countdownTimer: number | null = null

const ORDER_TIMEOUT_MS = 5 * 60 * 1000

const updateCountdown = () => {
  if (!order.value || order.value.status !== 'PENDING_PAY') { countdownText.value = ''; return }
  const created = new Date(order.value.createTime.includes('T') ? order.value.createTime : order.value.createTime.replace(' ', 'T')).getTime()
  const remaining = ORDER_TIMEOUT_MS - (Date.now() - created)
  if (remaining <= 0) { countdownText.value = '已超时'; loadData(); return }
  const minutes = Math.floor(remaining / 60000)
  const seconds = Math.floor((remaining % 60000) / 1000)
  countdownText.value = `剩余 ${minutes}:${String(seconds).padStart(2, '0')}`
}

const isBuyer = computed(() => order.value?.buyerId === userStore.userInfo?.id)
const isSeller = computed(() => order.value?.sellerId === userStore.userInfo?.id)


const loadData = async () => {
  loading.value = true
  try {
    order.value = await getOrderDetail(Number(route.params.id))
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  await ElMessageBox.confirm('确认支付该订单？', '支付确认')
  await payOrder(order.value!.id)
  ElMessage.success('支付成功')
  loadData()
}

const handleCancel = async () => {
  await ElMessageBox.confirm('确认取消该订单？', '取消确认')
  await cancelOrder(order.value!.id)
  ElMessage.success('已取消')
  loadData()
}

const handleShip = async () => {
  const { value } = await ElMessageBox.prompt('请输入物流运单号（可选）', '确认发货', {
    confirmButtonText: '确认发货',
    cancelButtonText: '取消',
    inputPlaceholder: '运单号（可留空）'
  }).catch(() => ({ value: null }))
  if (value === null) return
  await shipOrder(order.value!.id, value || undefined)
  ElMessage.success('已发货')
  loadData()
}

const handleModifyPrice = async () => {
  const { value } = await ElMessageBox.prompt('请输入新的订单金额', '修改订单金额', {
    confirmButtonText: '确认改价',
    cancelButtonText: '取消',
    inputValue: String(order.value!.totalAmount),
    inputPattern: /^\d+(\.\d{1,2})?$/,
    inputErrorMessage: '请输入有效的金额（最多两位小数）'
  })
  const newPrice = parseFloat(value)
  if (newPrice <= 0) { ElMessage.warning('金额必须大于0'); return }
  await modifyPrice(order.value!.id, newPrice)
  ElMessage.success('已修改订单金额')
  loadData()
}

const handleFinish = async () => {
  await ElMessageBox.confirm('确认已收到商品？', '收货确认')
  await finishOrder(order.value!.id)
  ElMessage.success('已确认收货')
  loadData()
}

const handleRefund = async () => {
  const { value } = await ElMessageBox.prompt('请输入退款原因', '申请退款', { inputPattern: /\S+/, inputErrorMessage: '退款原因不能为空' })
  await refundOrder(order.value!.id, value)
  ElMessage.success('已申请退款')
  loadData()
}

const handleApproveRefund = async () => {
  await ElMessageBox.confirm('确认同意退款？', '同意退款')
  await approveRefund(order.value!.id)
  ElMessage.success('已同意退款')
  loadData()
}

const handleRejectRefund = async () => {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝退款', { inputPattern: /\S+/, inputErrorMessage: '拒绝原因不能为空' })
  await rejectRefund(order.value!.id, value)
  ElMessage.success('已拒绝退款')
  loadData()
}

const handleRate = async () => {
  if (!order.value || ratingForm.rating === 0) return
  ratingLoading.value = true
  try {
    await rateOrder(order.value.id, ratingForm.rating, ratingForm.comment || undefined)
    ElMessage.success('评价成功')
    loadData()
  } finally { ratingLoading.value = false }
}

onMounted(() => { loadData(); countdownTimer = window.setInterval(updateCountdown, 1000) })
onUnmounted(() => { if (countdownTimer) clearInterval(countdownTimer) })
</script>

<style scoped lang="scss">
.order-detail-page { padding: 20px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; }
.countdown-hint { color: var(--danger); font-size: 13px; margin-left: 8px; font-weight: 500; }
.price-highlight { color: var(--danger); font-weight: bold; font-size: 18px; }
.price-bold { color: var(--danger); font-weight: bold; }
.detail-section-title { margin: 24px 0 12px; }
.goods-item { display: flex; align-items: center; gap: 12px; }
.goods-item-img { width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0; }
.action-bar { margin-top: 24px; display: flex; gap: 12px; }
.rating-section {
  margin-top: 24px;
  padding: 24px;
  background: var(--bg-glass);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  :deep(.el-rate) { --el-rate-primary-color: var(--primary); }
}
</style>
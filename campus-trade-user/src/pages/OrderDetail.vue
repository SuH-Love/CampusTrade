<template>
  <div class="order-detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">订单详情</h3>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>
      <template v-if="order">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(order.status)">{{ statusLabel(order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ order.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ order.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="金额">
            <span style="color: #f56c6c; font-weight: bold; font-size: 18px">¥{{ order.totalAmount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注">{{ order.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="配送方式">{{ order.deliveryMethod === 1 || order.deliveryMethod === 'DELIVERY' ? '配送' : '自取' }}</el-descriptions-item>
          <el-descriptions-item v-if="order.deliveryMethod === 1 || order.deliveryMethod === 'DELIVERY'" label="配送地址">{{ order.deliveryAddress || order.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ order.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ order.shipTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ order.finishTime || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="order.cancelTime" label="取消时间">{{ order.cancelTime }}</el-descriptions-item>
          <el-descriptions-item v-if="order.cancelReason" label="取消原因">{{ order.cancelReason }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin: 24px 0 12px">商品信息</h4>
        <el-table :data="order.items || []" stripe>
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div style="display: flex; align-items: center; gap: 12px">
                <el-image v-if="row.goodsImage" :src="row.goodsImage" style="width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0" fit="cover" />
                <span>{{ row.goodsTitle }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="成交价" width="120">
            <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold">¥{{ row.price }}</span></template>
          </el-table-column>
        </el-table>

        <div style="margin-top: 24px; display: flex; gap: 12px" v-if="isBuyer || isSeller">
          <el-button v-if="order.status === 'PENDING_PAY' && isBuyer" type="primary" @click="handlePay">支付</el-button>
          <el-button v-if="order.status === 'PENDING_PAY' && isBuyer" @click="handleCancel">取消订单</el-button>
          <el-button v-if="order.status === 'PAID' && isSeller" type="primary" @click="handleShip">发货</el-button>
          <el-button v-if="order.status === 'SHIPPING' && isBuyer" type="success" @click="handleFinish">确认收货</el-button>
          <el-button v-if="(order.status === 'PAID' || order.status === 'SHIPPING') && isBuyer" type="danger" @click="handleRefund">申请退款</el-button>
          <el-button v-if="order.status === 'REFUND' && isSeller" type="success" @click="handleApproveRefund">同意退款</el-button>
          <el-button v-if="order.status === 'REFUND' && isSeller" type="warning" @click="handleRejectRefund">拒绝退款</el-button>
        </div>

        <div v-if="order.status === 'PENDING_REVIEW' && isBuyer" class="rating-section">
          <h4 style="margin: 24px 0 12px">评价卖家</h4>
          <el-form :model="ratingForm" label-width="80px">
            <el-form-item label="评分">
              <el-rate v-model="ratingForm.rating" :colors="['#6366f1', '#6366f1', '#6366f1']" />
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
      <el-empty v-else-if="!loading" description="订单不存在" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail, payOrder, cancelOrder, shipOrder, finishOrder, refundOrder, approveRefund, rejectRefund, rateOrder } from '@/api/order'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { OrderVO } from '@/api/order'

const route = useRoute()
const userStore = useUserStore()
const order = ref<OrderVO | null>(null)
const loading = ref(false)
const ratingForm = reactive({ rating: 0, comment: '' })
const ratingLoading = ref(false)

const isBuyer = computed(() => order.value?.buyerId === userStore.userInfo?.id)
const isSeller = computed(() => order.value?.sellerId === userStore.userInfo?.id)

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货',
    PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中'
  }
  return map[status] || status
}

const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    PENDING_PAY: 'warning', PAID: 'primary', SHIPPING: '',
    PENDING_REVIEW: 'warning', FINISHED: 'success', CANCELLED: 'info', REFUND: 'danger'
  }
  return map[status] || ''
}

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
  await shipOrder(order.value!.id)
  ElMessage.success('已发货')
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

onMounted(loadData)
</script>

<style scoped lang="scss">
.order-detail-page { padding: 20px; }
.rating-section {
  margin-top: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
</style>
<template>
  <div class="orders-page page-bg">
    <el-card>
      <template #header>
        <div class="orders-header">
          <h3 class="orders-heading">我的订单</h3>
          <div class="filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索订单号" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch" />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="status-select">
              <el-option label="全部" value="" />
              <el-option label="待支付" value="PENDING_PAY" />
              <el-option label="待发货" value="PAID" />
              <el-option label="待收货" value="SHIPPING" />
              <el-option label="待评价" value="PENDING_REVIEW" />
              <el-option label="已完成" value="FINISHED" />
              <el-option label="已取消" value="CANCELLED" />
              <el-option label="退款售后" value="REFUND" />
            </el-select>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的" name="seller" />
      </el-tabs>

      <el-table v-if="!isMobile" :data="filteredOrders" stripe class="order-table" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column label="商品信息" min-width="200">
          <template #default="{ row }">
            <div v-if="row.items && row.items.length > 0" class="goods-cell">
              <el-image v-if="row.items[0].goodsImage" :src="row.items[0].goodsImage" class="goods-thumb" fit="cover" />
              <div class="goods-title-cell">{{ row.items[0].goodsTitle }}</div>
              <el-tag v-if="row.items.length > 1" size="small" type="info" class="goods-more-tag">+{{ row.items.length - 1 }}</el-tag>
            </div>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="activeTab === 'buyer' ? '卖家' : '买家'" min-width="100">
          <template #default="{ row }">{{ activeTab === 'buyer' ? row.sellerName : row.buyerName }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" min-width="80">
          <template #default="{ row }"><span class="amount-text">{{ formatPrice(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="配送方式" min-width="90">
          <template #default="{ row }">
            <el-tag v-if="row.deliveryMethod === 'DELIVERY' || row.deliveryMethod === 1" size="small" type="primary">配送</el-tag>
            <el-tag v-else-if="row.deliveryMethod === 'PICKUP' || row.deliveryMethod === 2" size="small" type="success">自取</el-tag>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="orderStatusTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
            <div v-if="row.status === 'PENDING_PAY' && countdownMap.get(row.id)" class="countdown-text">{{ countdownMap.get(row.id) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column prop="cancelReason" label="取消原因" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.cancelReason" class="cancel-reason">{{ row.cancelReason }}</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/order/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" type="primary" size="small" @click="handlePay(row.id)">支付</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" size="small" @click="handleCancel(row.id)">取消</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'seller'" type="warning" size="small" @click="handleModifyPrice(row.id, row.totalAmount)">改价</el-button>
            <el-button v-if="row.status === 'PAID' && activeTab === 'seller'" type="primary" size="small" @click="handleShip(row.id)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPING' && activeTab === 'buyer'" type="success" size="small" @click="handleFinish(row.id)">确认收货</el-button>
            <el-button v-if="(row.status === 'PAID' || row.status === 'SHIPPING') && activeTab === 'buyer'" type="danger" size="small" @click="handleRefund(row.id)">退款</el-button>
            <el-button v-if="row.status === 'REFUND' && activeTab === 'seller'" type="success" size="small" @click="handleApproveRefund(row.id)">同意退款</el-button>
            <el-button v-if="row.status === 'REFUND' && activeTab === 'seller'" type="warning" size="small" @click="handleRejectRefund(row.id)">拒绝退款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isMobile" class="order-cards" v-loading="loading">
        <div v-for="row in filteredOrders" :key="row.id" class="order-card">
          <div class="order-card-header">
            <span class="order-card-no">{{ row.orderNo }}</span>
            <el-tag :type="orderStatusTagType(row.status)" size="small">{{ orderStatusLabel(row.status) }}</el-tag>
          </div>
          <div class="order-card-body">
            <div v-if="row.items && row.items.length > 0" class="order-card-goods">
              <el-image v-if="row.items[0].goodsImage" :src="row.items[0].goodsImage" class="order-card-img" fit="cover" />
              <div class="order-card-info">
                <div class="order-card-title">{{ row.items[0].goodsTitle }}</div>
                <el-tag v-if="row.items.length > 1" size="small" type="info">+{{ row.items.length - 1 }}</el-tag>
              </div>
            </div>
            <div class="order-card-meta">
              <span class="amount-text">{{ formatPrice(row.totalAmount) }}</span>
              <el-tag v-if="row.deliveryMethod === 'DELIVERY' || row.deliveryMethod === 1" size="small" type="primary">配送</el-tag>
              <el-tag v-else-if="row.deliveryMethod === 'PICKUP' || row.deliveryMethod === 2" size="small" type="success">自取</el-tag>
            </div>
            <div class="order-card-user">{{ activeTab === 'buyer' ? row.sellerName : row.buyerName }} · {{ formatTime(row.createTime) }}</div>
          </div>
          <div v-if="row.status === 'PENDING_PAY' && countdownMap.get(row.id)" class="order-card-countdown">{{ countdownMap.get(row.id) }}</div>
          <div class="order-card-actions">
            <el-button size="small" @click="$router.push(`/order/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" type="primary" size="small" @click="handlePay(row.id)">支付</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" size="small" @click="handleCancel(row.id)">取消</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'seller'" type="warning" size="small" @click="handleModifyPrice(row.id, row.totalAmount)">改价</el-button>
            <el-button v-if="row.status === 'PAID' && activeTab === 'seller'" type="primary" size="small" @click="handleShip(row.id)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPING' && activeTab === 'buyer'" type="success" size="small" @click="handleFinish(row.id)">确认收货</el-button>
            <el-button v-if="(row.status === 'PAID' || row.status === 'SHIPPING') && activeTab === 'buyer'" type="danger" size="small" @click="handleRefund(row.id)">退款</el-button>
            <el-button v-if="row.status === 'REFUND' && activeTab === 'seller'" type="success" size="small" @click="handleApproveRefund(row.id)">同意退款</el-button>
            <el-button v-if="row.status === 'REFUND' && activeTab === 'seller'" type="warning" size="small" @click="handleRejectRefund(row.id)">拒绝退款</el-button>
          </div>
        </div>
      </div>

      <EmptyState v-if="filteredOrders.length === 0 && !loading" icon="📦" title="暂无订单" description="快去挑选心仪的商品吧" action-text="去购物" @action="$router.push('/goods')" />
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="order-pagination" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getBuyerOrders, getSellerOrders, payOrder, createPayment, cancelOrder, shipOrder, finishOrder, refundOrder, approveRefund, rejectRefund, modifyPrice, getOrderDetail } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { OrderVO } from '@/api/order'
import type { OrderQueryParams } from '@/types'
import { formatPrice, formatTime, orderStatusLabel, orderStatusTagType } from '@/utils/labels'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const activeTab = ref((route.query.tab as string) || 'buyer')
const orders = ref<OrderVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref((route.query.status as string) || '')
const isMobile = ref(window.innerWidth < 768)

const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}


const filteredOrders = computed(() => {
  let list = orders.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(o => o.orderNo.toLowerCase().includes(kw))
  }
  return list
})

const loadData = async () => {
  loading.value = true
  try {
    const params: OrderQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    if (activeTab.value === 'buyer') {
      const res = await getBuyerOrders(params)
      orders.value = (res.list || []).filter((o: OrderVO) => !statusFilter.value || o.status === statusFilter.value)
      total.value = res.total || 0
    } else {
      const res = await getSellerOrders(params)
      orders.value = (res.list || []).filter((o: OrderVO) => !statusFilter.value || o.status === statusFilter.value)
      total.value = res.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  loadData()
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handlePay = async (id: number) => {
  try {
    const payForm = await createPayment(id)
    if (payForm) {
      const div = document.createElement('div')
      div.innerHTML = payForm
      document.body.appendChild(div)
      const form = div.querySelector('form')
      if (form) {
        form.submit()
        startPayPolling(id)
      } else {
        window.open(payForm, '_blank')
        startPayPolling(id)
      }
    }
  } catch (e: unknown) {
    const errMsg = e instanceof Error ? e.message : '支付失败'
    if (errMsg.includes('503') || errMsg.includes('未配置')) {
      await ElMessageBox.confirm('支付宝未配置，是否使用模拟支付？', '支付方式', { confirmButtonText: '模拟支付', cancelButtonText: '取消' })
      await payOrder(id)
      ElMessage.success('模拟支付成功')
      loadData()
    } else {
      ElMessage.error(errMsg)
    }
  }
}

const startPayPolling = (orderId: number) => {
  ElMessage.info('请在支付宝页面完成支付，支付完成后将自动刷新')
  let count = 0
  const timer = setInterval(async () => {
    count++
    if (count > 60) { clearInterval(timer); return }
    try {
      const order = await getOrderDetail(orderId)
      if (order.status !== 'PENDING_PAY') {
        clearInterval(timer)
        ElMessage.success('支付成功')
        loadData()
      }
    } catch { /* ignore */ }
  }, 2000)
}

const handleCancel = async (id: number) => {
  await ElMessageBox.confirm('确认取消该订单？', '取消确认')
  await cancelOrder(id)
  ElMessage.success('已取消')
  loadData()
}

const handleShip = async (id: number) => {
  const { value } = await ElMessageBox.prompt('请输入物流运单号（可选）', '确认发货', {
    confirmButtonText: '确认发货',
    cancelButtonText: '取消',
    inputPlaceholder: '运单号（可留空）'
  }).catch(() => ({ value: null }))
  if (value === null) return
  await shipOrder(id, value || undefined)
  ElMessage.success('已发货')
  loadData()
}

const handleModifyPrice = async (id: number, currentPrice: number) => {
  const { value } = await ElMessageBox.prompt('请输入新的订单金额', '修改订单金额', {
    confirmButtonText: '确认改价',
    cancelButtonText: '取消',
    inputValue: String(currentPrice),
    inputPattern: /^\d+(\.\d{1,2})?$/,
    inputErrorMessage: '请输入有效的金额（最多两位小数）'
  })
  const newPrice = parseFloat(value)
  if (newPrice <= 0) { ElMessage.warning('金额必须大于0'); return }
  await modifyPrice(id, newPrice)
  ElMessage.success('已修改订单金额')
  loadData()
}

const handleFinish = async (id: number) => {
  await ElMessageBox.confirm('确认已收到商品？', '收货确认')
  await finishOrder(id)
  ElMessage.success('已确认收货')
  loadData()
}

const handleRefund = async (id: number) => {
  const { value } = await ElMessageBox.prompt('请输入退款原因', '申请退款', { inputPattern: /\S+/, inputErrorMessage: '退款原因不能为空' })
  await refundOrder(id, value)
  ElMessage.success('已申请退款')
  loadData()
}

const handleApproveRefund = async (id: number) => {
  await ElMessageBox.confirm('确认同意退款？退款后商品将重新上架', '同意退款')
  await approveRefund(id)
  ElMessage.success('已同意退款')
  loadData()
}

const handleRejectRefund = async (id: number) => {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝退款', { inputPattern: /\S+/, inputErrorMessage: '拒绝原因不能为空' })
  await rejectRefund(id, value)
  ElMessage.success('已拒绝退款')
  loadData()
}

const ORDER_TIMEOUT_MS = 5 * 60 * 1000
let countdownTimer: number | null = null
const countdownMap = reactive(new Map<number, string>())

const updateCountdowns = () => {
  let hasExpired = false
  for (const order of orders.value) {
    if (order.status === 'PENDING_PAY') {
      const created = new Date(order.createTime.includes('T') ? order.createTime : order.createTime.replace(' ', 'T')).getTime()
      const remaining = ORDER_TIMEOUT_MS - (Date.now() - created)
      if (remaining <= 0) {
        countdownMap.set(order.id, '已超时')
        hasExpired = true
      } else {
        const minutes = Math.floor(remaining / 60000)
        const seconds = Math.floor((remaining % 60000) / 1000)
        countdownMap.set(order.id, `剩余 ${minutes}:${String(seconds).padStart(2, '0')}`)
      }
    } else {
      countdownMap.delete(order.id)
    }
  }
  if (hasExpired) loadData()
}

onMounted(() => {
  loadData()
  countdownTimer = window.setInterval(updateCountdowns, 1000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.orders-page {
  padding: 20px;

  :deep(.el-card) {
    border-radius: 16px;
    border: 1px solid var(--border);
    box-shadow: var(--shadow-sm);
  }
}
.orders-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.orders-heading { margin: 0; }
.filter-bar { display: flex; gap: 12px; align-items: center; }
.search-input { width: 200px; }
.status-select { width: 140px; }
.order-table { width: 100%; }
.goods-cell { display: flex; align-items: center; gap: 10px; }
.goods-thumb { width: 44px; height: 44px; border-radius: 6px; flex-shrink: 0; }
.goods-title-cell { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 500; }
.goods-more-tag { flex-shrink: 0; }
.amount-text { color: var(--danger); font-weight: bold; }
.text-placeholder { color: var(--text-muted); }
.countdown-text { color: var(--danger); font-size: 12px; margin-top: 4px; font-weight: 500; }
.cancel-reason { color: var(--danger); }
.order-pagination { margin-top: 16px; justify-content: center; }

.order-cards { display: flex; flex-direction: column; gap: 12px; }
.order-card {
  background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border);
  padding: var(--spacing-md); transition: var(--transition);
  &:hover { border-color: var(--primary-light); box-shadow: var(--shadow-sm); }
}
.order-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.order-card-no { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
.order-card-body { display: flex; flex-direction: column; gap: 8px; }
.order-card-goods { display: flex; align-items: center; gap: 10px; }
.order-card-img { width: 56px; height: 56px; border-radius: var(--radius-sm); flex-shrink: 0; }
.order-card-info { flex: 1; overflow: hidden; }
.order-card-title { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-card-meta { display: flex; align-items: center; gap: 8px; }
.order-card-user { font-size: 13px; color: var(--text-muted); }
.order-card-countdown { color: var(--danger); font-size: 12px; font-weight: 500; margin-top: 4px; }
.order-card-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); }
</style>

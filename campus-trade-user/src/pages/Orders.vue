<template>
  <div class="orders-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
          <h3 style="margin: 0">我的订单</h3>
          <div class="filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索订单号" clearable style="width: 200px" @keyup.enter="handleSearch" @clear="handleSearch" />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" style="width: 140px">
              <el-option label="待支付" value="PENDING_PAY" />
              <el-option label="已支付" value="PAID" />
              <el-option label="已发货" value="SHIPPING" />
              <el-option label="已完成" value="FINISHED" />
              <el-option label="退款中" value="REFUND" />
              <el-option label="已取消" value="CANCELLED" />
            </el-select>
          </div>
        </div>
      </template>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的" name="seller" />
      </el-tabs>
      <el-table :data="filteredOrders" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column :label="activeTab === 'buyer' ? '卖家' : '买家'" width="120">
          <template #default="{ row }">{{ activeTab === 'buyer' ? row.sellerName : row.buyerName }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold">¥{{ row.totalAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/order/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" type="primary" size="small" @click="handlePay(row.id)">支付</el-button>
            <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" size="small" @click="handleCancel(row.id)">取消</el-button>
            <el-button v-if="row.status === 'PAID' && activeTab === 'seller'" type="primary" size="small" @click="handleShip(row.id)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPING' && activeTab === 'buyer'" type="success" size="small" @click="handleFinish(row.id)">确认收货</el-button>
            <el-button v-if="(row.status === 'PAID' || row.status === 'SHIPPING') && activeTab === 'buyer'" type="danger" size="small" @click="handleRefund(row.id)">退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="filteredOrders.length === 0 && !loading" description="暂无订单" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getBuyerOrders, getSellerOrders, payOrder, cancelOrder, shipOrder, finishOrder, refundOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { OrderVO } from '@/api/order'
import type { OrderQueryParams } from '@/types'

const activeTab = ref('buyer')
const orders = ref<OrderVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const searchKeyword = ref('')
const statusFilter = ref('')

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货',
    FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中'
  }
  return map[status] || status
}

const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    PENDING_PAY: 'warning', PAID: 'primary', SHIPPING: '',
    FINISHED: 'success', CANCELLED: 'info', REFUND: 'danger'
  }
  return map[status] || ''
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
  await ElMessageBox.confirm('确认支付该订单？', '支付确认')
  await payOrder(id)
  ElMessage.success('支付成功')
  loadData()
}

const handleCancel = async (id: number) => {
  await ElMessageBox.confirm('确认取消该订单？', '取消确认')
  await cancelOrder(id)
  ElMessage.success('已取消')
  loadData()
}

const handleShip = async (id: number) => {
  await shipOrder(id)
  ElMessage.success('已发货')
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

onMounted(loadData)
</script>

<style scoped lang="scss">
.orders-page { padding: 20px; }
.filter-bar { display: flex; gap: 12px; align-items: center; }
</style>

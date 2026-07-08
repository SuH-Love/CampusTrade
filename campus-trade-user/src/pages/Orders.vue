<template>
  <div class="orders-page">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="我买到的" name="buyer" />
      <el-tab-pane label="我卖出的" name="seller" />
      <el-tab-pane label="已取消" name="cancelled" />
    </el-tabs>
    <el-table :data="orders" stripe style="width: 100%">
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/order/${row.id}`)">详情</el-button>
          <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" type="primary" size="small" @click="handlePay(row.id)">支付</el-button>
          <el-button v-if="row.status === 'PENDING_PAY' && activeTab === 'buyer'" size="small" @click="handleCancel(row.id)">取消</el-button>
          <el-button v-if="row.status === 'PAID' && activeTab === 'seller'" type="primary" size="small" @click="handleShip(row.id)">发货</el-button>
          <el-button v-if="row.status === 'SHIPPING' && activeTab === 'buyer'" type="success" size="small" @click="handleFinish(row.id)">确认收货</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="orders.length === 0" description="暂无订单" />
    <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getBuyerOrders, getSellerOrders, payOrder, cancelOrder, shipOrder, finishOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { OrderVO } from '@/api/order'

const activeTab = ref('buyer')
const orders = ref<OrderVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

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

const loadData = async () => {
  const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (activeTab.value === 'cancelled') {
    params.status = 'CANCELLED'
    const [bRes, sRes] = await Promise.all([getBuyerOrders(params), getSellerOrders(params)])
    orders.value = [...(bRes.list || []), ...(sRes.list || [])]
    total.value = (bRes.total || 0) + (sRes.total || 0)
  } else {
    const res = activeTab.value === 'buyer' ? await getBuyerOrders(params) : await getSellerOrders(params)
    orders.value = (res.list || []).filter((o: any) => o.status !== 'CANCELLED')
    total.value = res.total || 0
  }
}

const handleTabChange = () => {
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

onMounted(loadData)
</script>

<style scoped lang="scss">
.orders-page { padding: 20px; }
</style>

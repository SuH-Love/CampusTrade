<template>
  <div class="order-manage-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>订单管理</h3>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadData" style="width: 160px">
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已发货" value="SHIPPING" />
            <el-option label="待评价" value="PENDING_REVIEW" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="退款中" value="REFUND" />
          </el-select>
        </div>
      </template>
      <el-table :data="orders" stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="buyerName" label="买家" min-width="90" />
        <el-table-column prop="sellerName" label="卖家" min-width="90" />
        <el-table-column prop="totalAmount" label="金额" min-width="100">
          <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold">¥{{ row.totalAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'" effect="dark" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配送" min-width="80">
          <template #default="{ row }">
            <el-tag v-if="row.deliveryMethod === 1" size="small" type="primary">配送</el-tag>
            <el-tag v-else-if="row.deliveryMethod === 0 || row.deliveryMethod === 2" size="small" type="success">自取</el-tag>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="配送地址" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.address">{{ row.address }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="cancelReason" label="取消原因" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.cancelReason" style="color: #f56c6c">{{ row.cancelReason }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'REFUND'" type="success" size="small" @click="handleApproveRefund(row.id)">同意退款</el-button>
            <el-button v-if="row.status === 'REFUND'" type="warning" size="small" @click="handleRejectRefund(row.id)">拒绝退款</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="orders.length === 0" description="暂无订单" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderList, approveRefund, rejectRefund } from '@/api/admin'
import type { AdminOrderVO, PageQueryParams } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref<AdminOrderVO[]>([])
const statusFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const statusTagMap: Record<string, string> = {
  PENDING_PAY: 'warning', PAID: 'primary', SHIPPING: '',
  PENDING_REVIEW: 'warning', FINISHED: 'success', CANCELLED: 'info', REFUND: 'danger'
}
const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    PENDING_PAY: '待支付', PAID: '已支付', SHIPPING: '已发货',
    PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消', REFUND: '退款中'
  }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
  const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await getOrderList(params)
  orders.value = res.list || []
  total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const handleApproveRefund = async (id: number) => {
  await ElMessageBox.confirm('确认同意退款？', '同意退款')
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
</script>

<style scoped lang="scss">
.order-manage-page { padding: 20px; }
</style>

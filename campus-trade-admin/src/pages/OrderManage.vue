<template>
  <div class="order-manage-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>订单管理</h3>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadData" style="width: 160px">
            <el-option label="待支付" value="UNPAID" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已发货" value="SHIPPED" />
            <el-option label="已完成" value="FINISHED" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="退款中" value="REFUNDING" />
          </el-select>
        </div>
      </template>
      <el-table :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" width="200" />
        <el-table-column prop="buyerName" label="买家" width="100" />
        <el-table-column prop="sellerName" label="卖家" width="100" />
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold">¥{{ row.totalAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderList } from '@/api/admin'

const orders = ref<any[]>([])
const statusFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusTagMap: Record<string, string> = { UNPAID: 'warning', PAID: 'primary', SHIPPED: '', FINISHED: 'success', CANCELLED: 'info', REFUNDING: 'danger', REFUNDED: 'info' }
const statusLabel = (status: string) => {
  const map: Record<string, string> = { UNPAID: '待支付', PAID: '已支付', SHIPPED: '已发货', FINISHED: '已完成', CANCELLED: '已取消', REFUNDING: '退款中', REFUNDED: '已退款' }
  return map[status] || status
}

const loadData = async () => {
  const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await getOrderList(params)
  orders.value = res.list || []
  total.value = res.total || 0
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.order-manage-page { padding: 20px; }
</style>

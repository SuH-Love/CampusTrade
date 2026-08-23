<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>订单管理</h3>
          <div class="admin-filter-bar">
            <el-input v-model="searchOrderNo" placeholder="搜索订单号" clearable class="filter-input" @clear="handleSearch">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="filter-datepicker"
              @change="handleSearch"
            />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="filter-select">
              <el-option label="待支付" value="PENDING_PAY" />
              <el-option label="已支付" value="PAID" />
              <el-option label="已发货" value="SHIPPING" />
              <el-option label="待评价" value="PENDING_REVIEW" />
              <el-option label="已完成" value="FINISHED" />
              <el-option label="已取消" value="CANCELLED" />
              <el-option label="退款中" value="REFUND" />
            </el-select>
            <el-button @click="handleExportOrders">导出CSV</el-button>
          </div>
        </div>
      </template>
      <el-table :data="orders" stripe v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="buyerName" label="买家" min-width="90" />
        <el-table-column prop="sellerName" label="卖家" min-width="90" />
        <el-table-column prop="totalAmount" label="金额" min-width="100">
          <template #default="{ row }"><span class="price-text">¥{{ row.totalAmount }}</span></template>
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
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="配送地址" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.address">{{ row.address }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="cancelReason" label="取消原因" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.cancelReason" class="cancel-text">{{ row.cancelReason }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'REFUND'" type="success" size="small" @click="handleApproveRefund(row.id)">同意退款</el-button>
            <el-button v-if="row.status === 'REFUND'" type="warning" size="small" @click="openRejectRefundDialog(row.id)">拒绝退款</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无订单" :image-size="60" /></template>
      </el-table>
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, prev, pager, next, sizes"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="680px">
      <template v-if="detailOrder">
        <div v-if="detailOrder.items && detailOrder.items.length" class="order-goods-section">
          <h4 class="section-title">商品信息</h4>
          <div class="order-goods-list">
            <div v-for="item in detailOrder.items" :key="item.goodsId" class="order-goods-item">
              <el-image v-if="item.goodsImage" :src="item.goodsImage" fit="cover" class="order-goods-image" />
              <div class="order-goods-info">
                <span class="order-goods-title">{{ item.goodsTitle }}</span>
                <span class="order-goods-price">¥{{ item.price }} × {{ item.quantity }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusTagMap[detailOrder.status] || 'info'" effect="dark" round>{{ statusLabel(detailOrder.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="买家">{{ detailOrder.buyerName }}</el-descriptions-item>
          <el-descriptions-item label="卖家">{{ detailOrder.sellerName }}</el-descriptions-item>
          <el-descriptions-item label="金额"><span class="price-text">¥{{ detailOrder.totalAmount }}</span></el-descriptions-item>
          <el-descriptions-item label="配送">{{ deliveryMethodLabel(detailOrder.deliveryMethod) }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detailOrder.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailOrder.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" :span="2" v-if="detailOrder.cancelReason">{{ detailOrder.cancelReason }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detailOrder.payTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <ReasonDialog
      v-model="rejectRefundDialogVisible"
      title="拒绝退款"
      label="拒绝原因"
      placeholder="请输入拒绝原因"
      confirm-text="确认拒绝"
      btn-type="danger"
      :loading="rejectRefundLoading"
      @confirm="handleRejectRefund"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderList, approveRefund, rejectRefund } from '@/api/admin'
import { downloadCsv } from '@/utils/download'
import { useDebounceSearch } from '@/composables/useDebounceSearch'
import ReasonDialog from '@/components/ReasonDialog.vue'
import type { AdminOrderVO, PageQueryParams } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderStatusLabel, deliveryMethodLabel } from '@/utils/labels'

interface OrderItemVO {
  goodsId: number
  goodsTitle: string
  goodsImage: string
  price: number
  quantity: number
}

interface AdminOrderDetailVO extends AdminOrderVO {
  items?: OrderItemVO[]
}

const route = useRoute()
const orders = ref<AdminOrderVO[]>([])
const searchOrderNo = ref('')
const statusFilter = ref('')
const dateRange = ref<string[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const detailOrder = ref<AdminOrderDetailVO | null>(null)
const rejectRefundDialogVisible = ref(false)
const rejectRefundOrderId = ref<number>(0)

const rejectRefundLoading = ref(false)

const statusTagMap: Record<string, string> = {
  PENDING_PAY: 'warning', PAID: 'primary', SHIPPING: '',
  PENDING_REVIEW: 'warning', FINISHED: 'success', CANCELLED: 'info', REFUND: 'danger'
}
const statusLabel = (status: string) => orderStatusLabel(status)

const loadData = async () => {
  loading.value = true
  try {
    const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchOrderNo.value) params.orderNo = searchOrderNo.value
    if (statusFilter.value) params.status = statusFilter.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getOrderList(params)
    orders.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

useDebounceSearch(searchOrderNo, handleSearch)

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}

onMounted(() => {
  const queryStatus = route.query.status as string
  if (queryStatus) statusFilter.value = queryStatus
  loadData()
})

const handleApproveRefund = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认同意退款？', '同意退款')
  } catch { return }
  try {
    await approveRefund(id)
    ElMessage.success('已同意退款')
    loadData()
  } catch (e) { console.error(e) }
}

const openRejectRefundDialog = (id: number) => {
  rejectRefundOrderId.value = id
  rejectRefundDialogVisible.value = true
}

const handleRejectRefund = async (reason: string) => {
  rejectRefundLoading.value = true
  try {
    await rejectRefund(rejectRefundOrderId.value, reason)
    ElMessage.success('已拒绝退款')
    rejectRefundDialogVisible.value = false
    loadData()
  } finally {
    rejectRefundLoading.value = false
  }
}

const showDetail = (row: AdminOrderVO) => {
  detailOrder.value = row as AdminOrderDetailVO
  detailVisible.value = true
}

const handleExportOrders = () => downloadCsv('/admin/export/orders', 'orders.csv')
</script>

<style scoped lang="scss">
.filter-datepicker { width: 260px; }
.cancel-text { color: var(--admin-price-color); }
.order-goods-section { margin-bottom: 16px; }
.section-title { font-size: 14px; font-weight: 600; color: var(--admin-text); margin: 0 0 8px 0; }
.order-goods-list { display: flex; flex-direction: column; gap: 8px; }
.order-goods-item { display: flex; align-items: center; gap: 12px; padding: 8px 12px; background: var(--admin-bg); border-radius: 8px; }
.order-goods-image { width: 48px; height: 48px; border-radius: 6px; flex-shrink: 0; }
.order-goods-info { display: flex; flex-direction: column; gap: 2px; }
.order-goods-title { font-size: 13px; color: var(--admin-text); }
.order-goods-price { font-size: 12px; color: var(--admin-price-color); }
</style>

<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>资金流水</h3>
          <div class="admin-filter-bar">
            <el-select v-model="typeFilter" placeholder="类型筛选" clearable @change="handleSearch" class="filter-select">
              <el-option label="买家支付" value="PAY" />
              <el-option label="担保冻结" value="FREEZE" />
              <el-option label="结算给卖家" value="SETTLE" />
              <el-option label="退款" value="REFUND" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="orderId" label="订单ID" width="90" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="{ 'amount-in': row.type === 'REFUND', 'amount-out': row.type === 'PAY' }" style="font-weight:600">
              ¥{{ row.amount?.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="fundTypeTag(row.type)" size="small">{{ fundTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'" size="small">{{ fundStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="交易号" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-family:monospace;font-size:12px">{{ row.tradeNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
        <template #empty><el-empty description="暂无资金流水" :image-size="60" /></template>
      </el-table>
      <div class="pagination-wrapper">
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFundLogList, type FundLogVO } from '@/api/admin'
import type { PageQueryParams } from '@/types'

const list = ref<FundLogVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const typeFilter = ref('')

const fundTypeLabel = (type: string): string => {
  const map: Record<string, string> = { PAY: '买家支付', FREEZE: '担保冻结', SETTLE: '结算给卖家', REFUND: '退款' }
  return map[type] || type
}
const fundTypeTag = (type: string): string => {
  const map: Record<string, string> = { PAY: '', FREEZE: 'warning', SETTLE: 'success', REFUND: 'danger' }
  return map[type] || 'info'
}
const fundStatusLabel = (status: string): string => {
  const map: Record<string, string> = { SUCCESS: '成功', PENDING: '处理中', FAILED: '失败' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
    const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (typeFilter.value) params.type = typeFilter.value
    const res = await getFundLogList(params)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.amount-in { color: var(--admin-price-color, #10b981); }
.amount-out { color: var(--admin-price-color, #ef4444); }
</style>

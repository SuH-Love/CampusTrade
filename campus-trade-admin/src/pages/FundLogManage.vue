<template>
  <div class="fund-log-page">
    <el-card>
      <template #header>
        <div class="page-header">
          <h3 class="m-0">资金流水</h3>
          <div class="filter-bar">
            <el-select v-model="typeFilter" placeholder="类型筛选" clearable @change="loadData" style="width:140px">
              <el-option label="全部" value="" />
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
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="交易号" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-family:monospace;font-size:12px">{{ row.tradeNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top:16px;justify-content:flex-end" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFundLogList } from '@/api/admin'
import type { FundLogVO } from '@/api/admin'

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

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (typeFilter.value) params.type = typeFilter.value
    const res = await getFundLogList(params as Parameters<typeof getFundLogList>[0])
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.fund-log-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { display: flex; gap: 12px; }
.amount-in { color: var(--el-color-success); }
.amount-out { color: var(--el-color-danger); }
</style>
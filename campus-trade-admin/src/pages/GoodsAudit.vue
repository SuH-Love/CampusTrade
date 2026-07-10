<template>
  <div class="goods-audit-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3>商品审核</h3>
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="loadData" style="width: 160px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="审核通过" value="APPROVED" />
            <el-option label="已上架" value="ONLINE" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已下架" value="OFFLINE" />
            <el-option label="已售出" value="SOLD" />
          </el-select>
        </div>
      </template>
      <el-table :data="goodsList" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="封面" min-width="80">
          <template #default="{ row }">
            <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" style="width: 50px; height: 50px; border-radius: 8px" :preview-src-list="[row.coverImage]" />
            <span v-else style="color: #c0c4cc">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="username" label="卖家" min-width="100" />
        <el-table-column prop="categoryName" label="分类" min-width="80" />
        <el-table-column prop="price" label="价格" min-width="100">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600">¥{{ row.price }}</span>
            <span v-if="row.originalPrice" style="font-size: 12px; color: #c0c4cc; text-decoration: line-through; margin-left: 4px">¥{{ row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" min-width="60" />
        <el-table-column prop="favoriteCount" label="收藏" min-width="60" />
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'" effect="dark" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.rejectReason" style="color: #f56c6c">{{ row.rejectReason }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="150" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" v-permission="'goods:audit'" type="success" size="small" @click="handleAudit(row.id, 'APPROVED')">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" v-permission="'goods:audit'" type="danger" size="small" @click="handleReject(row.id)">拒绝</el-button>
            <el-button size="small" @click="handleViewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无商品" :image-size="60" /></template>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="detailVisible" title="商品详情" width="600px">
      <el-descriptions :column="2" border v-if="currentGoods">
        <el-descriptions-item label="标题" :span="2">{{ currentGoods.title }}</el-descriptions-item>
        <el-descriptions-item label="卖家">{{ currentGoods.username }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentGoods.categoryName }}</el-descriptions-item>
        <el-descriptions-item label="售价">¥{{ currentGoods.price }}</el-descriptions-item>
        <el-descriptions-item label="原价">¥{{ currentGoods.originalPrice }}</el-descriptions-item>
        <el-descriptions-item label="浏览">{{ currentGoods.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="收藏">{{ currentGoods.favoriteCount }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentGoods.description }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(currentGoods.status) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentGoods.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGoodsList, auditGoods } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminGoodsVO } from '@/types'
import type { PageQueryParams } from '@/types'

const goodsList = ref<AdminGoodsVO[]>([])
const statusFilter = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentGoods = ref<AdminGoodsVO | null>(null)
const loading = ref(false)

const statusTagMap: Record<string, string> = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: '', OFFLINE: 'info', SOLD: 'success' }
const statusLabel = (status: string) => {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING: '待审核', APPROVED: '审核通过', REJECTED: '已拒绝', ONLINE: '已上架', OFFLINE: '已下架', SOLD: '已售出' }
  return map[status] || status
}

const loadData = async () => {
  loading.value = true
  try {
  const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (statusFilter.value) params.status = statusFilter.value
  const res = await getGoodsList(params)
  goodsList.value = res.list || []
  total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleAudit = async (id: number, status: string) => {
  await auditGoods(id, { status })
  ElMessage.success('审核通过')
  loadData()
}

const handleReject = async (id: number) => {
  const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审核', { inputPattern: /\S+/, inputErrorMessage: '拒绝原因不能为空' })
  await auditGoods(id, { status: 'REJECTED', rejectReason: value })
  ElMessage.success('已拒绝')
  loadData()
}

const handleViewDetail = (row: AdminGoodsVO) => {
  currentGoods.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.goods-audit-page { padding: 20px; }
</style>

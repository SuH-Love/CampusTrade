<template>
  <div class="my-goods-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px">
          <h3 style="margin: 0">我的商品</h3>
          <div class="filter-bar">
            <el-button type="success" @click="$router.push('/goods/publish')">发布商品</el-button>
            <el-input v-model="searchKeyword" placeholder="搜索商品标题" clearable style="width: 200px" @keyup.enter="handleSearch" @clear="handleSearch" />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" style="width: 140px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="审核通过" value="APPROVED" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="已上架" value="ONLINE" />
              <el-option label="已下架" value="OFFLINE" />
              <el-option label="已售出" value="SOLD" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="filteredGoods" stripe v-loading="loading">
        <el-table-column label="商品" min-width="250">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 12px">
              <el-image :src="row.coverImage || '/default-cover.svg'" style="width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0" fit="cover" />
              <div>
                <div style="font-weight: 500">{{ row.title }}</div>
                <div style="color: #999; font-size: 12px">{{ row.categoryName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            <div v-if="statusTip(row.status)" style="color: var(--text-muted); font-size: 12px; margin-top: 4px">{{ statusTip(row.status) }}</div>
            <div v-if="row.rejectReason" style="color: #f56c6c; font-size: 12px; margin-top: 4px">原因：{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" />
        <el-table-column prop="favoriteCount" label="收藏" width="70" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/goods/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" size="small" @click="$router.push(`/goods/edit/${row.id}`)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" type="warning" size="small" @click="handleSubmitAudit(row.id)" :loading="actionLoading === row.id">提交审核</el-button>
            <el-button v-if="row.status === 'APPROVED' || row.status === 'OFFLINE'" type="success" size="small" @click="handleOnline(row.id)" :loading="actionLoading === row.id">上架</el-button>
            <el-button v-if="row.status === 'ONLINE'" type="info" size="small" @click="handleOffline(row.id)" :loading="actionLoading === row.id">下架</el-button>
            <el-button v-if="row.status !== 'ONLINE'" type="danger" size="small" @click="handleDelete(row.id)" :loading="actionLoading === row.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="filteredGoods.length === 0 && !loading" description="暂无发布的商品" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData"  />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMyGoods, submitAudit, onlineGoods, offlineGoods, deleteGoods } from '@/api/goods'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { GoodsVO } from '@/api/goods'

const goodsList = ref<GoodsVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const actionLoading = ref<number | null>(null)
const searchKeyword = ref('')
const statusFilter = ref('')

const statusLabel = (status: string) => {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING: '待审核', APPROVED: '审核通过', REJECTED: '已拒绝', ONLINE: '已上架', OFFLINE: '已下架', SOLD: '已售出' }
  return map[status] || status
}

const statusTagType = (status: string) => {
  const map: Record<string, string> = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: '', OFFLINE: 'info', SOLD: 'success' }
  return map[status] || 'info'
}

const statusTip = (status: string) => {
  const map: Record<string, string> = { APPROVED: '可点击上架', PENDING: '等待管理员审核', REJECTED: '请修改后重新提交', DRAFT: '可提交审核或继续编辑' }
  return map[status] || ''
}

const filteredGoods = computed(() => {
  let list = goodsList.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(g => g.title.toLowerCase().includes(kw))
  }
  return list
})

const loadData = async () => {
  loading.value = true
  try {
    const params: { pageNum: number; pageSize: number; status?: string } = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getMyGoods(params)
    goodsList.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleSubmitAudit = async (id: number) => {
  await ElMessageBox.confirm('确认提交审核？提交后管理员将审核您的商品', '提交审核')
  actionLoading.value = id
  try { await submitAudit(id); ElMessage.success('已提交审核'); loadData() } finally { actionLoading.value = null }
}

const handleOnline = async (id: number) => {
  actionLoading.value = id
  try { await onlineGoods(id); ElMessage.success('已上架'); loadData() } finally { actionLoading.value = null }
}

const handleOffline = async (id: number) => {
  await ElMessageBox.confirm('确认下架该商品？', '下架确认')
  actionLoading.value = id
  try { await offlineGoods(id); ElMessage.success('已下架'); loadData() } finally { actionLoading.value = null }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除该商品？删除后不可恢复', '删除确认')
  actionLoading.value = id
  try { await deleteGoods(id); ElMessage.success('已删除'); loadData() } finally { actionLoading.value = null }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.my-goods-page { padding: 20px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
</style>

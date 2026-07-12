<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>商品审核</h3>
          <div class="admin-filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索标题/卖家" clearable class="filter-input" @keyup.enter="handleSearch" @clear="handleSearch">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="categoryFilter" placeholder="分类筛选" clearable @change="handleSearch" class="filter-select">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
            </el-select>
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="filter-select">
              <el-option label="待审核" value="PENDING" />
              <el-option label="审核通过" value="APPROVED" />
              <el-option label="已上架" value="ONLINE" />
              <el-option label="已拒绝" value="REJECTED" />
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已下架" value="OFFLINE" />
              <el-option label="已售出" value="SOLD" />
            </el-select>
          </div>
        </div>
      </template>
      <el-table :data="goodsList" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="封面" min-width="80">
          <template #default="{ row }">
            <el-image v-if="row.coverImage" :src="row.coverImage" fit="cover" class="cover-image" :preview-src-list="[row.coverImage]" />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="username" label="卖家" min-width="100" />
        <el-table-column prop="categoryName" label="分类" min-width="80" />
        <el-table-column prop="price" label="价格" min-width="100">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price }}</span>
            <span v-if="row.originalPrice" class="original-price">¥{{ row.originalPrice }}</span>
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
            <span v-if="row.rejectReason" class="reject-text">{{ row.rejectReason }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" min-width="150" />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" v-permission="'goods:audit'" type="success" size="small" @click="handleAudit(row.id, 'APPROVED')">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" v-permission="'goods:audit'" type="danger" size="small" @click="openRejectDialog(row.id)">拒绝</el-button>
            <el-button size="small" @click="handleViewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无商品" :image-size="60" /></template>
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

    <el-dialog v-model="detailVisible" title="商品详情" width="680px">
      <template v-if="currentGoods">
        <div v-if="goodsImages.length" class="detail-images">
          <el-image
            v-for="(img, idx) in goodsImages"
            :key="idx"
            :src="img"
            fit="cover"
            class="detail-image-item"
            :preview-src-list="goodsImages"
            :initial-index="idx"
          />
        </div>
        <el-descriptions :column="2" border class="detail-descriptions">
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
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝审核" width="460px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="拒绝原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入拒绝原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="rejectLoading">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getGoodsList, auditGoods, getCategoryList } from '@/api/admin'
import type { CategoryVO } from '@/api/admin'
import { ElMessage } from 'element-plus'
import type { AdminGoodsVO, PageQueryParams } from '@/types'
import { goodsStatusLabel } from '@/utils/labels'

const goodsList = ref<AdminGoodsVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref<number | ''>('')
const categories = ref<CategoryVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentGoods = ref<AdminGoodsVO | null>(null)
const loading = ref(false)
const rejectDialogVisible = ref(false)
const rejectGoodsId = ref<number>(0)
const rejectReason = ref('')
const rejectLoading = ref(false)

const statusTagMap: Record<string, string> = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: '', OFFLINE: 'info', SOLD: 'success' }
const statusLabel = (status: string) => goodsStatusLabel(status)

const goodsImages = computed(() => {
  if (!currentGoods.value) return []
  const imgs: string[] = []
  if (currentGoods.value.coverImage) imgs.push(currentGoods.value.coverImage)
  if (currentGoods.value.images) {
    currentGoods.value.images.split(',').filter(Boolean).forEach(img => {
      if (!imgs.includes(img)) imgs.push(img)
    })
  }
  return imgs
})

const loadData = async () => {
  loading.value = true
  try {
    const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value) params.status = statusFilter.value
    if (categoryFilter.value) params.categoryId = categoryFilter.value
    const res = await getGoodsList(params)
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

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}

const handleAudit = async (id: number, status: string) => {
  await auditGoods(id, { status })
  ElMessage.success('审核通过')
  loadData()
}

const openRejectDialog = (id: number) => {
  rejectGoodsId.value = id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const handleReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  rejectLoading.value = true
  try {
    await auditGoods(rejectGoodsId.value, { status: 'REJECTED', rejectReason: rejectReason.value })
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadData()
  } finally {
    rejectLoading.value = false
  }
}

const handleViewDetail = (row: AdminGoodsVO) => {
  currentGoods.value = row
  detailVisible.value = true
}

const loadCategories = async () => {
  try {
    categories.value = await getCategoryList()
  } catch { /* ignore */ }
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped lang="scss">
.filter-input { width: 200px; }
.filter-select { width: 140px; }
.text-muted { color: var(--admin-text-secondary); }
.price-text { color: #f56c6c; font-weight: 600; }
.original-price { font-size: 12px; color: var(--admin-text-secondary); text-decoration: line-through; margin-left: 4px; }
.reject-text { color: #f56c6c; }
.cover-image { width: 50px; height: 50px; border-radius: 8px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
.detail-images { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.detail-image-item { width: 100px; height: 100px; border-radius: 8px; border: 1px solid var(--admin-border); }
.detail-descriptions { margin-top: 8px; }
</style>

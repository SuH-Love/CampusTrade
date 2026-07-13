<template>
  <div class="my-goods-page page-bg">
    <div class="my-goods-inner">
      <div class="my-goods-header">
        <h3 class="my-goods-title">我的商品</h3>
        <div class="filter-bar">
          <el-button type="success" @click="$router.push('/goods/publish')">发布商品</el-button>
          <el-input v-model="searchKeyword" placeholder="搜索商品标题" clearable class="search-input" @keyup.enter="handleSearch" @clear="handleSearch" />
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="status-select">
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

      <el-table v-if="!isMobile" :data="filteredGoods" stripe v-loading="loading" class="goods-table">
        <el-table-column label="商品" min-width="250">
          <template #default="{ row }">
            <div class="goods-cell">
              <el-image :src="row.coverImage || '/default-cover.svg'" class="goods-thumb" fit="cover" />
              <div>
                <div class="goods-cell-title">{{ row.title }}</div>
                <div class="goods-cell-cat">{{ row.categoryName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" min-width="110">
          <template #default="{ row }">
            <div>
              <span class="price-text">¥{{ row.price }}</span>
              <span v-if="row.originalPrice && row.originalPrice > row.price" class="original-price">¥{{ row.originalPrice }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="condition" label="成色" min-width="80">
          <template #default="{ row }">
            <el-tag v-if="row.condition" size="small" type="warning">{{ row.condition }}</el-tag>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" min-width="60">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.stock <= 3 }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="goodsStatusTagType(row.status)">{{ goodsStatusLabel(row.status) }}</el-tag>
            <div v-if="statusTip(row.status)" class="status-tip">{{ statusTip(row.status) }}</div>
            <div v-if="row.rejectReason" class="reject-reason">原因：{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" min-width="60" />
        <el-table-column prop="favoriteCount" label="收藏" min-width="60" />
        <el-table-column prop="createTime" label="发布时间" min-width="150" />
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/goods/${row.id}`)">查看</el-button>
            <el-button v-if="row.status !== 'ONLINE' && row.status !== 'SOLD'" size="small" type="primary" @click="$router.push(`/goods/edit/${row.id}`)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" type="warning" size="small" @click="handleSubmitAudit(row.id)" :loading="actionLoading === row.id">提交审核</el-button>
            <el-button v-if="row.status === 'APPROVED' || row.status === 'OFFLINE'" type="success" size="small" @click="handleOnline(row.id)" :loading="actionLoading === row.id">上架</el-button>
            <el-button v-if="row.status === 'ONLINE'" type="info" size="small" @click="handleOffline(row.id)" :loading="actionLoading === row.id">下架</el-button>
            <el-button v-if="row.status !== 'ONLINE'" type="danger" size="small" @click="handleDelete(row.id)" :loading="actionLoading === row.id">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isMobile" class="goods-cards" v-loading="loading">
        <div v-for="row in filteredGoods" :key="row.id" class="goods-card">
          <div class="goods-card-header">
            <div class="goods-card-info">
              <el-image :src="row.coverImage || '/default-cover.svg'" class="goods-card-img" fit="cover" />
              <div class="goods-card-text">
                <div class="goods-card-title">{{ row.title }}</div>
                <div class="goods-card-cat">{{ row.categoryName }}</div>
              </div>
            </div>
            <el-tag :type="goodsStatusTagType(row.status)" size="small">{{ goodsStatusLabel(row.status) }}</el-tag>
          </div>
          <div class="goods-card-body">
            <div class="goods-card-meta">
              <span class="price-text">¥{{ row.price }}</span>
              <span v-if="row.originalPrice && row.originalPrice > row.price" class="original-price">¥{{ row.originalPrice }}</span>
              <el-tag v-if="row.condition" size="small" type="warning">{{ row.condition }}</el-tag>
            </div>
            <div class="goods-card-stats">
              <span>库存: <span :class="{ 'low-stock': row.stock <= 3 }">{{ row.stock }}</span></span>
              <span>浏览: {{ row.viewCount }}</span>
              <span>收藏: {{ row.favoriteCount }}</span>
            </div>
            <div v-if="statusTip(row.status)" class="status-tip">{{ statusTip(row.status) }}</div>
            <div v-if="row.rejectReason" class="reject-reason">原因：{{ row.rejectReason }}</div>
          </div>
          <div class="goods-card-actions">
            <el-button size="small" @click="$router.push(`/goods/${row.id}`)">查看</el-button>
            <el-button v-if="row.status !== 'ONLINE' && row.status !== 'SOLD'" size="small" type="primary" @click="$router.push(`/goods/edit/${row.id}`)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" type="warning" size="small" @click="handleSubmitAudit(row.id)" :loading="actionLoading === row.id">提交审核</el-button>
            <el-button v-if="row.status === 'APPROVED' || row.status === 'OFFLINE'" type="success" size="small" @click="handleOnline(row.id)" :loading="actionLoading === row.id">上架</el-button>
            <el-button v-if="row.status === 'ONLINE'" type="info" size="small" @click="handleOffline(row.id)" :loading="actionLoading === row.id">下架</el-button>
            <el-button v-if="row.status !== 'ONLINE'" type="danger" size="small" @click="handleDelete(row.id)" :loading="actionLoading === row.id">删除</el-button>
          </div>
        </div>
      </div>

      <EmptyState v-if="filteredGoods.length === 0 && !loading" icon="🏪" title="暂无发布的商品" description="发布你的闲置物品，让它们找到新主人" action-text="发布商品" @action="$router.push('/goods/publish')" />
      <el-pagination v-if="total > 0" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" class="list-pagination" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMyGoods, submitAudit, onlineGoods, offlineGoods, deleteGoods } from '@/api/goods'
import EmptyState from '@/components/EmptyState.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { GoodsVO } from '@/api/goods'
import { goodsStatusLabel, goodsStatusTagType } from '@/utils/labels'

const route = useRoute()

const goodsList = ref<GoodsVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const actionLoading = ref<number | null>(null)
const searchKeyword = ref('')
const statusFilter = ref((route.query.status as string) || '')
const isMobile = ref(window.innerWidth < 768)

const handleResize = () => {
  isMobile.value = window.innerWidth < 768
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

onMounted(() => { loadData(); window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })
</script>

<style scoped lang="scss">
.my-goods-page { padding: 20px; }
.my-goods-inner {
  background: var(--bg-glass);

  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
}
.my-goods-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
.my-goods-title { margin: 0; }
.filter-bar { display: flex; gap: 12px; align-items: center; }
.search-input { width: 200px; }
.status-select { width: 140px; }
.goods-table { width: 100%; }
.goods-cell { display: flex; align-items: center; gap: 12px; }
.goods-thumb { width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0; }
.goods-cell-title { font-weight: 500; }
.goods-cell-cat { color: var(--text-muted); font-size: 12px; }
.price-text { color: var(--danger); font-weight: bold; }
.original-price { color: var(--text-muted); font-size: 12px; text-decoration: line-through; margin-left: 6px; }
.low-stock { color: var(--danger); font-weight: bold; }
.text-placeholder { color: var(--text-muted); }
.status-tip { color: var(--text-muted); font-size: 12px; margin-top: 4px; }
.reject-reason { color: var(--danger); font-size: 12px; margin-top: 4px; }
.list-pagination { margin-top: 20px; justify-content: center; }

.goods-cards { display: flex; flex-direction: column; gap: 12px; }
.goods-card {
  background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border);
  padding: var(--spacing-md); transition: var(--transition);
  &:hover { border-color: var(--primary-light); box-shadow: var(--shadow-sm); }
}
.goods-card-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.goods-card-info { display: flex; align-items: center; gap: 10px; flex: 1; min-width: 0; }
.goods-card-img { width: 56px; height: 56px; border-radius: var(--radius-sm); flex-shrink: 0; }
.goods-card-text { flex: 1; min-width: 0; }
.goods-card-title { font-weight: 600; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.goods-card-cat { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
.goods-card-body { margin-top: 10px; display: flex; flex-direction: column; gap: 6px; }
.goods-card-meta { display: flex; align-items: center; gap: 8px; }
.goods-card-stats { display: flex; gap: 12px; font-size: 13px; color: var(--text-secondary); }
.goods-card-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; padding-top: 10px; border-top: 1px solid var(--border); }

@media (max-width: 576px) {
  .my-goods-page { padding: 12px; }
  .my-goods-inner { padding: 16px; }
  .my-goods-header { flex-direction: column; align-items: flex-start; }
  .filter-bar { width: 100%; flex-wrap: wrap; }
  .search-input { width: 100%; }
  .status-select { width: 100%; }
}
</style>

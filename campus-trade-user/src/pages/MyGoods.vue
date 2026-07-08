<template>
  <div class="my-goods-page">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">我的商品</h3>
          <el-button type="success" @click="$router.push('/goods/publish')">发布商品</el-button>
        </div>
      </template>
      <el-table :data="goodsList" stripe>
        <el-table-column label="商品" min-width="250">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 12px">
              <el-image :src="row.coverImage || '/placeholder.png'" style="width: 60px; height: 60px; border-radius: 4px; flex-shrink: 0" fit="cover" />
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
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" />
        <el-table-column prop="favoriteCount" label="收藏" width="70" />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/goods/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'DRAFT' || row.status === 'REJECTED'" type="warning" size="small" @click="handleSubmitAudit(row.id)">提交审核</el-button>
            <el-button v-if="row.status === 'APPROVED' || row.status === 'OFFLINE'" type="success" size="small" @click="handleOnline(row.id)">上架</el-button>
            <el-button v-if="row.status === 'ONLINE'" type="info" size="small" @click="handleOffline(row.id)">下架</el-button>
            <el-button v-if="row.status !== 'ONLINE'" type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="goodsList.length === 0" description="暂无发布的商品" />
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGoodsList, submitAudit, onlineGoods, offlineGoods, deleteGoods } from '@/api/goods'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { GoodsVO } from '@/api/goods'

const userStore = useUserStore()
const goodsList = ref<GoodsVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statusLabel = (status: string) => {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING: '待审核', APPROVED: '审核通过', REJECTED: '已拒绝', ONLINE: '已上架', OFFLINE: '已下架', SOLD: '已售出' }
  return map[status] || status
}

const statusTagType = (status: string) => {
  const map: Record<string, string> = { DRAFT: 'info', PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', ONLINE: '', OFFLINE: 'info', SOLD: 'success' }
  return map[status] || ''
}

const loadData = async () => {
  const res = await getGoodsList({ pageNum: pageNum.value, pageSize: pageSize.value, userId: userStore.userInfo?.id })
  goodsList.value = res.list || []
  total.value = res.total || 0
}

const handleSubmitAudit = async (id: number) => {
  await ElMessageBox.confirm('确认提交审核？提交后管理员将审核您的商品', '提交审核')
  await submitAudit(id)
  ElMessage.success('已提交审核')
  loadData()
}

const handleOnline = async (id: number) => {
  await onlineGoods(id)
  ElMessage.success('已上架')
  loadData()
}

const handleOffline = async (id: number) => {
  await ElMessageBox.confirm('确认下架该商品？', '下架确认')
  await offlineGoods(id)
  ElMessage.success('已下架')
  loadData()
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除该商品？删除后不可恢复', '删除确认')
  await deleteGoods(id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.my-goods-page { padding: 20px; }
</style>
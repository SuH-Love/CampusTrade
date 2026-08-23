<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>用户管理</h3>
          <div class="admin-filter-bar">
            <el-input v-model="searchKeyword" placeholder="搜索用户名/手机号" clearable class="filter-input" @clear="handleSearch">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="handleSearch" class="filter-select">
              <el-option label="正常" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button @click="handleExportUsers">导出CSV</el-button>
          </div>
        </div>
      </template>
      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column label="头像" min-width="70">
          <template #default="{ row }">
            <el-avatar :size="36" :src="row.avatar || '/default-avatar.svg'" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120">
          <template #default="{ row }">
            <span v-if="row.phone">{{ row.phone }}</span>
            <span v-else class="text-muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.email">{{ row.email }}</span>
            <span v-else class="text-muted">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="90">
          <template #default="{ row }">
            <span v-if="row.realName">{{ row.realName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" min-width="100">
          <template #default="{ row }">
            <span v-if="row.studentId">{{ row.studentId }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="realVerified" label="认证" min-width="70">
          <template #default="{ row }">
            <el-tag :type="row.realVerified === 1 ? 'success' : 'info'" size="small" effect="dark" round>{{ row.realVerified === 1 ? '已认证' : '未认证' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark" round>{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="150" />
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 1" v-permission="'user:manage'" type="danger" size="small" @click="openBanDialog(row.id)">封禁</el-button>
            <el-button v-else v-permission="'user:manage'" type="success" size="small" @click="handleUnban(row.id)">解封</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" :image-size="60" /></template>
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

    <el-dialog v-model="detailVisible" title="用户详情" width="600px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email || '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUser.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ currentUser.studentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认证">
          <el-tag :type="currentUser.realVerified === 1 ? 'success' : 'info'" size="small">{{ currentUser.realVerified === 1 ? '已认证' : '未认证' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentUser.status === 1 ? 'success' : 'danger'" size="small">{{ currentUser.status === 1 ? '正常' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ currentUser.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <ReasonDialog
      v-model="banDialogVisible"
      title="封禁用户"
      label="封禁原因"
      placeholder="请输入封禁原因"
      confirm-text="确认封禁"
      btn-type="danger"
      :loading="banLoading"
      @confirm="handleBan"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, banUser, unbanUser } from '@/api/admin'
import { downloadCsv } from '@/utils/download'
import { useDebounceSearch } from '@/composables/useDebounceSearch'
import ReasonDialog from '@/components/ReasonDialog.vue'
import { ElMessage } from 'element-plus'
import type { AdminUserVO, PageQueryParams } from '@/types'

const users = ref<AdminUserVO[]>([])
const searchKeyword = ref('')
const statusFilter = ref<number | ''>('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const currentUser = ref<AdminUserVO | null>(null)
const banDialogVisible = ref(false)
const banUserId = ref<number>(0)

const banLoading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value !== '') params.status = statusFilter.value
    const res = await getUserList(params)
    users.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

useDebounceSearch(searchKeyword, handleSearch)

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}

const handleViewDetail = (row: AdminUserVO) => {
  currentUser.value = row
  detailVisible.value = true
}

const openBanDialog = (id: number) => {
  banUserId.value = id
  banDialogVisible.value = true
}

const handleBan = async (reason: string) => {
  banLoading.value = true
  try {
    await banUser(banUserId.value, reason)
    ElMessage.success('已封禁')
    banDialogVisible.value = false
    loadData()
  } finally {
    banLoading.value = false
  }
}

const handleUnban = async (id: number) => {
  await unbanUser(id)
  ElMessage.success('已解封')
  loadData()
}

onMounted(loadData)

const handleExportUsers = () => downloadCsv('/admin/export/users', 'users.csv')
</script>

<style scoped lang="scss">

</style>

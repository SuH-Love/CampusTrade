<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">用户管理</h3>
          <div style="display: flex; gap: 8px; align-items: center">
            <el-input v-model="searchUsername" placeholder="搜索用户名" clearable style="width: 200px" @keyup.enter="handleSearch" />
            <el-button @click="handleExportUsers">导出CSV</el-button>
          </div>
        </div>
      </template>
      <el-table :data="users" style="width: 100%" stripe v-loading="loading">
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
            <span v-else style="color: #c0c4cc">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.email">{{ row.email }}</span>
            <span v-else style="color: #c0c4cc">未绑定</span>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" min-width="90">
          <template #default="{ row }">
            <span v-if="row.realName">{{ row.realName }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="studentId" label="学号" min-width="100">
          <template #default="{ row }">
            <span v-if="row.studentId">{{ row.studentId }}</span>
            <span v-else style="color: #c0c4cc">-</span>
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
            <el-button v-if="row.status === 1" v-permission="'user:ban'" type="danger" size="small" @click="handleBan(row.id)">封禁</el-button>
            <el-button v-else v-permission="'user:ban'" type="success" size="small" @click="handleUnban(row.id)">解封</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" :image-size="60" /></template>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, banUser, unbanUser } from '@/api/admin'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminUserVO, PageQueryParams } from '@/types'

const users = ref<AdminUserVO[]>([])
const searchUsername = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const currentUser = ref<AdminUserVO | null>(null)

const loadData = async () => {
  loading.value = true
  try {
  const params: PageQueryParams = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (searchUsername.value) params.username = searchUsername.value
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

const handleViewDetail = (row: AdminUserVO) => {
  currentUser.value = row
  detailVisible.value = true
}

const handleBan = async (id: number) => {
  await ElMessageBox.confirm('确认封禁该用户？', '确认')
  await banUser(id)
  ElMessage.success('已封禁')
  loadData()
}

const handleUnban = async (id: number) => {
  await unbanUser(id)
  ElMessage.success('已解封')
  loadData()
}

onMounted(loadData)

const handleExportUsers = async () => {
  try {
    const res = await request.get('/admin/export/users', { responseType: 'blob' }) as unknown as Blob
    const url = window.URL.createObjectURL(new Blob([res], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a')
    link.href = url
    link.download = 'users.csv'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) { console.error(e) }
}
</script>

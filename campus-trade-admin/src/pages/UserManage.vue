<template>
  <div>
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">用户管理</h3>
          <el-input v-model="searchUsername" placeholder="搜索用户名" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </div>
      </template>
      <el-table :data="users" style="width: 100%" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
        <el-table-column prop="realVerified" label="认证" min-width="70">
          <template #default="{ row }">
            <el-tag :type="row.realVerified === 1 ? 'success' : 'info'" size="small">{{ row.realVerified === 1 ? '已认证' : '未认证' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="150" />
        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" v-permission="'user:ban'" type="danger" size="small" @click="handleBan(row.id)">封禁</el-button>
            <el-button v-else v-permission="'user:ban'" type="success" size="small" @click="handleUnban(row.id)">解封</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" :image-size="60" /></template>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, banUser, unbanUser } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminUserVO, PageQueryParams } from '@/types'

const users = ref<AdminUserVO[]>([])
const searchUsername = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

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
</script>

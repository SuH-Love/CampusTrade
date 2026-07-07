<template>
  <div>
    <el-card>
      <h3>用户管理</h3>
      <el-table :data="users" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="danger" size="small" @click="handleBan(row.id)">封禁</el-button>
            <el-button v-else type="success" size="small" @click="handleUnban(row.id)">解封</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, banUser, unbanUser } from '@/api/admin'
import { ElMessage } from 'element-plus'

const users = ref<any[]>([])

const loadData = async () => {
  const res: any = await getUserList({ pageNum: 1, pageSize: 50 })
  users.value = res.list || []
}

const handleBan = async (id: number) => { await banUser(id); ElMessage.success('已封禁'); loadData() }
const handleUnban = async (id: number) => { await unbanUser(id); ElMessage.success('已解封'); loadData() }

onMounted(loadData)
</script>
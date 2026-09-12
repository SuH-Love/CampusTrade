<template>
  <div class="admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>平台知识管理</span>
          <el-button type="primary" size="small" @click="openCreate">新增知识块</el-button>
        </div>
      </template>
      <el-table :data="list" stripe v-loading="loading" size="default">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" width="150" />
        <el-table-column prop="keywords" label="关键词" show-overflow-tooltip min-width="200" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip min-width="300" />
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link @click="openEdit(row)">编辑</el-button>
            <el-button size="small" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="editing ? '编辑知识块' : '新增知识块'" width="700px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="如：密码与账号" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="空格分隔，如：密码 注册 登录 重置" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="知识块内容，支持Markdown格式" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAiKnowledge, createAiKnowledge, updateAiKnowledge, deleteAiKnowledge } from '@/api/admin'

const list = ref<Record<string, any>[]>([])
const loading = ref(false)
const formVisible = ref(false)
const editing = ref(false)
const form = ref<Record<string, any>>({ title: '', keywords: '', content: '', enabled: 1, sortOrder: 0 })

const loadData = async () => {
  loading.value = true
  try {
    list.value = await getAiKnowledge()
  } catch { /* ignore */ } finally { loading.value = false }
}

const openCreate = () => {
  editing.value = false
  form.value = { title: '', keywords: '', content: '', enabled: 1, sortOrder: 0 }
  formVisible.value = true
}

const openEdit = (row: Record<string, any>) => {
  editing.value = true
  form.value = { ...row }
  formVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  try {
    if (editing.value) {
      await updateAiKnowledge(form.value)
    } else {
      await createAiKnowledge(form.value)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadData()
  } catch { ElMessage.error('保存失败') }
}

const handleDelete = (row: Record<string, any>) => {
  ElMessageBox.confirm(`确定删除"${row.title}"？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteAiKnowledge(row.id)
        ElMessage.success('删除成功')
        loadData()
      } catch { ElMessage.error('删除失败') }
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
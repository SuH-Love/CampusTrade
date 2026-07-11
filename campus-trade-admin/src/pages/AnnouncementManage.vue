<template>
  <div class="announcement-manage">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">公告管理</h3>
          <el-button type="primary" @click="handleAdd">发布公告</el-button>
        </div>
      </template>
      <el-table :data="announcements" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" min-width="70" />
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggle(row)">{{ row.status === 1 ? '隐藏' : '显示' }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '发布公告'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAnnouncementList, createAnnouncement, updateAnnouncement, deleteAnnouncement, type AnnouncementVO } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const announcements = ref<AnnouncementVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = reactive({ title: '', content: '', sortOrder: 0 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAnnouncementList({ pageNum: pageNum.value, pageSize: pageSize.value })
    announcements.value = res.list || []
    total.value = res.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleAdd = () => {
  editingId.value = null
  form.title = ''; form.content = ''; form.sortOrder = 0
  dialogVisible.value = true
}

const handleEdit = (row: AnnouncementVO) => {
  editingId.value = row.id
  form.title = row.title; form.content = row.content; form.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) { ElMessage.error('标题和内容不能为空'); return }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, { title: form.title, content: form.content, sortOrder: form.sortOrder })
      ElMessage.success('修改成功')
    } else {
      await createAnnouncement({ title: form.title, content: form.content, sortOrder: form.sortOrder })
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { console.error(e) } finally { submitting.value = false }
}

const handleToggle = async (row: AnnouncementVO) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await updateAnnouncement(row.id, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已显示' : '已隐藏')
    loadData()
  } catch (e) { console.error(e) }
}

const handleDelete = async (row: AnnouncementVO) => {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」？`, '提示', { type: 'warning' })
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.announcement-manage { padding: 20px; }
</style>
<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>公告管理</h3>
          <el-button type="primary" @click="handleAdd">发布公告</el-button>
        </div>
      </template>
      <el-table :data="announcements" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" min-width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type] || 'info'">{{ typeLabelMap[row.type] || '未知' }}</el-tag>
          </template>
        </el-table-column>
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
        <template #empty><el-empty description="暂无公告" /></template>
      </el-table>
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '发布公告'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择公告类型">
            <el-option label="系统通知" :value="1" />
            <el-option label="活动公告" :value="2" />
            <el-option label="维护通知" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="显示" inactive-text="隐藏" />
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

const typeLabelMap: Record<number, string> = { 1: '系统通知', 2: '活动公告', 3: '维护通知' }
const typeTagMap: Record<number, string> = { 1: 'primary', 2: 'success', 3: 'warning' }

const announcements = ref<AnnouncementVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = reactive({ title: '', content: '', sortOrder: 0, type: 1, status: 1 })

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
  form.title = ''; form.content = ''; form.sortOrder = 0; form.type = 1; form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: AnnouncementVO) => {
  editingId.value = row.id
  form.title = row.title; form.content = row.content; form.sortOrder = row.sortOrder || 0; form.type = row.type ?? 1; form.status = row.status ?? 1
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) { ElMessage.error('标题和内容不能为空'); return }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateAnnouncement(editingId.value, { title: form.title, content: form.content, sortOrder: form.sortOrder, type: form.type, status: form.status })
      ElMessage.success('修改成功')
    } else {
      await createAnnouncement({ title: form.title, content: form.content, sortOrder: form.sortOrder, type: form.type, status: form.status })
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { console.error(e) } finally { submitting.value = false }
}

const handleToggle = async (row: AnnouncementVO) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      newStatus === 1 ? `确认显示公告「${row.title}」？` : `确认隐藏公告「${row.title}」？`,
      '操作确认',
      { type: 'warning' }
    )
    await updateAnnouncement(row.id, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已显示' : '已隐藏')
    loadData()
  } catch { /* cancel */ }
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

const handleSizeChange = () => {
  pageNum.value = 1
  loadData()
}
</script>



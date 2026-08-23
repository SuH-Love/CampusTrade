<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>分类管理</h3>
          <div class="header-actions">
            <el-input v-model="searchKeyword" placeholder="搜索分类名称" clearable class="filter-input">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="handleAdd">新增分类</el-button>
          </div>
        </div>
      </template>
      <el-table :data="filteredCategories" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="60" />
        <el-table-column prop="categoryName" label="分类名称" min-width="150" />
        <el-table-column prop="sortOrder" label="排序" min-width="80" />
        <el-table-column prop="icon" label="图标" min-width="100" />
        <el-table-column prop="status" label="状态" min-width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无分类" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="440px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标名称（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { getCategoryList, createCategory, updateCategory, deleteCategory, type CategoryVO } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const categories = ref<CategoryVO[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const searchKeyword = ref('')

const form = reactive({
  categoryName: '',
  sortOrder: 0,
  icon: '',
  status: 1
})

const filteredCategories = computed(() => {
  if (!searchKeyword.value.trim()) return categories.value
  const keyword = searchKeyword.value.trim().toLowerCase()
  return categories.value.filter(item => item.categoryName.toLowerCase().includes(keyword))
})

const loadData = async () => {
  loading.value = true
  try {
    categories.value = await getCategoryList() || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}


const handleAdd = () => {
  editingId.value = null
  form.categoryName = ''
  form.sortOrder = 0
  form.icon = ''
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: CategoryVO) => {
  editingId.value = row.id
  form.categoryName = row.categoryName
  form.sortOrder = row.sortOrder || 0
  form.icon = row.icon || ''
  form.status = row.status ?? 1
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.categoryName.trim()) { ElMessage.error('请输入分类名称'); return }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, { categoryName: form.categoryName, sortOrder: form.sortOrder, icon: form.icon, status: form.status })
      ElMessage.success('修改成功')
    } else {
      await createCategory({ categoryName: form.categoryName, sortOrder: form.sortOrder, icon: form.icon, status: form.status })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { console.error(e) } finally { submitting.value = false }
}

const handleToggleStatus = async (row: CategoryVO) => {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      newStatus === 1 ? `确认启用分类「${row.categoryName}」？` : `确认禁用分类「${row.categoryName}」？`,
      '操作确认',
      { type: 'warning' }
    )
    await updateCategory(row.id, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
    loadData()
  } catch { /* cancel */ }
}

const handleDelete = async (row: CategoryVO) => {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.categoryName}」？`, '提示', { type: 'warning' })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>



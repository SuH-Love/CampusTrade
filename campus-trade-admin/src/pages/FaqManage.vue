<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>AI知识库管理</h3>
          <div class="header-actions">
            <el-input v-model="searchKeyword" placeholder="搜索问题" clearable class="filter-input">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="filterCategory" placeholder="全部分类" clearable class="filter-select">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
            <el-button type="primary" @click="handleAdd">新增FAQ</el-button>
          </div>
        </div>
      </template>
      <el-table :data="pagedFaqs" stripe v-loading="loading">
        <el-table-column type="index" label="#" min-width="50" :index="indexMethod" />
        <el-table-column prop="category" label="分类" min-width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" min-width="150" fixed="right">
          <template #default="{ row, $index }">
            <el-button size="small" @click="handleEdit(row, $index)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete($index)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无FAQ" /></template>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="filteredFaqs.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingIndex >= 0 ? '编辑FAQ' : '新增FAQ'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类" allow-create filterable>
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题" required>
          <el-input v-model="form.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="form.answer" type="textarea" :rows="5" placeholder="请输入答案" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getFaqList, addFaq, updateFaq, deleteFaq, type FaqItem } from '@/api/ai'

const loading = ref(false)
const submitting = ref(false)
const faqList = ref<FaqItem[]>([])
const searchKeyword = ref('')
const filterCategory = ref('')
const dialogVisible = ref(false)
const editingIndex = ref(-1)
const form = ref<FaqItem>({ question: '', answer: '', category: '交易' })
const currentPage = ref(1)
const pageSize = ref(10)

const categories = computed(() => {
  const set = new Set<string>()
  faqList.value.forEach(f => { if (f.category) set.add(f.category) })
  return Array.from(set)
})

const filteredFaqs = computed(() => {
  return faqList.value.filter(f => {
    if (filterCategory.value && f.category !== filterCategory.value) return false
    if (searchKeyword.value && !f.question.includes(searchKeyword.value) && !f.answer.includes(searchKeyword.value)) return false
    return true
  })
})

const pagedFaqs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredFaqs.value.slice(start, start + pageSize.value)
})

const indexMethod = (index: number) => (currentPage.value - 1) * pageSize.value + index + 1

watch([searchKeyword, filterCategory], () => { currentPage.value = 1 })

const fetchFaqs = async () => {
  loading.value = true
  try {
    faqList.value = await getFaqList()
  } catch {
    ElMessage.error('获取FAQ列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingIndex.value = -1
  form.value = { question: '', answer: '', category: '交易' }
  dialogVisible.value = true
}

const handleEdit = (row: FaqItem, index: number) => {
  editingIndex.value = (currentPage.value - 1) * pageSize.value + index
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (index: number) => {
  const globalIndex = (currentPage.value - 1) * pageSize.value + index
  try {
    await ElMessageBox.confirm('确定删除这条FAQ吗？', '提示', { type: 'warning' })
    await deleteFaq(globalIndex)
    ElMessage.success('删除成功')
    await fetchFaqs()
    if (pagedFaqs.value.length === 0 && currentPage.value > 1) currentPage.value--
  } catch {}
}

const handleSubmit = async () => {
  if (!form.value.question.trim() || !form.value.answer.trim() || !form.value.category.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    if (editingIndex.value >= 0) {
      await updateFaq(editingIndex.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await addFaq(form.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchFaqs()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchFaqs)
</script>

<style scoped>
.filter-input { width: 200px; }
.filter-select { width: 120px; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
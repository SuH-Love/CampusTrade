<template>
  <div class="banner-manage">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 style="margin: 0">横幅管理</h3>
          <el-button type="primary" @click="handleAdd">新增横幅</el-button>
        </div>
      </template>
      <el-table :data="banners" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="预览" width="180">
          <template #default="{ row }">
            <div v-if="row.imageUrl" class="banner-preview"><img :src="row.imageUrl" /></div>
            <div v-else class="banner-preview" :style="{ background: row.bgColor }"><span class="preview-text">{{ row.title }}</span></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="subtitle" label="副标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="linkUrl" label="链接" width="140" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggle(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" style="margin-top: 16px" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑横幅' : '新增横幅'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="横幅标题" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="横幅副标题" />
        </el-form-item>
        <el-form-item label="背景图">
          <el-input v-model="form.imageUrl" placeholder="背景图片URL(可选,优先于背景色)">
            <template #append>
              <el-upload :show-file-list="false" :before-upload="handleUpload" accept="image/*"><el-button>上传</el-button></el-upload>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="背景色">
          <el-input v-model="form.bgColor" placeholder="CSS渐变或颜色值, 如 linear-gradient(135deg, #6366f1, #a78bfa)" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.linkUrl" placeholder="点击跳转路径, 如 /goods" />
        </el-form-item>
        <el-form-item label="按钮文字">
          <el-input v-model="form.buttonText" placeholder="按钮显示文字, 如 浏览商品" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadImage } from '@/utils/upload'
import request from '@/utils/request'

interface BannerVO {
  id: number
  title: string
  subtitle: string
  imageUrl: string
  linkUrl: string
  bgColor: string
  buttonText: string
  sortOrder: number
  status: number
  createTime: string
}

interface PageResult<T> { list: T[]; total: number }

const banners = ref<BannerVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ title: '', subtitle: '', imageUrl: '', linkUrl: '', bgColor: '', buttonText: '', sortOrder: 0, status: 1 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get<never, PageResult<BannerVO>>('/banner/list', { params: { pageNum: pageNum.value, pageSize: pageSize.value } })
    banners.value = res.list || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const handleAdd = () => {
  editingId.value = null
  form.value = { title: '', subtitle: '', imageUrl: '', linkUrl: '', bgColor: '', buttonText: '', sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = (row: BannerVO) => {
  editingId.value = row.id
  form.value = { title: row.title, subtitle: row.subtitle, imageUrl: row.imageUrl, linkUrl: row.linkUrl, bgColor: row.bgColor, buttonText: row.buttonText, sortOrder: row.sortOrder, status: row.status }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.title) return ElMessage.warning('请输入标题')
  submitting.value = true
  try {
    if (editingId.value) {
      await request.put(`/banner/${editingId.value}`, form.value)
      ElMessage.success('修改成功')
    } else {
      await request.post('/banner', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

const handleToggle = async (row: BannerVO) => {
  await request.put(`/banner/${row.id}/toggle`)
  ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
  loadData()
}

const handleDelete = async (row: BannerVO) => {
  await ElMessageBox.confirm('确认删除该横幅？', '删除确认')
  await request.delete(`/banner/${row.id}`)
  ElMessage.success('已删除')
  loadData()
}

const handleUpload = async (file: File) => {
  try {
    const url = await uploadImage(file)
    form.value.imageUrl = url
    ElMessage.success('上传成功')
  } catch { ElMessage.error('上传失败') }
  return false
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.banner-manage { padding: 0; }
.banner-preview {
  width: 160px; height: 60px; border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center;
  img { width: 100%; height: 100%; object-fit: cover; }
  .preview-text { color: #fff; font-size: 12px; font-weight: 600; text-shadow: 0 1px 2px rgba(0,0,0,0.3); }
}
</style>
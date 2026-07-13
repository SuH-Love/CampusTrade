<template>
  <div class="admin-page">
    <el-card>
      <template #header>
        <div class="admin-card-header">
          <h3>横幅管理</h3>
          <el-button type="primary" @click="handleAdd">新增横幅</el-button>
        </div>
      </template>
      <el-table :data="banners" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="50" />
        <el-table-column label="预览" min-width="160">
          <template #default="{ row }">
            <div v-if="row.imageUrl" class="banner-preview"><img :src="row.imageUrl" /></div>
            <div v-else class="banner-preview" :style="{ background: row.bgColor || '#6366f1' }"><span class="preview-text">{{ row.title }}</span></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="subtitle" label="副标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="linkUrl" label="链接" min-width="120" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" min-width="60" />
        <el-table-column prop="status" label="状态" min-width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="150" />
        <el-table-column label="操作" min-width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggle(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无横幅" /></template>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, prev, pager, next, sizes"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
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
          <el-upload
            class="banner-uploader"
            :show-file-list="false"
            :before-upload="handleUpload"
            accept="image/*"
          >
            <img v-if="form.imageUrl" :src="form.imageUrl" class="banner-upload-preview" />
            <el-icon v-else class="banner-upload-icon"><Plus /></el-icon>
          </el-upload>
          <el-button v-if="form.imageUrl" size="small" type="danger" class="banner-remove-btn" @click="form.imageUrl = ''">移除图片</el-button>
        </el-form-item>
        <el-form-item label="背景色">
          <div class="color-field">
            <el-color-picker v-model="solidBgColor" show-alpha @change="onSolidBgChange" />
            <el-input v-model="form.bgColor" placeholder="如 #6366f1 或 linear-gradient(135deg, #6366f1, #8b5cf6)" class="color-input" />
            <el-dropdown trigger="click" @command="onGradientBg">
              <el-button size="small">渐变预设</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="g in gradientPresets" :key="g.value" :command="g.value">
                    <span class="gradient-item"><span class="gradient-dot" :style="{ background: g.value }"></span>{{ g.label }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.linkUrl" placeholder="点击跳转路径, 如 /goods" />
        </el-form-item>
        <el-form-item label="按钮文字">
          <el-input v-model="form.buttonText" placeholder="按钮显示文字, 留空则不显示按钮" />
        </el-form-item>
        <el-form-item label="按钮颜色">
          <div class="color-field">
            <el-color-picker v-model="solidBtnColor" show-alpha @change="onSolidBtnChange" />
            <el-input v-model="form.buttonColor" placeholder="如 #ffffff 或 linear-gradient(135deg, #f59e0b, #ef4444)" class="color-input" />
            <el-dropdown trigger="click" @command="onGradientBtn">
              <el-button size="small">渐变预设</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="g in gradientPresets" :key="g.value" :command="g.value">
                    <span class="gradient-item"><span class="gradient-dot" :style="{ background: g.value }"></span>{{ g.label }}</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
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
import { Plus } from '@element-plus/icons-vue'
import { uploadImage } from '@/utils/upload'
import { getBannerList, createBanner, updateBanner, toggleBanner, deleteBanner, type BannerVO } from '@/api/admin'

const banners = ref<BannerVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const form = ref<Omit<BannerVO, 'id' | 'createTime'>>({ title: '', subtitle: '', imageUrl: '', linkUrl: '', bgColor: '', buttonText: '', buttonColor: '', sortOrder: 0, status: 1 })

const solidBgColor = ref('')
const solidBtnColor = ref('')

const gradientPresets = [
  { label: '靛蓝紫', value: 'linear-gradient(135deg, #6366f1, #8b5cf6)' },
  { label: '蓝青', value: 'linear-gradient(135deg, #3b82f6, #06b6d4)' },
  { label: '橙红', value: 'linear-gradient(135deg, #f59e0b, #ef4444)' },
  { label: '粉紫', value: 'linear-gradient(135deg, #ec4899, #8b5cf6)' },
  { label: '绿青', value: 'linear-gradient(135deg, #10b981, #06b6d4)' },
  { label: '深蓝紫', value: 'linear-gradient(135deg, #1e3a8a, #7c3aed)' },
  { label: '暖橙', value: 'linear-gradient(135deg, #f97316, #eab308)' },
  { label: '玫瑰金', value: 'linear-gradient(135deg, #f43f5e, #d97706)' },
]

const onSolidBgChange = (val: string) => { if (val) form.value.bgColor = val }
const onSolidBtnChange = (val: string) => { if (val) form.value.buttonColor = val }
const onGradientBg = (val: string) => { form.value.bgColor = val; solidBgColor.value = '' }
const onGradientBtn = (val: string) => { form.value.buttonColor = val; solidBtnColor.value = '' }

const loadData = async () => {
  loading.value = true
  try {
    const res = await getBannerList({ pageNum: pageNum.value, pageSize: pageSize.value })
    banners.value = res.list || []
    total.value = res.total || 0
  } finally { loading.value = false }
}

const handleAdd = () => {
  editingId.value = null
  form.value = { title: '', subtitle: '', imageUrl: '', linkUrl: '', bgColor: '', buttonText: '', buttonColor: '', sortOrder: 0, status: 1 }
  solidBgColor.value = ''
  solidBtnColor.value = ''
  dialogVisible.value = true
}

const handleEdit = (row: BannerVO) => {
  editingId.value = row.id
  form.value = { title: row.title, subtitle: row.subtitle, imageUrl: row.imageUrl, linkUrl: row.linkUrl, bgColor: row.bgColor, buttonText: row.buttonText, buttonColor: row.buttonColor, sortOrder: row.sortOrder, status: row.status }
  solidBgColor.value = row.bgColor && !row.bgColor.includes('gradient') ? row.bgColor : ''
  solidBtnColor.value = row.buttonColor && !row.buttonColor.includes('gradient') ? row.buttonColor : ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.title) return ElMessage.warning('请输入标题')
  submitting.value = true
  try {
    if (editingId.value) {
      await updateBanner(editingId.value, form.value)
      ElMessage.success('修改成功')
    } else {
      await createBanner(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

const handleToggle = async (row: BannerVO) => {
  try {
    await ElMessageBox.confirm(
      row.status === 1 ? '确认禁用该横幅？' : '确认启用该横幅？',
      '操作确认',
      { type: 'warning' }
    )
    await toggleBanner(row.id)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    loadData()
  } catch { /* cancel */ }
}

const handleDelete = async (row: BannerVO) => {
  try {
    await ElMessageBox.confirm('确认删除该横幅？', '删除确认', { type: 'warning' })
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancel */ }
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
.banner-preview {
  width: 160px; height: 60px; border-radius: 6px; overflow: hidden; display: flex; align-items: center; justify-content: center;
  img { width: 100%; height: 100%; object-fit: cover; }
  .preview-text { color: #fff; font-size: 12px; font-weight: 600; text-shadow: 0 1px 2px rgba(0,0,0,0.3); }
}
.banner-uploader {
  :deep(.el-upload) {
    width: 200px; height: 80px; border: 1px dashed var(--admin-border); border-radius: 8px; display: flex; align-items: center; justify-content: center; cursor: pointer; overflow: hidden; transition: var(--admin-transition);
    &:hover { border-color: var(--admin-primary); }
  }
}
.banner-upload-preview { width: 100%; height: 100%; object-fit: cover; }
.banner-upload-icon { font-size: 28px; color: var(--admin-text-secondary); }
.banner-remove-btn { margin-left: 12px; }
.color-field { display: flex; align-items: center; gap: 8px; width: 100%; }
.color-input { flex: 1; }
.gradient-item { display: flex; align-items: center; gap: 8px; }
.gradient-dot { width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0; border: 1px solid rgba(0,0,0,0.1); }
</style>

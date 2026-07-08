<template>
  <div class="report-page">
    <el-card>
      <template #header><h3>提交举报</h3></template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" style="max-width: 600px">
        <el-form-item label="举报类型" prop="targetType">
          <el-select v-model="form.targetType" placeholder="选择类型" :disabled="!!route.query.targetType" @change="handleTypeChange">
            <el-option label="商品" :value="1" />
            <el-option label="用户" :value="2" />
            <el-option label="聊天" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报目标" prop="targetId">
          <el-input v-model="form.targetId" disabled placeholder="举报目标ID" />
        </el-form-item>
        <el-form-item label="目标信息" v-if="targetInfo">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="名称">{{ targetInfo.name }}</el-descriptions-item>
            <el-descriptions-item label="详情">{{ targetInfo.detail }}</el-descriptions-item>
          </el-descriptions>
        </el-form-item>
        <el-form-item label="举报原因" prop="reason">
          <el-input v-model="form.reason" placeholder="简要描述原因" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="详细描述举报内容" />
        </el-form-item>
        <el-form-item><el-button type="danger" @click="handleSubmit" :loading="loading">提交举报</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header><h3>我的举报</h3></template>
      <el-table :data="reports" stripe>
        <el-table-column prop="targetType" label="类型" width="80">
          <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status] || 'info'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResult" label="处理结果" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-if="reports.length === 0" description="暂无举报记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { createReport, listMyReports } from '@/api/report'
import { getGoodsDetail } from '@/api/goods'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type { ReportVO } from '@/api/report'

const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const reports = ref<ReportVO[]>([])
const targetInfo = ref<{ name: string; detail: string } | null>(null)

const form = reactive({ targetType: 1, targetId: '', reason: '', description: '' })
const rules = {
  targetType: [{ required: true, message: '请选择举报类型', trigger: 'change' }],
  targetId: [{ required: true, message: '举报目标不能为空', trigger: 'change' }],
  reason: [{ required: true, message: '请输入举报原因', trigger: 'blur' }]
}

const statusTagMap: Record<string, string> = { PENDING: 'warning', FINISHED: '', RESOLVED: 'success', DISMISSED: 'info' }
const statusLabel = (s: string) => ({ PENDING: '待处理', FINISHED: '已处理', RESOLVED: '已解决', DISMISSED: '已驳回' }[s] || s)
const targetTypeLabel = (t: number) => ({ 1: '商品', 2: '用户', 3: '聊天' }[t] || '其他')

const handleTypeChange = () => {
  form.targetId = ''
  targetInfo.value = null
}

const loadTargetInfo = async () => {
  if (!form.targetId) { targetInfo.value = null; return }
  try {
    if (form.targetType === 1) {
      const goods = await getGoodsDetail(Number(form.targetId))
      targetInfo.value = { name: goods.title, detail: `¥${goods.price} · ${goods.categoryName || ''}` }
    }
  } catch { targetInfo.value = null }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  loading.value = true
  try {
    await createReport({ targetType: form.targetType, targetId: Number(form.targetId), reason: form.reason, description: form.description })
    ElMessage.success('举报已提交')
    form.reason = ''
    form.description = ''
    loadReports()
  } finally {
    loading.value = false
  }
}

const loadReports = async () => {
  try {
    const res = await listMyReports()
    reports.value = res.list || []
  } catch { /* ignore */ }
}

onMounted(async () => {
  if (route.query.targetType) {
    form.targetType = Number(route.query.targetType)
  }
  if (route.query.targetId) {
    form.targetId = String(route.query.targetId)
    await loadTargetInfo()
  }
  loadReports()
})
</script>

<style scoped lang="scss">
.report-page { padding: 20px; }
</style>

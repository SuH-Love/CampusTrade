<template>
  <div class="payment-config-page page-bg">
    <div class="payment-config-inner">
      <div class="page-header">
        <h3 class="page-title">收款管理</h3>
        <el-button type="primary" @click="showAddDialog = true">添加收款账号</el-button>
      </div>

      <EmptyState v-if="configs.length === 0 && !loading" icon="💳" title="暂无收款配置" description="添加支付宝收款账号，发布商品时自动关联" action-text="添加收款账号" @action="showAddDialog = true" />

      <TransitionGroup v-else name="list" tag="div" class="config-list" v-loading="loading">
        <div v-for="config in configs" :key="config.id" class="config-card" :class="{ 'is-default': config.isDefault === 1 }">
          <div class="config-info">
            <div class="config-type">
              <el-tag :type="config.isDefault === 1 ? 'primary' : 'info'" size="small">
                {{ config.isDefault === 1 ? '默认' : '支付宝' }}
              </el-tag>
            </div>
            <div class="config-detail">
              <div class="config-account">{{ config.alipayAccount }}</div>
              <div class="config-name">{{ config.realName }}</div>
            </div>
          </div>
          <div class="config-actions">
            <el-button v-if="config.isDefault !== 1" size="small" @click="handleSetDefault(config.id)">设为默认</el-button>
            <el-button size="small" @click="handleEdit(config)">编辑</el-button>
            <el-button size="small" type="danger" text @click="handleDelete(config.id)">删除</el-button>
          </div>
        </div>
      </TransitionGroup>

      <el-dialog v-model="showAddDialog" :title="editingConfig ? '编辑收款账号' : '添加收款账号'" width="440px" @close="resetForm">
        <el-form :model="form" label-width="100px">
          <el-form-item label="支付宝账号" required>
            <el-input v-model="form.alipayAccount" placeholder="请输入支付宝账号" />
          </el-form-item>
          <el-form-item label="真实姓名" required>
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaymentConfigs, createPaymentConfig, updatePaymentConfig, deletePaymentConfig, setDefaultPaymentConfig, type PaymentConfigVO } from '@/api/paymentConfig'
import EmptyState from '@/components/EmptyState.vue'

const configs = ref<PaymentConfigVO[]>([])
const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)
const editingConfig = ref<PaymentConfigVO | null>(null)

const form = ref({ alipayAccount: '', realName: '', isDefault: 0 })

const loadData = async () => {
  loading.value = true
  try { configs.value = await getPaymentConfigs() || [] } finally { loading.value = false }
}

const resetForm = () => {
  form.value = { alipayAccount: '', realName: '', isDefault: 0 }
  editingConfig.value = null
}

const handleEdit = (config: PaymentConfigVO) => {
  editingConfig.value = config
  form.value = { alipayAccount: config.alipayAccount, realName: config.realName, isDefault: config.isDefault }
  showAddDialog.value = true
}

const handleSubmit = async () => {
  if (!form.value.alipayAccount.trim()) { ElMessage.warning('请输入支付宝账号'); return }
  if (!form.value.realName.trim()) { ElMessage.warning('请输入真实姓名'); return }
  submitting.value = true
  try {
    if (editingConfig.value) {
      await updatePaymentConfig(editingConfig.value.id, form.value.alipayAccount, form.value.realName, form.value.isDefault)
      ElMessage.success('修改成功')
    } else {
      await createPaymentConfig(form.value.alipayAccount, form.value.realName, form.value.isDefault)
      ElMessage.success('添加成功')
    }
    showAddDialog.value = false
    loadData()
  } catch (e) { console.error(e) } finally { submitting.value = false }
}

const handleSetDefault = async (id: number) => {
  await setDefaultPaymentConfig(id)
  ElMessage.success('已设为默认')
  loadData()
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确认删除该收款配置？', '删除确认', { type: 'warning' })
  await deletePaymentConfig(id)
  ElMessage.success('已删除')
  loadData()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.payment-config-page { padding: 20px; }
.payment-config-inner {
  background: var(--bg-glass);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  padding: 24px;
  max-width: 640px;
  margin: 0 auto;
}
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { margin: 0; }
.config-list { display: flex; flex-direction: column; gap: 12px; }
.config-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px; border-radius: var(--radius-md);
  border: 1px solid var(--border); background: var(--bg-card);
  transition: var(--transition);
  &.is-default { border-color: var(--primary-light); box-shadow: 0 0 0 1px var(--primary-lighter); }
}
.config-info { display: flex; align-items: center; gap: 12px; }
.config-detail { display: flex; flex-direction: column; gap: 2px; }
.config-account { font-weight: 600; color: var(--text-primary); font-size: 14px; }
.config-name { font-size: 12px; color: var(--text-muted); }
.config-actions { display: flex; gap: 4px; }

.list-enter-active { transition: all 0.3s ease; }
.list-leave-active { transition: all 0.2s ease; }
.list-enter-from { opacity: 0; transform: translateY(-10px); }
.list-leave-to { opacity: 0; transform: translateX(20px); }

@media (max-width: 768px) {
  .payment-config-inner { padding: 16px; }
}
</style>
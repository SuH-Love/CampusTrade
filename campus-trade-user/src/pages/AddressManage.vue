<template>
  <div class="address-page page-bg">
    <div class="address-inner page-container">
    <div class="page-header">
      <h2>收货地址管理</h2>
      <el-button type="primary" @click="openDialog()" round>新增地址</el-button>
    </div>
    <div class="address-list" v-if="addressList.length > 0">
      <div v-for="addr in addressList" :key="addr.id" class="address-card" :class="{ default: addr.isDefault === 1 }">
        <div class="address-body">
          <div class="address-top">
            <span class="receiver-name">{{ addr.receiverName }}</span>
            <span class="receiver-phone">{{ addr.receiverPhone }}</span>
            <el-tag v-if="addr.isDefault === 1" type="danger" size="small" effect="dark" round>默认</el-tag>
          </div>
          <div class="address-detail">
            {{ [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') }}
          </div>
        </div>
        <div class="address-actions">
          <el-button v-if="addr.isDefault !== 1" text size="small" @click="handleSetDefault(addr.id)">设为默认</el-button>
          <el-button text size="small" @click="openDialog(addr)">编辑</el-button>
          <el-button text size="small" type="danger" @click="handleDelete(addr.id)">删除</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无收货地址" />

    <el-dialog v-model="dialogVisible" :title="editingAddr ? '编辑地址' : '新增地址'" width="520px" destroy-on-close append-to-body>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省/市/区">
          <el-cascader v-model="areaValue" :options="areaOptions" :props="cascaderProps" placeholder="请选择省/市/区" class="w-full" @change="handleAreaChange" teleported />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" :rows="2" placeholder="街道、楼栋、门牌号等" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" round>取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" round>保存</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress, type DeliveryAddressVO } from '@/api/address'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import areaOptions from '@/data/area'

const addressList = ref<DeliveryAddressVO[]>([])
const dialogVisible = ref(false)
const submitting = ref(false)
const editingAddr = ref<DeliveryAddressVO | null>(null)
const formRef = ref<FormInstance>()
const areaValue = ref<string[]>([])

const cascaderProps = {
  expandTrigger: 'hover' as const
}

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const handleAreaChange = (val: string[]) => {
  form.province = val[0] || ''
  form.city = val[1] || ''
  form.district = val[2] || ''
}

const loadList = async () => {
  try { addressList.value = await getAddressList() } catch { /* ignore */ }
}

const openDialog = (addr?: DeliveryAddressVO) => {
  editingAddr.value = addr || null
  if (addr) {
    form.receiverName = addr.receiverName
    form.receiverPhone = addr.receiverPhone
    form.province = addr.province || ''
    form.city = addr.city || ''
    form.district = addr.district || ''
    form.detailAddress = addr.detailAddress
    form.isDefault = addr.isDefault
    areaValue.value = [form.province, form.city, form.district].filter(Boolean)
  } else {
    form.receiverName = ''
    form.receiverPhone = ''
    form.province = ''
    form.city = ''
    form.district = ''
    form.detailAddress = ''
    form.isDefault = 0
    areaValue.value = []
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  if (!form.province || !form.city || !form.district) {
    ElMessage.warning('请选择省/市/区')
    return
  }
  submitting.value = true
  try {
    const data = { ...form }
    if (editingAddr.value) {
      await updateAddress(editingAddr.value.id, data)
      ElMessage.success('修改成功')
    } else {
      await addAddress(data)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally { submitting.value = false }
}

const handleSetDefault = async (id: number) => {
  try {
    await setDefaultAddress(id)
    ElMessage.success('已设为默认')
    await loadList()
  } catch { /* ignore */ }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定删除该地址？', '提示', { type: 'warning' })
    await deleteAddress(id)
    ElMessage.success('已删除')
    await loadList()
  } catch { /* cancel */ }
}

onMounted(loadList)
</script>

<style scoped lang="scss">
.address-page {
  padding: 20px;

}
.address-inner {
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.page-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px;
  h2 { margin: 0; font-size: 22px; font-weight: 700; }
}
.address-card {
  background: var(--bg-card); border-radius: var(--radius-md); padding: 20px;
  border: 1px solid var(--border); margin-bottom: 16px;
  transition: var(--transition);
  display: flex; justify-content: space-between; align-items: center;
  &:hover { border-color: var(--primary-light); box-shadow: var(--shadow-sm); }
  &.default { border-color: var(--primary); background: linear-gradient(135deg, rgba(99,102,241,0.04), rgba(139,92,246,0.04)); }
}
.address-body { flex: 1; }
.address-top { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.receiver-name { font-weight: 600; font-size: 16px; }
.receiver-phone { color: var(--text-secondary); font-size: 14px; }
.address-detail { color: var(--text-secondary); font-size: 14px; line-height: 1.6; }
.address-actions { display: flex; gap: 4px; flex-shrink: 0; }
</style>
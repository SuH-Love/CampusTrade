<template>
  <el-dialog :model-value="visible" @update:model-value="emit('update:visible', $event)" title="确认购买" width="520px" destroy-on-close>
    <div v-if="goods">
      <p class="buy-confirm-text">确认购买「{{ goods.title }}」？单价 ¥{{ goods.price }}</p>
      <div class="buy-quantity-row">
        <span class="buy-label">购买数量：</span>
        <el-input-number v-model="buyQuantity" :min="1" :max="goods.stock || 1" size="small" />
        <span class="buy-stock-hint">（库存 {{ goods.stock || 1 }} 件）</span>
      </div>
      <div class="buy-delivery-row">
        <span class="buy-label">配送方式：</span>
        <el-radio-group v-model="buyDeliveryMethod">
          <el-radio value="PICKUP">自取</el-radio>
          <el-radio value="DELIVERY">配送</el-radio>
        </el-radio-group>
      </div>
      <template v-if="buyDeliveryMethod === 'DELIVERY'">
        <div v-if="addresses.length > 0" class="buy-address-list">
          <div class="buy-address-label">选择已有地址：</div>
          <div
            v-for="addr in addresses" :key="addr.id"
            class="buy-address-item"
            :class="{ active: buySelectedAddressId === addr.id }"
            @click="buySelectedAddressId = addr.id"
          >
            <div class="buy-address-name">{{ addr.receiverName }} {{ addr.receiverPhone }}</div>
            <div class="buy-address-detail">{{ [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') }}</div>
          </div>
        </div>
        <div class="buy-add-addr-toggle">
          <el-button type="primary" size="small" link @click="buyShowAddAddr = !buyShowAddAddr">{{ buyShowAddAddr ? '收起' : '新增收货地址' }}</el-button>
        </div>
        <div v-if="buyShowAddAddr" class="buy-add-addr-form">
          <el-form :model="buyAddrForm" label-width="90px" size="small">
            <el-row :gutter="8">
              <el-col :span="12"><el-form-item label="收货人"><el-input v-model="buyAddrForm.receiverName" placeholder="收货人" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="手机号"><el-input v-model="buyAddrForm.receiverPhone" placeholder="手机号" /></el-form-item></el-col>
            </el-row>
            <el-form-item label="省/市/区">
              <el-cascader v-model="buyAreaValue" :options="areaOptions" :props="{ expandTrigger: 'hover' }" placeholder="请选择" class="buy-area-cascader" teleported />
            </el-form-item>
            <el-form-item label="详细地址"><el-input v-model="buyAddrForm.detailAddress" placeholder="街道、楼栋、门牌号" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleBuyAddAddress" :loading="buyAddrSaving">保存地址</el-button></el-form-item>
          </el-form>
        </div>
      </template>
      <div class="buy-remark-row">
        <span class="buy-label">订单备注：</span>
        <el-input v-model="buyRemark" placeholder="选填，如特殊要求等" size="small" class="buy-remark-input" />
      </div>
    </div>
    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleConfirm" :loading="confirmLoading">确认购买</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { addAddress, getAddressList, type DeliveryAddressVO } from '@/api/address'
import { ElMessage } from 'element-plus'
import areaOptions from '@/data/area'
import type { GoodsVO } from '@/api/goods'

const props = defineProps<{
  visible: boolean
  goods: GoodsVO | null
  addresses: DeliveryAddressVO[]
}>()

const emit = defineEmits<{
  'update:visible': [val: boolean]
  confirm: [data: { goodsId: number; quantity: number; remark?: string; deliveryMethod?: string; deliveryAddress?: string }]
}>()

const buyQuantity = ref(1)
const buyDeliveryMethod = ref('PICKUP')
const buySelectedAddressId = ref<number | null>(null)
const buyShowAddAddr = ref(false)
const buyAddrSaving = ref(false)
const buyAreaValue = ref<string[]>([])
const buyRemark = ref('')
const confirmLoading = ref(false)
const buyAddrForm = reactive({ receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
const localAddresses = ref<DeliveryAddressVO[]>([])

watch(() => props.visible, (val) => {
  if (val) {
    buyQuantity.value = 1
    buyDeliveryMethod.value = 'PICKUP'
    buySelectedAddressId.value = null
    buyShowAddAddr.value = false
    buyAreaValue.value = []
    buyRemark.value = ''
    buyAddrForm.receiverName = ''; buyAddrForm.receiverPhone = ''; buyAddrForm.province = ''; buyAddrForm.city = ''; buyAddrForm.district = ''; buyAddrForm.detailAddress = ''
    localAddresses.value = [...props.addresses]
  }
})

const handleBuyAddAddress = async () => {
  if (!buyAddrForm.receiverName || !buyAddrForm.receiverPhone || !buyAddrForm.detailAddress) { ElMessage.error('请填写收货人、手机号和详细地址'); return }
  if (buyAreaValue.value.length === 3) {
    buyAddrForm.province = buyAreaValue.value[0]
    buyAddrForm.city = buyAreaValue.value[1]
    buyAddrForm.district = buyAreaValue.value[2]
  }
  buyAddrSaving.value = true
  try {
    await addAddress(buyAddrForm)
    localAddresses.value = await getAddressList()
    const newest = localAddresses.value[0]
    if (newest) buySelectedAddressId.value = newest.id
    buyShowAddAddr.value = false
    buyAddrForm.receiverName = ''; buyAddrForm.receiverPhone = ''; buyAddrForm.province = ''; buyAddrForm.city = ''; buyAddrForm.district = ''; buyAddrForm.detailAddress = ''
    buyAreaValue.value = []
    ElMessage.success('地址添加成功')
  } catch (e) { console.error(e); ElMessage.error('添加失败') } finally { buyAddrSaving.value = false }
}

const handleConfirm = () => {
  if (!props.goods) return
  if (buyDeliveryMethod.value === 'DELIVERY' && !buySelectedAddressId.value) {
    ElMessage.error('请选择配送地址')
    return
  }
  const data: { goodsId: number; quantity: number; remark?: string; deliveryMethod?: string; deliveryAddress?: string } = { goodsId: props.goods.id, quantity: buyQuantity.value }
  if (buyRemark.value.trim()) data.remark = buyRemark.value.trim()
  if (buyDeliveryMethod.value === 'DELIVERY') {
    data.deliveryMethod = 'DELIVERY'
    const addr = localAddresses.value.find(a => a.id === buySelectedAddressId.value)
    data.deliveryAddress = addr ? [addr.province, addr.city, addr.district, addr.detailAddress].filter(Boolean).join(' ') + ` (${addr.receiverName} ${addr.receiverPhone})` : ''
  } else {
    data.deliveryMethod = 'PICKUP'
  }
  emit('confirm', data)
}
</script>

<style scoped lang="scss">
.buy-confirm-text { margin-bottom: 12px; }

.buy-quantity-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.buy-label { margin-right: 12px; }

.buy-stock-hint {
  margin-left: 8px;
  color: var(--text-muted);
  font-size: 13px;
}

.buy-delivery-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}

.buy-address-list { margin-bottom: 10px; }

.buy-address-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.buy-address-item {
  padding: 10px 14px;
  margin-bottom: 6px;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { border-color: var(--primary); }
  &.active { border-color: var(--primary); background: rgba(14, 165, 233, 0.06); }
}

.buy-address-name { font-size: 14px; font-weight: 500; }

.buy-address-detail {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.buy-add-addr-toggle { margin-bottom: 8px; }

.buy-add-addr-form {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
  background: var(--bg-hover);
}

.buy-area-cascader { width: 100%; }

.buy-remark-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.buy-remark-input {
  width: 100%;
  margin-top: 4px;
}
</style>
<template>
  <el-dialog v-model="visible" :title="title" width="460px" :close-on-click-modal="false" destroy-on-close>
    <el-form label-position="top">
      <el-form-item :label="label" required>
        <el-input v-model="reason" type="textarea" :rows="4" :placeholder="placeholder" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button :type="btnType" @click="handleConfirm" :loading="loading">{{ confirmText }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  label?: string
  placeholder?: string
  confirmText?: string
  btnType?: '' | 'primary' | 'success' | 'warning' | 'danger'
  loading?: boolean
}>(), {
  label: '原因',
  placeholder: '请输入原因',
  confirmText: '确认',
  btnType: 'danger',
  loading: false,
})

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  confirm: [reason: string]
}>()

const visible = ref(props.modelValue)
const reason = ref('')

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) reason.value = ''
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleConfirm = () => {
  if (!reason.value.trim()) {
    ElMessage.warning(`请输入${props.label}`)
    return
  }
  emit('confirm', reason.value)
}
</script>

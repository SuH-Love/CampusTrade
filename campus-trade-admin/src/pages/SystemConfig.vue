<template>
  <div class="admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>支付宝沙箱配置</span>
          <el-tag :type="alipayConfigured ? 'success' : 'danger'" size="small">
            {{ alipayConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </div>
      </template>

      <el-alert v-if="!alipayConfigured" title="支付宝未配置，支付功能将使用模拟模式" type="warning" :closable="false" show-icon style="margin-bottom: 20px" />

      <el-form label-width="140px" v-loading="loading">
        <el-form-item label="应用ID (AppID)">
          <el-input v-model="form['alipay.app_id']" placeholder="支付宝沙箱应用ID" clearable />
        </el-form-item>
        <el-form-item label="应用私钥">
          <el-input v-model="form['alipay.private_key']" type="textarea" :rows="3" placeholder="RSA2私钥（已配置显示为******，重新输入覆盖）" show-password />
        </el-form-item>
        <el-form-item label="支付宝公钥">
          <el-input v-model="form['alipay.alipay_public_key']" type="textarea" :rows="3" placeholder="支付宝公钥（已配置显示为******，重新输入覆盖）" show-password />
        </el-form-item>
        <el-form-item label="网关地址">
          <el-input v-model="form['alipay.gateway']" placeholder="沙箱: https://openapi-sandbox.dl.alipaydev.com/gateway.do" />
        </el-form-item>
        <el-form-item label="异步通知URL">
          <el-input v-model="form['alipay.notify_url']" placeholder="http://你的服务器IP/api/order/pay/notify" />
        </el-form-item>
        <el-form-item label="同步跳转URL">
          <el-input v-model="form['alipay.return_url']" placeholder="http://你的服务器IP/order/" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
          <el-button @click="loadData">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <el-descriptions title="配置说明" :column="1" border size="small">
        <el-descriptions-item label="获取方式">
          登录 <el-link type="primary" href="https://open.alipay.com" target="_blank">支付宝开放平台</el-link> → 沙箱应用 → 获取 AppID、密钥
        </el-descriptions-item>
        <el-descriptions-item label="密钥格式">RSA2（推荐），使用支付宝密钥生成工具生成</el-descriptions-item>
        <el-descriptions-item label="异步通知">必须为支付宝服务器可访问的公网地址</el-descriptions-item>
        <el-descriptions-item label="安全提示">私钥在数据库中AES加密存储，管理端仅显示掩码</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>AI 助手配置</span>
          <el-tag :type="aiConfig.enabled ? 'success' : 'danger'" size="small">
            {{ aiConfig.enabled ? '在线' : '离线' }}
          </el-tag>
        </div>
      </template>

      <el-form label-width="140px" v-loading="aiLoading">
        <el-form-item label="当前模型">
          <el-input v-model="aiForm.model" placeholder="如 deepseek-ai/DeepSeek-V4-Flash" clearable />
        </el-form-item>
        <el-form-item label="API 地址">
          <el-input v-model="aiForm.baseUrl" placeholder="如 https://api.siliconflow.cn/v1" clearable />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input :model-value="aiConfig.apiKeyMasked || '未配置'" disabled>
            <template #append>
              <el-button @click="showApiKeyInput = !showApiKeyInput">更新</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="showApiKeyInput" label="新 API Key">
          <el-input v-model="aiForm.apiKey" type="password" show-password placeholder="输入新的 API Key" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="aiSaving" @click="handleSaveAiConfig">保存配置</el-button>
          <el-button @click="loadAiConfig">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <el-descriptions title="配置说明" :column="1" border size="small">
        <el-descriptions-item label="获取方式">
          登录 <el-link type="primary" href="https://siliconflow.cn" target="_blank">硅基流动平台</el-link> → API Keys → 新建密钥
        </el-descriptions-item>
        <el-descriptions-item label="模型名称">如 deepseek-ai/DeepSeek-V4-Flash，在平台模型列表中查看</el-descriptions-item>
        <el-descriptions-item label="API 地址">硅基流动: https://api.siliconflow.cn/v1</el-descriptions-item>
        <el-descriptions-item label="热更新">所有配置更新后立即生效，无需重启服务</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig, getAlipayStatus, getAiConfigStatus, updateAiConfig, type SystemConfigVO, type AiConfigStatus } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const alipayConfigured = ref(false)
const form = reactive<Record<string, string>>({
  'alipay.app_id': '',
  'alipay.private_key': '',
  'alipay.alipay_public_key': '',
  'alipay.gateway': '',
  'alipay.notify_url': '',
  'alipay.return_url': ''
})

const aiLoading = ref(false)
const aiSaving = ref(false)
const showApiKeyInput = ref(false)
const aiConfig = ref<AiConfigStatus>({ enabled: false, model: '', apiKeyMasked: '', baseUrl: '' })
const aiForm = reactive({ model: '', baseUrl: '', apiKey: '' })

const loadAiConfig = async () => {
  aiLoading.value = true
  try {
    aiConfig.value = await getAiConfigStatus()
    aiForm.model = aiConfig.value.model
    aiForm.baseUrl = aiConfig.value.baseUrl
    aiForm.apiKey = ''
    showApiKeyInput.value = false
  } catch (e) { console.error(e) } finally { aiLoading.value = false }
}

const handleSaveAiConfig = async () => {
  aiSaving.value = true
  try {
    const data: { apiKey?: string; model?: string; baseUrl?: string } = {}
    if (aiForm.model !== aiConfig.value.model) data.model = aiForm.model
    if (aiForm.baseUrl !== aiConfig.value.baseUrl) data.baseUrl = aiForm.baseUrl
    if (showApiKeyInput.value && aiForm.apiKey.trim()) data.apiKey = aiForm.apiKey.trim()
    if (Object.keys(data).length === 0) { ElMessage.info('无变更'); return }
    const res = await updateAiConfig(data)
    aiConfig.value = res
    aiForm.model = res.model
    aiForm.baseUrl = res.baseUrl
    aiForm.apiKey = ''
    showApiKeyInput.value = false
    ElMessage.success('AI 配置已保存')
  } catch (e) { console.error(e) } finally { aiSaving.value = false }
}

const loadData = async () => {
  loading.value = true
  try {
    const configs = await getSystemConfig()
    if (configs) {
      for (const c of configs) {
        if (form.hasOwnProperty(c.configKey)) {
          form[c.configKey] = c.configValue || ''
        }
      }
    }
    const status = await getAlipayStatus()
    alipayConfigured.value = !!status?.configured
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleSave = async () => {
  saving.value = true
  try {
    const configs: SystemConfigVO[] = Object.entries(form).map(([key, value]) => ({
      id: 0,
      configKey: key,
      configValue: value,
      description: ''
    }))
    await updateSystemConfig(configs)
    ElMessage.success('配置已保存')
    loadData()
  } catch (e) { console.error(e) } finally { saving.value = false }
}

onMounted(() => { loadData(); loadAiConfig() })
</script>

<style scoped lang="scss">

.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
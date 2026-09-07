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
          <span>邮件服务配置（QQ邮箱）</span>
          <el-tag :type="mailConfigured ? 'success' : 'danger'" size="small">
            {{ mailConfigured ? '已配置' : '未配置' }}
          </el-tag>
        </div>
      </template>

      <el-alert v-if="!mailConfigured" title="邮件服务未配置，重置密码功能不可用" type="warning" :closable="false" show-icon style="margin-bottom: 20px" />

      <el-form label-width="140px" v-loading="loading">
        <el-form-item label="SMTP 服务器">
          <el-input v-model="form['mail.host']" placeholder="smtp.qq.com" clearable />
        </el-form-item>
        <el-form-item label="SMTP 端口">
          <el-input v-model="form['mail.port']" placeholder="465（SSL）或 587（TLS）" clearable />
        </el-form-item>
        <el-form-item label="发件人邮箱">
          <el-input v-model="form['mail.username']" placeholder="你的QQ邮箱地址，如 123456@qq.com" clearable />
        </el-form-item>
        <el-form-item label="邮箱授权码">
          <el-input v-model="form['mail.password']" type="password" show-password placeholder="QQ邮箱授权码（已配置显示为******，重新输入覆盖）" clearable />
        </el-form-item>
        <el-form-item label="发件人名称">
          <el-input v-model="form['mail.from']" placeholder="如 CampusTrade校园贸易" clearable />
        </el-form-item>
        <el-form-item label="启用SSL">
          <el-switch v-model="mailSslEnabled" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
          <el-button @click="loadData">重置</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <el-descriptions title="配置说明" :column="1" border size="small">
        <el-descriptions-item label="获取授权码">
          登录 <el-link type="primary" href="https://mail.qq.com" target="_blank">QQ邮箱</el-link> → 设置 → 账户 → POP3/SMTP服务 → 开启 → 生成授权码
        </el-descriptions-item>
        <el-descriptions-item label="授权码说明">授权码不是QQ密码，是QQ邮箱生成的16位独立密码</el-descriptions-item>
        <el-descriptions-item label="端口选择">465（SSL加密，推荐）或 587（STARTTLS）</el-descriptions-item>
        <el-descriptions-item label="热更新">配置更新后立即生效，无需重启服务</el-descriptions-item>
        <el-descriptions-item label="安全提示">授权码在数据库中AES加密存储，管理端仅显示掩码</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>AI 助手配置</span>
          <el-tag :type="aiConfig.healthy ? 'success' : 'danger'" size="small">
            {{ aiConfig.healthy ? '在线' : '离线' }}
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

      <el-divider content-position="left">Embedding 向量检索配置</el-divider>
      <el-form label-width="140px" v-loading="aiLoading">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
          title="DeepSeek不支持embedding API，须独立配置其他embedding服务（如OpenAI、智谱等）。留空则降级到TF-IDF检索。" />
        <el-form-item label="Embedding可用">
          <el-tag :type="aiConfig.embeddingAvailable ? 'success' : 'warning'" size="small">
            {{ aiConfig.embeddingAvailable ? '可用' : '降级到TF-IDF' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="Embedding模型">
          <el-input v-model="aiForm.embModel" placeholder="如 text-embedding-3-small / embedding-2" clearable />
        </el-form-item>
        <el-form-item label="Embedding API地址">
          <el-input v-model="aiForm.embBaseUrl" placeholder="如 https://api.openai.com/v1（留空则用主API地址）" clearable />
        </el-form-item>
        <el-form-item label="Embedding API Key">
          <el-input :model-value="aiConfig.embApiKeyMasked || '未配置（将用主API Key）'" disabled>
            <template #append>
              <el-button @click="showEmbKeyInput = !showEmbKeyInput">更新</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="showEmbKeyInput" label="新Embedding Key">
          <el-input v-model="aiForm.embApiKey" type="password" show-password placeholder="输入embedding服务的API Key" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="aiSaving" @click="handleSaveAiConfig">保存Embedding配置</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">多模型路由配置</el-divider>
      <el-form label-width="140px" v-loading="aiLoading">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px"
          title="启用后，含分析/计算/比较/推荐等关键词的问题自动路由到推理模型(deepseek-reasoner)，其余用主模型。" />
        <el-form-item label="启用路由">
          <el-switch v-model="aiForm.routingEnabled" />
        </el-form-item>
        <el-form-item label="推理模型">
          <el-input v-model="aiForm.reasonerModel" placeholder="如 deepseek-reasoner" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="aiSaving" @click="handleSaveAiConfig">保存路由配置</el-button>
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

    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>AI 系统提示词（System Prompt）</span>
        </div>
      </template>
      <el-form label-width="140px" v-loading="promptLoading">
        <el-form-item label="当前提示词">
          <el-input v-model="promptForm" type="textarea" :rows="8" placeholder="定义AI助手的角色、职责、行为约束..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="promptSaving" @click="handleSavePrompt">保存提示词</el-button>
          <el-button @click="loadPrompt">重置</el-button>
        </el-form-item>
      </el-form>
      <el-divider />
      <el-descriptions title="说明" :column="1" border size="small">
        <el-descriptions-item label="作用">定义AI助手"小苏"的角色、职责、可用工具、回答风格</el-descriptions-item>
        <el-descriptions-item label="热更新">更新后立即生效，无需重启服务</el-descriptions-item>
        <el-descriptions-item label="长度限制">最多5000字符</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig, getAlipayStatus, getAiConfigStatus, updateAiConfig, getAiSystemPrompt, updateAiSystemPrompt, type SystemConfigVO, type AiConfigStatus } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const alipayConfigured = ref(false)
const mailConfigured = ref(false)
const mailSslEnabled = ref(true)
const form = reactive<Record<string, string>>({
  'alipay.app_id': '',
  'alipay.private_key': '',
  'alipay.alipay_public_key': '',
  'alipay.gateway': '',
  'alipay.notify_url': '',
  'alipay.return_url': '',
  'mail.host': '',
  'mail.port': '',
  'mail.username': '',
  'mail.password': '',
  'mail.from': '',
  'mail.ssl': ''
})

const aiLoading = ref(false)
const aiSaving = ref(false)
const showApiKeyInput = ref(false)
const showEmbKeyInput = ref(false)
const aiConfig = ref<AiConfigStatus>({ enabled: false, healthy: false, model: '', apiKeyMasked: '', baseUrl: '' })
const aiForm = reactive({
  model: '', baseUrl: '', apiKey: '',
  embApiKey: '', embBaseUrl: '', embModel: '',
  routingEnabled: false, reasonerModel: ''
})

const promptLoading = ref(false)
const promptSaving = ref(false)
const promptForm = ref('')

const loadAiConfig = async () => {
  aiLoading.value = true
  try {
    aiConfig.value = await getAiConfigStatus()
    aiForm.model = aiConfig.value.model
    aiForm.baseUrl = aiConfig.value.baseUrl
    aiForm.apiKey = ''
    aiForm.embApiKey = ''
    aiForm.embBaseUrl = aiConfig.value.embBaseUrl || ''
    aiForm.embModel = aiConfig.value.embModel || ''
    aiForm.routingEnabled = aiConfig.value.routingEnabled || false
    aiForm.reasonerModel = aiConfig.value.reasonerModel || ''
    showApiKeyInput.value = false
    showEmbKeyInput.value = false
  } catch (e) { console.error(e) } finally { aiLoading.value = false }
}

const handleSaveAiConfig = async () => {
  aiSaving.value = true
  try {
    const data: Record<string, string> = {}
    if (aiForm.model !== aiConfig.value.model) data.model = aiForm.model
    if (aiForm.baseUrl !== aiConfig.value.baseUrl) data.baseUrl = aiForm.baseUrl
    if (showApiKeyInput.value && aiForm.apiKey.trim()) data.apiKey = aiForm.apiKey.trim()
    if (showEmbKeyInput.value) data.embApiKey = aiForm.embApiKey.trim()
    if (aiForm.embBaseUrl !== (aiConfig.value.embBaseUrl || '')) data.embBaseUrl = aiForm.embBaseUrl
    if (aiForm.embModel !== (aiConfig.value.embModel || '')) data.embModel = aiForm.embModel
    if (aiForm.routingEnabled !== (aiConfig.value.routingEnabled || false)) data.routingEnabled = String(aiForm.routingEnabled)
    if (aiForm.reasonerModel !== (aiConfig.value.reasonerModel || '')) data.reasonerModel = aiForm.reasonerModel
    if (Object.keys(data).length === 0) { ElMessage.info('无变更'); return }
    const res = await updateAiConfig(data)
    aiConfig.value = res
    aiForm.model = res.model
    aiForm.baseUrl = res.baseUrl
    aiForm.apiKey = ''
    aiForm.embApiKey = ''
    aiForm.embBaseUrl = res.embBaseUrl || ''
    aiForm.embModel = res.embModel || ''
    aiForm.routingEnabled = res.routingEnabled || false
    aiForm.reasonerModel = res.reasonerModel || ''
    showApiKeyInput.value = false
    showEmbKeyInput.value = false
    ElMessage.success('AI 配置已保存')
  } catch (e) { console.error(e) } finally { aiSaving.value = false }
}

const loadPrompt = async () => {
  promptLoading.value = true
  try {
    promptForm.value = await getAiSystemPrompt()
  } catch (e) { console.error(e) } finally { promptLoading.value = false }
}

const handleSavePrompt = async () => {
  if (!promptForm.value.trim()) { ElMessage.warning('提示词不能为空'); return }
  promptSaving.value = true
  try {
    await updateAiSystemPrompt(promptForm.value)
    ElMessage.success('系统提示词已保存')
  } catch (e) { console.error(e) } finally { promptSaving.value = false }
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
    mailSslEnabled.value = form['mail.ssl'] !== 'false'
    mailConfigured.value = !!(form['mail.username'] && form['mail.password'] && form['mail.password'] !== '')
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleSave = async () => {
  saving.value = true
  try {
    form['mail.ssl'] = mailSslEnabled.value ? 'true' : 'false'
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

onMounted(() => { loadData(); loadAiConfig(); loadPrompt() })
</script>

<style scoped lang="scss">

.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
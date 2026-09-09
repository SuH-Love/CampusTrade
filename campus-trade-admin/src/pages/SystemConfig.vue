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
          <span>AI 助手配置 — 渠道管理</span>
          <el-tag :type="aiConfig.healthy ? 'success' : 'danger'" size="small">
            {{ aiConfig.healthy ? '在线' : '离线' }}
          </el-tag>
        </div>
      </template>

      <div v-loading="channelLoading" class="ai-compact-list">
        <div v-for="(ch, idx) in channels" :key="idx" class="ai-row">
          <el-input v-model="ch.name" placeholder="渠道名称" class="ai-col-name" />
          <el-input v-model="ch.baseUrl" placeholder="API 地址" class="ai-col-url" />
          <el-input v-model="ch.apiKey" :placeholder="ch.apiKey ? '已配置(输入覆盖)' : 'API Key'" class="ai-col-key" />
          <el-input-number v-model="ch.priority" :min="1" :max="99" controls-position="right" class="ai-col-pri" />
          <el-switch v-model="ch.enabled" />
          <el-button type="danger" link @click="channels.splice(idx, 1)">删除</el-button>
        </div>
        <div v-if="!channels.length && !channelLoading" class="empty-tip">暂无渠道</div>
      </div>
      <div class="ai-bar">
        <el-button @click="channels.push({ id: 'ch' + Date.now(), name: '', baseUrl: '', apiKey: '', priority: channels.length + 1, enabled: true })">+ 新增渠道</el-button>
        <el-button type="primary" :loading="channelSaving" @click="handleSaveChannels">保存</el-button>
      </div>

      <el-divider content-position="left">模型注册</el-divider>
      <div v-loading="modelLoading" class="ai-compact-list">
        <div v-for="(m, idx) in modelRegs" :key="idx" class="ai-row ai-row-model">
          <el-select v-model="m.channelId" placeholder="渠道" class="ai-col-sel">
            <el-option v-for="ch in channels" :key="ch.id" :label="ch.name" :value="ch.id" />
          </el-select>
          <el-input v-model="m.model" placeholder="模型名" class="ai-col-model" />
          <div class="ai-caps-group">
            <span v-for="cap in capOptions" :key="cap.value"
              class="ai-cap-tag" :class="{ active: m.caps.includes(cap.value) }"
              @click="toggleCap(m, cap.value)">
              {{ cap.label }}
            </span>
          </div>
          <el-button type="danger" link @click="modelRegs.splice(idx, 1)">删除</el-button>
        </div>
        <div v-if="!modelRegs.length && !modelLoading" class="empty-tip">暂无模型</div>
      </div>
      <div class="ai-bar">
        <el-button @click="modelRegs.push({ channelId: channels[0]?.id || '', model: '', caps: ['chat'] })">+ 新增模型</el-button>
        <el-button type="primary" :loading="modelSaving" @click="handleSaveModels">保存</el-button>
      </div>

      <el-divider />
      <el-descriptions title="说明" :column="1" border size="small">
        <el-descriptions-item label="渠道">API服务商连接配置，可服务多个模型</el-descriptions-item>
        <el-descriptions-item label="能力标签">点击切换，系统按场景自动路由：图片→图片标签、分析→分析标签、FAQ→向量标签、其他→对话标签</el-descriptions-item>
        <el-descriptions-item label="优先级">数字越小优先级越高，失败自动降级</el-descriptions-item>
        <el-descriptions-item label="API Key">仅显示掩码，输入新值可覆盖</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 20px">
      <template #header><div class="card-header"><span>AI 系统提示词（System Prompt）</span></div></template>
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
        <el-descriptions-item label="作用">定义AI助手"小苏"的角色、职责、可用工具、回答风格。图片能力说明由后端自动注入，无需手动添加。</el-descriptions-item>
        <el-descriptions-item label="热更新">更新后立即生效，无需重启服务</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemConfig, updateSystemConfig, getAlipayStatus, getAiConfigStatus, getAiSystemPrompt, updateAiSystemPrompt, getAiChannels, saveAiChannels, getAiModels, saveAiModels, type SystemConfigVO, type AiConfigStatus, type AiChannel, type AiModelReg } from '@/api/admin'

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

const aiConfig = ref<AiConfigStatus>({ enabled: false, healthy: false, model: '', apiKeyMasked: '', baseUrl: '' })
const channels = ref<AiChannel[]>([])
const modelRegs = ref<AiModelReg[]>([])
const channelLoading = ref(false)
const channelSaving = ref(false)
const modelLoading = ref(false)
const modelSaving = ref(false)

const capOptions = [
  { value: 'chat', label: '对话' },
  { value: 'reasoning', label: '分析' },
  { value: 'vision', label: '图片' },
  { value: 'embedding', label: '向量' }
]
const toggleCap = (m: AiModelReg, cap: string) => {
  const i = m.caps.indexOf(cap)
  if (i >= 0) m.caps.splice(i, 1)
  else m.caps.push(cap)
}

const loadChannels = async () => {
  channelLoading.value = true
  try {
    const json = await getAiChannels()
    const raw = JSON.parse(json || '[]')
    channels.value = raw.map((ch: Record<string, unknown>) => ({
      id: (ch.id as string) || ('ch' + Date.now() + Math.random()),
      name: (ch.name as string) || '',
      baseUrl: (ch.baseUrl as string) || '',
      apiKey: (ch.apiKey as string) || '',
      priority: (ch.priority as number) || 1,
      enabled: ch.enabled !== false
    }))
  } catch { channels.value = [] } finally { channelLoading.value = false }
}

const handleSaveChannels = async () => {
  channelSaving.value = true
  try {
    await saveAiChannels(channels.value)
    ElMessage.success('渠道配置已保存')
  } catch { ElMessage.error('保存失败') } finally { channelSaving.value = false }
}

const loadModels = async () => {
  modelLoading.value = true
  try {
    const json = await getAiModels()
    const raw = JSON.parse(json || '[]')
    modelRegs.value = raw.map((m: Record<string, unknown>) => ({
      channelId: (m.channelId as string) || (m.channel as string) || '',
      model: (m.model as string) || (m.name as string) || '',
      caps: (m.caps as string[]) || (m.capabilities as string[]) || ['chat']
    }))
  } catch { modelRegs.value = [] } finally { modelLoading.value = false }
}

const handleSaveModels = async () => {
  modelSaving.value = true
  try {
    await saveAiModels(modelRegs.value)
    ElMessage.success('模型配置已保存')
  } catch { ElMessage.error('保存失败') } finally { modelSaving.value = false }
}

const promptLoading = ref(false)
const promptSaving = ref(false)
const promptForm = ref('')

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

onMounted(() => { loadData(); loadChannels(); loadModels(); loadPrompt(); getAiConfigStatus().then(r => aiConfig.value = r).catch(() => {}) })
</script>

<style scoped lang="scss">

.card-header { display: flex; justify-content: space-between; align-items: center; }

.ai-compact-list { display: flex; flex-direction: column; gap: 8px; }
.ai-row {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border-radius: 6px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-left: 3px solid #0ACFFE;
  transition: all 0.2s;
  &:hover {
    border-color: #4FACFE;
    border-left-color: #4FACFE;
    box-shadow: 0 2px 10px rgba(10, 207, 254, 0.15);
  }
}
.ai-row-model { flex-wrap: wrap; }
.ai-col-name { width: 130px; flex-shrink: 0; }
.ai-col-url { flex: 1; min-width: 200px; }
.ai-col-key { width: 200px; flex-shrink: 0; }
.ai-col-pri { width: 90px; flex-shrink: 0; }
.ai-col-sel { width: 130px; flex-shrink: 0; }
.ai-col-model { flex: 1; min-width: 180px; }

.ai-caps-group { display: flex; gap: 6px; }
.ai-cap-tag {
  padding: 2px 12px; font-size: 12px; border-radius: 12px;
  cursor: pointer; user-select: none;
  background: var(--el-fill-color); color: var(--el-text-color-secondary);
  border: 1px solid var(--el-border-color);
  transition: all 0.25s;
  &:hover { transform: translateY(-1px); }
  &.active {
    color: #fff; border: none;
    background: linear-gradient(135deg, #0ACFFE 0%, #4FACFE 100%);
    box-shadow: 0 2px 8px rgba(10, 207, 254, 0.4);
  }
}

.ai-bar { margin-top: 10px; display: flex; gap: 8px; }
.empty-tip { text-align: center; color: var(--el-text-color-secondary); padding: 16px 0; font-size: 13px; }
</style>
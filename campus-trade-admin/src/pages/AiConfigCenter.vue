<template>
  <div class="admin-page">
    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 提示词模板 -->
      <el-tab-pane label="提示词模板" name="prompts">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-select v-model="promptCategory" placeholder="全部分类" clearable @change="loadPrompts" style="width: 160px">
                <el-option label="系统(system)" value="system" />
                <el-option label="审核(review)" value="review" />
                <el-option label="安全(safety)" value="safety" />
                <el-option label="降级(fallback)" value="fallback" />
              </el-select>
              <span class="count-tag">共 {{ prompts.length }} 条</span>
            </div>
          </template>
          <el-table :data="prompts" stripe v-loading="loading.prompts" @row-click="editPrompt">
            <el-table-column prop="templateKey" label="标识" width="200" />
            <el-table-column prop="templateName" label="名称" width="200" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column label="内容预览">
              <template #default="{ row }">
                <span class="content-preview">{{ row.content?.substring(0, 80) }}{{ row.content?.length > 80 ? '...' : '' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="configVersion" label="版本" width="80" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" @click.stop="editPrompt(row)">编辑</el-button>
                <el-button size="small" @click.stop="viewPromptVersions(row)">历史</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 安全规则 -->
      <el-tab-pane label="安全规则" name="safety">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-select v-model="safetyType" placeholder="全部类型" clearable @change="loadSafetyRules" style="width: 180px">
                <el-option label="注入防护(injection)" value="injection" />
                <el-option label="敏感词掩码(sensitive_mask)" value="sensitive_mask" />
                <el-option label="DSML过滤(dsml_filter)" value="dsml_filter" />
                <el-option label="违禁词(blocked_keyword)" value="blocked_keyword" />
              </el-select>
              <el-button type="primary" @click="safetyDialog = true; safetyForm = {}">新增规则</el-button>
              <span class="count-tag">共 {{ safetyRules.length }} 条</span>
            </div>
          </template>
          <el-table :data="safetyRules" stripe v-loading="loading.safety">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="ruleType" label="类型" width="150" />
            <el-table-column prop="rulePattern" label="匹配规则" show-overflow-tooltip />
            <el-table-column prop="ruleAction" label="动作" width="100" />
            <el-table-column prop="replacement" label="替换" width="100" />
            <el-table-column prop="sortOrder" label="排序" width="60" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="editSafetyRule(row)">编辑</el-button>
                <el-button size="small" :type="row.isActive === 1 ? 'warning' : 'success'" @click="toggleSafety(row)">{{ row.isActive === 1 ? '禁用' : '启用' }}</el-button>
                <el-button size="small" type="danger" @click="removeSafety(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 参数配置 -->
      <el-tab-pane label="参数配置" name="params">
        <el-card v-loading="loading.params">
          <div v-for="(items, group) in configParams" :key="group" class="param-group">
            <h4 class="group-title">{{ group }}</h4>
            <el-table :data="items" stripe>
              <el-table-column prop="configKey" label="参数名" width="250" />
              <el-table-column prop="configValue" label="当前值" width="200" />
              <el-table-column prop="configType" label="类型" width="80" />
              <el-table-column prop="description" label="说明" show-overflow-tooltip />
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button size="small" @click="editParam(row)">修改</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 快捷问题 -->
      <el-tab-pane label="快捷问题" name="questions">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-button type="primary" @click="questionDialog = true; questionForm = { isActive: 1, sortOrder: 0 }">新增问题</el-button>
              <span class="count-tag">共 {{ quickQuestions.length }} 条</span>
            </div>
          </template>
          <el-table :data="quickQuestions" stripe v-loading="loading.questions">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="question" label="问题内容" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="sortOrder" label="排序" width="60" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button size="small" @click="editQuestion(row)">编辑</el-button>
                <el-button size="small" :type="row.isActive === 1 ? 'warning' : 'success'" @click="toggleQuestion(row)">{{ row.isActive === 1 ? '禁用' : '启用' }}</el-button>
                <el-button size="small" type="danger" @click="removeQuestion(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 版本历史 -->
      <el-tab-pane label="版本历史" name="versions">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-select v-model="versionType" placeholder="配置类型" clearable style="width: 150px">
                <el-option label="提示词" value="prompt" />
                <el-option label="安全规则" value="safety" />
                <el-option label="参数" value="config" />
              </el-select>
              <el-input v-model="versionKey" placeholder="配置标识" clearable style="width: 200px" />
              <el-button type="primary" @click="loadVersions">查询</el-button>
            </div>
          </template>
          <el-table :data="versions" stripe v-loading="loading.versions">
            <el-table-column prop="configType" label="类型" width="100" />
            <el-table-column prop="configKey" label="标识" width="200" />
            <el-table-column prop="configVersion" label="版本" width="80" />
            <el-table-column label="快照预览">
              <template #default="{ row }">
                <span class="content-preview">{{ row.snapshot?.substring(0, 80) }}{{ row.snapshot?.length > 80 ? '...' : '' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="changeNote" label="变更说明" width="200" />
            <el-table-column prop="createdAt" label="时间" width="180" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="rollbackVersion(row)" v-if="row.configType === 'prompt'">回滚</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      <!-- 工具管理 -->
      <el-tab-pane label="工具管理" name="tools">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="count-tag">共 {{ tools.length }} 个工具</span>
            </div>
          </template>
          <el-table :data="tools" stripe v-loading="loading.tools">
            <el-table-column prop="toolName" label="工具名" width="200" />
            <el-table-column prop="displayName" label="显示名" width="150" />
            <el-table-column prop="toolGroup" label="分组" width="100" />
            <el-table-column prop="description" label="描述" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" :type="row.isActive === 1 ? 'warning' : 'success'" @click="toggleTool(row)">{{ row.isActive === 1 ? '禁用' : '启用' }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- Prompt预览/测试 -->
      <el-tab-pane label="预览/测试" name="preview">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-button type="primary" @click="loadPreview">刷新预览</el-button>
            </div>
          </template>
          <el-descriptions :column="3" border v-if="previewData">
            <el-descriptions-item label="模板数量">{{ previewData.templateCount }}</el-descriptions-item>
            <el-descriptions-item label="字符长度">{{ previewData.length }}</el-descriptions-item>
            <el-descriptions-item label="估算Token">{{ previewData.estimatedTokens }}</el-descriptions-item>
          </el-descriptions>
          <el-input v-if="previewData" :model-value="previewData.assembled" type="textarea" :rows="12" readonly style="margin-top: 12px" />
          <el-divider />
          <h4>在线测试</h4>
          <el-input v-model="testMessage" placeholder="输入测试消息" style="margin-bottom: 12px" />
          <el-button type="primary" @click="runTest" :loading="testing">发送测试</el-button>
          <el-input v-if="testResult" :model-value="testResult" type="textarea" :rows="8" readonly style="margin-top: 12px" />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 提示词编辑弹窗 -->
    <el-dialog v-model="promptDialog" :title="`编辑提示词: ${promptForm.templateKey || ''}`" width="70%" top="5vh">
      <el-form :model="promptForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="promptForm.templateName" disabled />
        </el-form-item>
        <el-form-item label="分类">
          <el-tag>{{ promptForm.category }}</el-tag>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="promptForm.content" type="textarea" :rows="18" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="promptForm.note" placeholder="本次修改说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promptDialog = false">取消</el-button>
        <el-button type="primary" @click="savePrompt" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 安全规则编辑弹窗 -->
    <el-dialog v-model="safetyDialog" :title="safetyForm.id ? '编辑安全规则' : '新增安全规则'" width="600px">
      <el-form :model="safetyForm" label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="safetyForm.ruleType" style="width: 100%">
            <el-option label="注入防护" value="injection" />
            <el-option label="敏感词掩码" value="sensitive_mask" />
            <el-option label="DSML过滤" value="dsml_filter" />
            <el-option label="违禁词" value="blocked_keyword" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则">
          <el-input v-model="safetyForm.rulePattern" placeholder="正则表达式或关键词" />
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="safetyForm.ruleAction" style="width: 100%">
            <el-option label="拦截" value="block" />
            <el-option label="替换" value="replace" />
            <el-option label="过滤" value="filter" />
          </el-select>
        </el-form-item>
        <el-form-item label="替换词">
          <el-input v-model="safetyForm.replacement" placeholder="替换为（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="safetyForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="safetyForm.description" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="safetyDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSafety" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 参数编辑弹窗 -->
    <el-dialog v-model="paramDialog" title="修改参数" width="500px">
      <el-form :model="paramForm" label-width="80px">
        <el-form-item label="参数">
          <el-input :model-value="`${paramForm.configGroup}/${paramForm.configKey}`" disabled />
        </el-form-item>
        <el-form-item label="说明">
          <el-input :model-value="paramForm.description" disabled />
        </el-form-item>
        <el-form-item label="新值">
          <el-input v-model="paramForm.configValue" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paramDialog = false">取消</el-button>
        <el-button type="primary" @click="saveParam" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 快捷问题编辑弹窗 -->
    <el-dialog v-model="questionDialog" :title="questionForm.id ? '编辑快捷问题' : '新增快捷问题'" width="500px">
      <el-form :model="questionForm" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="questionForm.question" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="questionForm.category" placeholder="如: 商品/订单/账户" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="questionForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialog = false">取消</el-button>
        <el-button type="primary" @click="saveQuestion" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getConfigPrompts, updateConfigPrompt, getPromptVersions, rollbackPrompt,
  getSafetyRules, addSafetyRule, updateSafetyRule, deleteSafetyRule, toggleSafetyRule,
  getConfigParams, updateConfigParam,
  getConfigQuickQuestions, addQuickQuestion, updateQuickQuestion, deleteQuickQuestion, toggleQuickQuestion,
  getConfigVersions,
  getConfigTools, toggleConfigTool,
  previewPrompt, testPrompt,
  compareVersions
} from '@/api/admin'

const activeTab = ref('prompts')
const saving = ref(false)

// 提示词
const prompts = ref<Record<string, any>[]>([])
const promptCategory = ref('')
const promptDialog = ref(false)
const promptForm = reactive<Record<string, any>>({})
const loading = reactive({ prompts: false, safety: false, params: false, questions: false, versions: false, tools: false })

const loadPrompts = async () => {
  loading.prompts = true
  try { prompts.value = await getConfigPrompts(promptCategory.value || undefined) }
  catch { ElMessage.error('加载失败') }
  finally { loading.prompts = false }
}

const editPrompt = (row: Record<string, any>) => {
  Object.assign(promptForm, row)
  promptForm.note = ''
  promptDialog.value = true
}

const savePrompt = async () => {
  saving.value = true
  try {
    await updateConfigPrompt(promptForm.templateKey, { content: promptForm.content, note: promptForm.note })
    ElMessage.success('保存成功')
    promptDialog.value = false
    loadPrompts()
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const viewPromptVersions = async (row: Record<string, any>) => {
  activeTab.value = 'versions'
  versionType.value = 'prompt'
  versionKey.value = row.templateKey
  loadVersions()
}

// 安全规则
const safetyRules = ref<Record<string, any>[]>([])
const safetyType = ref('')
const safetyDialog = ref(false)
const safetyForm = reactive<Record<string, any>>({})

const loadSafetyRules = async () => {
  loading.safety = true
  try { safetyRules.value = await getSafetyRules(safetyType.value || undefined) }
  catch { ElMessage.error('加载失败') }
  finally { loading.safety = false }
}

const editSafetyRule = (row: Record<string, any>) => {
  Object.assign(safetyForm, row)
  safetyDialog.value = true
}

const saveSafety = async () => {
  saving.value = true
  try {
    if (safetyForm.id) await updateSafetyRule(safetyForm.id, safetyForm)
    else await addSafetyRule(safetyForm)
    ElMessage.success('保存成功')
    safetyDialog.value = false
    loadSafetyRules()
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const toggleSafety = async (row: Record<string, any>) => {
  try {
    await toggleSafetyRule(row.id, row.isActive === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    loadSafetyRules()
  } catch { ElMessage.error('操作失败') }
}

const removeSafety = async (row: Record<string, any>) => {
  try {
    await ElMessageBox.confirm('确认删除此规则？', '提示', { type: 'warning' })
    await deleteSafetyRule(row.id)
    ElMessage.success('删除成功')
    loadSafetyRules()
  } catch {}
}

// 参数配置
const configParams = ref<Record<string, Record<string, any>[]>>({})
const paramDialog = ref(false)
const paramForm = reactive<Record<string, any>>({})

const loadParams = async () => {
  loading.params = true
  try { configParams.value = await getConfigParams() }
  catch { ElMessage.error('加载失败') }
  finally { loading.params = false }
}

const editParam = (row: Record<string, any>) => {
  Object.assign(paramForm, row)
  paramDialog.value = true
}

const saveParam = async () => {
  saving.value = true
  try {
    await updateConfigParam(paramForm.configGroup, paramForm.configKey, paramForm.configValue)
    ElMessage.success('保存成功')
    paramDialog.value = false
    loadParams()
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

// 快捷问题
const quickQuestions = ref<Record<string, any>[]>([])
const questionDialog = ref(false)
const questionForm = reactive<Record<string, any>>({})

const loadQuestions = async () => {
  loading.questions = true
  try { quickQuestions.value = await getConfigQuickQuestions() }
  catch { ElMessage.error('加载失败') }
  finally { loading.questions = false }
}

const editQuestion = (row: Record<string, any>) => {
  Object.assign(questionForm, row)
  questionDialog.value = true
}

const saveQuestion = async () => {
  saving.value = true
  try {
    if (questionForm.id) await updateQuickQuestion(questionForm.id, questionForm)
    else await addQuickQuestion(questionForm)
    ElMessage.success('保存成功')
    questionDialog.value = false
    loadQuestions()
  } catch { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const toggleQuestion = async (row: Record<string, any>) => {
  try {
    await toggleQuickQuestion(row.id, row.isActive === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    loadQuestions()
  } catch { ElMessage.error('操作失败') }
}

const removeQuestion = async (row: Record<string, any>) => {
  try {
    await ElMessageBox.confirm('确认删除此问题？', '提示', { type: 'warning' })
    await deleteQuickQuestion(row.id)
    ElMessage.success('删除成功')
    loadQuestions()
  } catch {}
}

// 版本历史
const versions = ref<Record<string, any>[]>([])
const versionType = ref('')
const versionKey = ref('')

const loadVersions = async () => {
  loading.versions = true
  try { versions.value = await getConfigVersions(versionType.value || undefined, versionKey.value || undefined) }
  catch { ElMessage.error('加载失败') }
  finally { loading.versions = false }
}

const rollbackVersion = async (row: Record<string, any>) => {
  try {
    await ElMessageBox.confirm(`确认回滚到版本 ${row.configVersion}？`, '提示', { type: 'warning' })
    await rollbackPrompt(row.configKey, row.configVersion)
    ElMessage.success('回滚成功')
  } catch {}
}

// 工具管理
const tools = ref<Record<string, any>[]>([])
const loadTools = async () => {
  loading.tools = true
  try { tools.value = await getConfigTools() }
  catch { ElMessage.error('加载失败') }
  finally { loading.tools = false }
}
const toggleTool = async (row: Record<string, any>) => {
  try { await toggleConfigTool(row.toolName, row.isActive === 1 ? 0 : 1); ElMessage.success('操作成功'); loadTools() }
  catch { ElMessage.error('操作失败') }
}

// Prompt预览/测试
const previewData = ref<Record<string, any> | null>(null)
const testMessage = ref('')
const testResult = ref('')
const testing = ref(false)
const loadPreview = async () => {
  try { previewData.value = await previewPrompt() }
  catch { ElMessage.error('预览失败') }
}
const runTest = async () => {
  if (!testMessage.value) return ElMessage.warning('请输入测试消息')
  testing.value = true
  try { const r = await testPrompt(testMessage.value); testResult.value = r.answer }
  catch { ElMessage.error('测试失败') }
  finally { testing.value = false }
}

onMounted(() => {
  loadPrompts()
  loadTools()
  loadPreview()
  loadSafetyRules()
  loadParams()
  loadQuestions()
})
</script>

<style scoped lang="scss">
.config-tabs {
  :deep(.el-tabs__header) { margin-bottom: 16px; }
}
.card-header {
  display: flex; align-items: center; gap: 12px;
  .count-tag { margin-left: auto; color: var(--el-text-color-secondary); font-size: 13px; }
}
.content-preview { color: var(--el-text-color-secondary); font-size: 13px; }
.param-group { margin-bottom: 24px; }
.group-title { margin: 0 0 12px 0; color: var(--el-text-color-primary); font-size: 15px; }
</style>
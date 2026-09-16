import request from '@/utils/request'
import axios from 'axios'
import type { PageResult, PageQueryParams, AdminUserVO, AdminGoodsVO, AdminOrderVO, AdminReportVO, OperationLogVO, SecurityLogVO, AdminLoginResult, DashboardStats, AdminInfoVO } from '@/types'

export type { DashboardStats } from '@/types'

export const adminLogin = (data: { username: string; password: string }) =>
  request.post<never, AdminLoginResult>('/auth/login', data)

export const getAdminInfo = () =>
  request.get<never, AdminInfoVO>('/admin/info')

export const updateAdminPassword = (data: { oldPassword: string; newPassword: string }) =>
  request.put<never, void>('/admin/password', data)

export const getUserList = (params: PageQueryParams) =>
  request.get<never, PageResult<AdminUserVO>>('/admin/user', { params })

export const banUser = (id: number, reason?: string) =>
  request.put<never, void>(`/admin/user/${id}/ban`, null, { params: { reason } })

export const unbanUser = (id: number) =>
  request.put<never, void>(`/admin/user/${id}/unban`)

export const getGoodsList = (params: PageQueryParams) =>
  request.get<never, PageResult<AdminGoodsVO>>('/admin/goods', { params })

export const auditGoods = (id: number, data: { status: string; rejectReason?: string }) =>
  request.put<never, void>(`/admin/goods/${id}/audit`, data)

export const getOrderList = (params: PageQueryParams) =>
  request.get<never, PageResult<AdminOrderVO>>('/admin/order', { params })

export const approveRefund = (id: number) =>
  request.put<never, void>(`/admin/order/${id}/approve-refund`)

export const rejectRefund = (id: number, reason?: string) =>
  request.put<never, void>(`/admin/order/${id}/reject-refund`, null, { params: { reason } })

export const getReportList = (params: PageQueryParams) =>
  request.get<never, PageResult<AdminReportVO>>('/admin/report', { params })

export const resolveReport = (id: number, reason?: string) =>
  request.put<never, void>(`/admin/report/${id}/resolve`, null, { params: { reason } })

export const dismissReport = (id: number, reason?: string) =>
  request.put<never, void>(`/admin/report/${id}/dismiss`, null, { params: { reason } })

export const getOperationLogs = (params: PageQueryParams) =>
  request.get<never, PageResult<OperationLogVO>>('/admin/log/operation', { params })

export const getSecurityLogs = (params: PageQueryParams) =>
  request.get<never, PageResult<SecurityLogVO>>('/admin/log/security', { params })

export const getDashboardStats = () =>
  request.get<never, DashboardStats>('/admin/dashboard/stats')

export interface CategoryVO {
  id: number
  categoryName: string
  parentId: number
  sortOrder: number
  icon: string
  status: number
  createTime: string
}

export const getCategoryList = () =>
  request.get<never, CategoryVO[]>('/goods-category')

export const createCategory = (data: { categoryName: string; sortOrder?: number; icon?: string; status?: number }) =>
  request.post<never, void>('/goods-category', data)

export const updateCategory = (id: number, data: { categoryName?: string; sortOrder?: number; icon?: string; status?: number }) =>
  request.put<never, void>(`/goods-category/${id}`, data)

export const deleteCategory = (id: number) =>
  request.delete<never, void>(`/goods-category/${id}`)

export interface AnnouncementVO {
  id: number
  title: string
  content: string
  type: number
  status: number
  sortOrder: number
  createTime: string
}

export const getAnnouncementList = (params: { pageNum: number; pageSize: number }) =>
  request.get<never, PageResult<AnnouncementVO>>('/announcement/list', { params })

export const createAnnouncement = (data: { title: string; content: string; type?: number; status?: number; sortOrder?: number }) =>
  request.post<never, void>('/announcement', data)

export const updateAnnouncement = (id: number, data: { title?: string; content?: string; type?: number; status?: number; sortOrder?: number }) =>
  request.put<never, void>(`/announcement/${id}`, data)

export const deleteAnnouncement = (id: number) =>
  request.delete<never, void>(`/announcement/${id}`)

export interface BannerVO {
  id: number
  title: string
  subtitle: string
  imageUrl: string
  linkUrl: string
  bgColor: string
  buttonText: string
  buttonColor: string
  sortOrder: number
  status: number
  createTime: string
}

export const getBannerList = (params: { pageNum: number; pageSize: number }) =>
  request.get<never, PageResult<BannerVO>>('/banner/list', { params })

export const createBanner = (data: Omit<BannerVO, 'id' | 'createTime'>) =>
  request.post<never, void>('/banner', data)

export const updateBanner = (id: number, data: Omit<BannerVO, 'id' | 'createTime'>) =>
  request.put<never, void>(`/banner/${id}`, data)

export const toggleBanner = (id: number) =>
  request.put<never, void>(`/banner/${id}/toggle`)

export const deleteBanner = (id: number) =>
  request.delete<never, void>(`/banner/${id}`)

export interface SystemConfigVO {
  id: number
  configKey: string
  configValue: string
  description: string
}

export interface FundLogVO {
  id: number
  orderId: number
  userId: number
  amount: number
  type: string
  status: string
  tradeNo: string | null
  remark: string | null
  createTime: string
}

export const getSystemConfig = () =>
  request.get<never, SystemConfigVO[]>('/admin/system-config')

export const updateSystemConfig = (configs: SystemConfigVO[]) =>
  request.put<never, void>('/admin/system-config', configs)

export const getAlipayStatus = () =>
  request.get<never, Record<string, unknown>>('/admin/alipay-status')

export const getEmailStatus = () =>
  request.get<never, Record<string, unknown>>('/admin/email-status')

export const getFundLogList = (params: PageQueryParams & { type?: string; orderId?: number }) =>
  request.get<never, PageResult<FundLogVO>>('/admin/fund-log', { params })

export interface AiConfigStatus {
  enabled: boolean
  healthy: boolean
  model: string
  apiKeyMasked: string
  baseUrl: string
  embApiKeyMasked?: string
  embBaseUrl?: string
  embModel?: string
  embeddingAvailable?: boolean
  routingEnabled?: boolean
  reasonerModel?: string
  visionModel?: string
  rateLimitPerMinute?: number
}

export const getAiConfigStatus = () =>
  request.get<never, AiConfigStatus>('/ai/config/status', { params: { _t: Date.now() } })

export const updateAiConfig = (data: {
  apiKey?: string
  model?: string
  baseUrl?: string
  embApiKey?: string
  embBaseUrl?: string
  embModel?: string
  routingEnabled?: string
  reasonerModel?: string
  visionModel?: string
}) =>
  request.put<never, AiConfigStatus>('/ai/config', data)

export const getAiSystemPrompt = () =>
  request.get<never, { prompt: string; isCustom: boolean; defaultPrompt: string }>('/ai/prompt')

export const updateAiSystemPrompt = (prompt: string) =>
  request.put<never, void>('/ai/prompt', { prompt })

export const resetAiSystemPrompt = () =>
  request.delete<never, void>('/ai/prompt')

export const suggestFaqs = () =>
  request.get<never, Array<{ question: string; answer: string; category: string }>>('/ai/faq/suggest')

export const getAiStats = () =>
  request.get<never, Record<string, unknown>>('/ai/stats')

export const getAiFeedbackStats = () =>
  request.get<never, Record<string, unknown>>('/ai/feedback/stats')

export interface AiChannel {
  id: string
  name: string
  baseUrl: string
  apiKey: string
  priority: number
  enabled: boolean
}

export interface AiModelReg {
  channelId: string
  model: string
  caps: string[]
}

export const getAiChannels = () =>
  request.get<never, string>('/ai/channels')

export const saveAiChannels = (channels: AiChannel[]) =>
  request.put<never, void>('/ai/channels', JSON.stringify(channels), { headers: { 'Content-Type': 'application/json' } })

export const getAiModels = () =>
  request.get<never, string>('/ai/models')

export const saveAiModels = (models: AiModelReg[]) =>
  request.put<never, void>('/ai/models', JSON.stringify(models), { headers: { 'Content-Type': 'application/json' } })

export const getAiTools = () =>
  request.get<never, Record<string, unknown>[]>('/ai/tools')

export const getAiFeedbackList = (params: { page?: number; size?: number; minRating?: number; maxRating?: number }) =>
  request.get<never, { list: Record<string, unknown>[]; total: number; page: number; size: number }>('/ai/feedback/list', { params })

export const getAiDashboard = () =>
  request.get<never, Record<string, any>>('/ai/dashboard')

export const exportRlhfData = (params: { offset?: number; limit?: number }) =>
  request.get<never, { data: Record<string, any>[]; count: number; offset: number; limit: number }>('/ai/feedback/rlhf-export', { params })

export const getAiFeedbackAnalysis = () =>
  request.get<never, Record<string, any>[]>('/ai/feedback/analysis')

export const getAiKnowledge = () =>
  request.get<never, Record<string, any>[]>('/ai/knowledge')

export const createAiKnowledge = (data: Record<string, any>) =>
  request.post<never, void>('/ai/knowledge', data)

export const updateAiKnowledge = (data: Record<string, any>) =>
  request.put<never, void>('/ai/knowledge', data)

export const deleteAiKnowledge = (id: number) =>
  request.delete<never, void>(`/ai/knowledge?id=${id}`)

export const getAiDocuments = () =>
  request.get<never, Record<string, any>[]>('/ai/document')

export const uploadAiDocument = (formData: FormData) =>
  request.post<never, void>('/ai/document/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })

export const getAiDocumentDetail = (id: number) =>
  request.get<never, Record<string, any>>(`/ai/document/${id}`)

export const deleteAiDocument = (id: number) =>
  request.delete<never, void>(`/ai/document/${id}`)

export const getAiHealth = async (): Promise<Record<string, unknown>> => {
  const adminStore = (await import('@/stores/admin')).useAdminStore()
  return axios.get('/actuator/health', {
    headers: adminStore.token ? { Authorization: `Bearer ${adminStore.token}` } : {}
  }).then(res => res.data)
}
// ===== AI配置中心 =====
export const getConfigPrompts = (category?: string) =>
  request.get('/ai/config/prompts', { params: category ? { category } : {} })

export const getConfigPrompt = (key: string) =>
  request.get<never, Record<string, any>>(`/ai/config/prompts/${key}`)

export const updateConfigPrompt = (key: string, data: { content: string; note?: string }) =>
  request.put(`/ai/config/prompts/${key}`, data)

export const getPromptVersions = (key: string) =>
  request.get<never, Record<string, any>[]>(`/ai/config/prompts/${key}/versions`)

export const rollbackPrompt = (key: string, ver: number) =>
  request.post(`/ai/config/prompts/${key}/rollback/${ver}`)

export const getSafetyRules = (ruleType?: string) =>
  request.get('/ai/config/safety-rules', { params: ruleType ? { ruleType } : {} })

export const addSafetyRule = (data: Record<string, any>) =>
  request.post('/ai/config/safety-rules', data)

export const updateSafetyRule = (id: number, data: Record<string, any>) =>
  request.put(`/ai/config/safety-rules/${id}`, data)

export const deleteSafetyRule = (id: number) =>
  request.delete(`/ai/config/safety-rules/${id}`)

export const toggleSafetyRule = (id: number, isActive: number) =>
  request.patch(`/ai/config/safety-rules/${id}/toggle`, { isActive })

export const getConfigParams = () =>
  request.get<never, Record<string, Record<string, any>[]>>('/ai/config/params')

export const updateConfigParam = (group: string, key: string, value: string) =>
  request.put(`/ai/config/params/${group}/${key}`, { value })

export const getConfigQuickQuestions = () =>
  request.get<never, Record<string, any>[]>('/ai/config/quick-questions')

export const addQuickQuestion = (data: Record<string, any>) =>
  request.post('/ai/config/quick-questions', data)

export const updateQuickQuestion = (id: number, data: Record<string, any>) =>
  request.put(`/ai/config/quick-questions/${id}`, data)

export const deleteQuickQuestion = (id: number) =>
  request.delete(`/ai/config/quick-questions/${id}`)

export const toggleQuickQuestion = (id: number, isActive: number) =>
  request.patch(`/ai/config/quick-questions/${id}/toggle`, { isActive })

export const getConfigVersions = (type?: string, key?: string) =>
  request.get('/ai/config/versions', { params: { type, key } })

export const getConfigTools = (group?: string) =>
  request.get('/ai/config/tools', { params: group ? { group } : {} })

export const updateConfigTool = (name: string, data: Record<string, any>) =>
  request.put(`/ai/config/tools/${name}`, data)

export const toggleConfigTool = (name: string, isActive: number) =>
  request.patch(`/ai/config/tools/${name}/toggle`, { isActive })

export const previewPrompt = () =>
  request.post<never, Record<string, any>>('/ai/config/prompts/preview')

export const testPrompt = (message: string) =>
  request.post<never, Record<string, any>>('/ai/config/prompts/test', { message })

export const compareVersions = (type: string, key: string, v1: number, v2: number) =>
  request.get<never, Record<string, any>>('/ai/config/versions/compare', { params: { type, key, v1, v2 } })

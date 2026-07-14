import request from '@/utils/request'
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

export const getFundLogList = (params: PageQueryParams & { type?: string; orderId?: number }) =>
  request.get<never, PageResult<FundLogVO>>('/admin/fund-log', { params })

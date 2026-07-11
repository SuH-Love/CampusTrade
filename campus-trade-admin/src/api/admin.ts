import request from '@/utils/request'
import type { PageResult, PageQueryParams, AdminUserVO, AdminGoodsVO, AdminOrderVO, AdminReportVO, OperationLogVO, SecurityLogVO, AdminLoginResult, DashboardStats, AdminInfoVO } from '@/types'

export type { DashboardStats } from '@/types'

export const adminLogin = (data: { username: string; password: string }) =>
  request.post<never, AdminLoginResult>('/auth/login', data)

export const getAdminInfo = () =>
  request.get<never, AdminInfoVO>('/admin/info')

export const getUserList = (params: PageQueryParams) =>
  request.get<never, PageResult<AdminUserVO>>('/admin/user', { params })

export const banUser = (id: number) =>
  request.put<never, void>(`/admin/user/${id}/ban`)

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

export const resolveReport = (id: number) =>
  request.put<never, void>(`/admin/report/${id}/resolve`)

export const dismissReport = (id: number) =>
  request.put<never, void>(`/admin/report/${id}/dismiss`)

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

export const createCategory = (data: { categoryName: string; sortOrder?: number; icon?: string }) =>
  request.post<never, void>('/goods-category', data)

export const updateCategory = (id: number, data: { categoryName?: string; sortOrder?: number; icon?: string; status?: number }) =>
  request.put<never, void>(`/goods-category/${id}`, data)

export const deleteCategory = (id: number) =>
  request.delete<never, void>(`/goods-category/${id}`)

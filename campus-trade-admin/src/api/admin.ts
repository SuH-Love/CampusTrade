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

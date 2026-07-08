import request from '@/utils/request'

interface PageResult<T> {
  list: T[]
  total: number
}

interface DashboardStats {
  userCount: number
  goodsCount: number
  orderCount: number
  pendingAudit: number
}

export const adminLogin = (data: { username: string; password: string }) =>
  request.post<any, any>('/auth/login', data)

export const getUserList = (params: any) =>
  request.get<any, PageResult<any>>('/admin/user', { params })

export const banUser = (id: number) =>
  request.put<any, void>(`/admin/user/${id}/ban`)

export const unbanUser = (id: number) =>
  request.put<any, void>(`/admin/user/${id}/unban`)

export const getGoodsList = (params: any) =>
  request.get<any, PageResult<any>>('/admin/goods', { params })

export const auditGoods = (id: number, data: { status: string; rejectReason?: string }) =>
  request.put<any, void>(`/admin/goods/${id}/audit`, data)

export const getOrderList = (params: any) =>
  request.get<any, PageResult<any>>('/admin/order', { params })

export const getReportList = (params: any) =>
  request.get<any, PageResult<any>>('/admin/report', { params })

export const resolveReport = (id: number) =>
  request.put<any, void>(`/admin/report/${id}/resolve`)

export const dismissReport = (id: number) =>
  request.put<any, void>(`/admin/report/${id}/dismiss`)

export const getOperationLogs = (params: any) =>
  request.get<any, PageResult<any>>('/admin/log/operation', { params })

export const getSecurityLogs = (params: any) =>
  request.get<any, PageResult<any>>('/admin/log/security', { params })

export const getDashboardStats = () =>
  request.get<any, DashboardStats>('/admin/dashboard/stats')

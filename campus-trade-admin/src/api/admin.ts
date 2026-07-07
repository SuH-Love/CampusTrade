import request from '@/utils/request'

export const adminLogin = (data: { username: string; password: string }) => request.post('/auth/login', data)

export const getUserList = (params: any) => request.get('/admin/user', { params })

export const banUser = (id: number) => request.put(`/admin/user/${id}/ban`)

export const unbanUser = (id: number) => request.put(`/admin/user/${id}/unban`)

export const getGoodsList = (params: any) => request.get('/admin/goods', { params })

export const auditGoods = (id: number, data: { status: string; rejectReason?: string }) =>
  request.put(`/admin/goods/${id}/audit`, data)

export const getOrderList = (params: any) => request.get('/admin/order', { params })

export const getReportList = (params: any) => request.get('/admin/report', { params })

export const resolveReport = (id: number) => request.put(`/admin/report/${id}/resolve`)

export const dismissReport = (id: number) => request.put(`/admin/report/${id}/dismiss`)

export const getOperationLogs = (params: any) => request.get('/admin/log/operation', { params })

export const getSecurityLogs = (params: any) => request.get('/admin/log/security', { params })

export const getDashboardStats = () => request.get('/admin/dashboard/stats')

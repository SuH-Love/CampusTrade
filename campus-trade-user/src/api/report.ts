import request from '@/utils/request'

export interface ReportVO {
  id: number
  reporterId: number
  reporterName: string
  targetType: number
  targetId: number
  reason: string
  description: string
  status: string
  handleResult: string
  createTime: string
}

export const createReport = (data: { targetType: number; targetId: number; reason: string; description?: string; images?: string }) =>
  request.post('/report', data)

export const listMyReports = (pageNum: number = 1, pageSize: number = 10) =>
  request.get<any, any>('/report/mine', { params: { pageNum, pageSize } })
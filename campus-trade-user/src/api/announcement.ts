import request from '@/utils/request'

export interface AnnouncementVO {
  id: number
  title: string
  content: string
  status: number
  sortOrder: number
  createTime: string
}

export const getActiveAnnouncements = () =>
  request.get<never, AnnouncementVO[]>('/announcement/active')
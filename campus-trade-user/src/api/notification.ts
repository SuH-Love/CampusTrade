import request from '@/utils/request'

export interface NotificationVO {
  id: number
  userId: number
  title: string
  content: string
  notificationType: string
  relatedId: number
  isRead: number
  createTime: string
}

export const listNotifications = (isRead?: number, pageNum: number = 1, pageSize: number = 10) =>
  request.get<any, any>('/notification', { params: { isRead, pageNum, pageSize } })

export const getUnreadCount = () => request.get<any, number>('/notification/unread-count')

export const markAsRead = (id: number) => request.put(`/notification/${id}/read`)

export const markAllAsRead = () => request.put('/notification/read-all')

export const deleteNotification = (id: number) => request.delete(`/notification/${id}`)
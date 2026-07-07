import request from '@/utils/request'

export interface ChatMessageVO {
  id: number
  senderId: number
  senderName: string
  senderAvatar: string
  receiverId: number
  content: string
  messageType: number
  isRead: number
  createTime: string
}

export const sendMessage = (data: { receiverId: number; content: string; messageType?: number }) =>
  request.post('/chat', data)

export const getHistory = (targetUserId: number, pageNum: number = 1, pageSize: number = 20) =>
  request.get<any, any>('/chat/history/' + targetUserId, { params: { pageNum, pageSize } })

export const getRecentContacts = () => request.get<any, any>('/chat/recent')

export const getUnreadCount = (senderId: number) => request.get<any, number>('/chat/unread/' + senderId)

export const markAsRead = (senderId: number) => request.put('/chat/read/' + senderId)
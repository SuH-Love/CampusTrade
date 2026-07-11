import request from '@/utils/request'
import type { PageResult, ContactVO } from '@/types'

export interface ChatMessageVO {
  id: number
  senderId: number
  senderName: string
  senderAvatar: string
  receiverId: number
  receiverName: string
  receiverAvatar: string
  content: string
  messageType: number
  isRead: number
  createTime: string
}

export const sendMessage = (data: { receiverId: number; content: string; messageType?: number }) =>
  request.post('/chat', data)

export const getHistory = (targetUserId: number, pageNum: number = 1, pageSize: number = 20) =>
  request.get<never, PageResult<ChatMessageVO>>('/chat/history/' + targetUserId, { params: { pageNum, pageSize } })

export const getRecentContacts = () => request.get<never, ContactVO[]>('/chat/recent')

export const getUnreadCount = (senderId: number) => request.get<never, number>('/chat/unread/' + senderId)

export const markAsRead = (senderId: number) => request.put('/chat/read/' + senderId)

export const getOnlineUsers = () => request.get<never, number[]>('/chat/online-users')

export const getTotalUnreadCount = () => request.get<never, number>('/chat/unread-total')

export const recallMessage = (messageId: number) => request.put(`/chat/recall/${messageId}`)

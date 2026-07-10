import request from '@/utils/request'
import type { UserUpdateParams } from '@/types'

export interface UserVO {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  avatar: string
  realName: string
  studentId: string
  realVerified: number
  status: number
  createTime: string
  followingCount?: number
  followersCount?: number
  goodsCount?: number
  soldCount?: number
  avgRating?: number
}

export const getUserInfo = () => request.get<never, UserVO>('/user/info')

export const updateUserInfo = (data: UserUpdateParams) => request.put<never, UserVO>('/user/info', data)

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  request.put('/user/password', data)

export const realNameVerify = (realName: string, studentId: string) =>
  request.post('/user/verify', null, { params: { realName, studentId } })

export const uploadAvatar = (fileUrl: string) => request.post('/user/avatar', null, { params: { fileUrl } })

export const getUserPublicInfo = (id: number) => request.get<never, UserVO>(`/user/${id}`)

export interface UserStatsVO {
  publishedGoods: number
  onlineGoods: number
  buyerOrders: number
  sellerOrders: number
  finishedOrders: number
}

export const getUserStats = () => request.get<never, UserStatsVO>('/user/stats')

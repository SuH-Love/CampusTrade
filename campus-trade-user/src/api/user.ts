import request from '@/utils/request'

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
}

export const getUserInfo = () => request.get<any, UserVO>('/user/info')

export const updateUserInfo = (data: any) => request.put<any, UserVO>('/user/info', data)

export const updatePassword = (data: { oldPassword: string; newPassword: string }) =>
  request.put('/user/password', data)

export const realNameVerify = (realName: string, studentId: string) =>
  request.post('/user/verify', null, { params: { realName, studentId } })

export const uploadAvatar = (fileUrl: string) => request.post('/user/avatar', null, { params: { fileUrl } })
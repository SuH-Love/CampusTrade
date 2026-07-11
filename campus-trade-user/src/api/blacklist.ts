import request from '@/utils/request'

export interface BlacklistItem {
  id: number
  blockedId: number
  blockedName: string
  blockedAvatar: string
  createTime: string
}

export const blockUser = (blockedId: number) => request.post(`/blacklist/${blockedId}`)
export const unblockUser = (blockedId: number) => request.delete(`/blacklist/${blockedId}`)
export const getBlacklist = () => request.get<never, BlacklistItem[]>('/blacklist')
export const isBlocked = (blockedId: number) => request.get<never, boolean>(`/blacklist/is-blocked/${blockedId}`)
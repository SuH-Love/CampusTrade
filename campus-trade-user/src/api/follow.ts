import request from '@/utils/request'

export const toggleFollow = (userId: number) => request.post(`/follow/${userId}`)
export const isFollowing = (userId: number) => request.get<never, boolean>(`/follow/is-following/${userId}`)
export const getFollowingList = (params: { pageNum: number; pageSize: number }) => request.get<never, any>('/follow/following', { params })
export const getFollowerList = (userId: number, params: { pageNum: number; pageSize: number }) => request.get<never, any>(`/follow/followers/${userId}`, { params })
export const getFollowCounts = (userId: number) => request.get<never, { following: number; followers: number }>(`/follow/counts/${userId}`)
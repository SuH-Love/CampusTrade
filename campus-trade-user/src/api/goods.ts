import request from '@/utils/request'

export interface GoodsVO {
  id: number
  userId: number
  username: string
  userAvatar: string
  categoryId: number
  categoryName: string
  title: string
  description: string
  price: number
  originalPrice: number
  coverImage: string
  images: string
  status: string
  viewCount: number
  favoriteCount: number
  isFavorited: boolean
  createTime: string
}

export interface PageResult<T> {
  list: T[]
  total: number
}

export const getGoodsList = (params: any) => request.get<any, PageResult<GoodsVO>>('/goods', { params })

export const getGoodsDetail = (id: number) => request.get<any, GoodsVO>(`/goods/${id}`)

export const createGoods = (data: any) => request.post<any, GoodsVO>('/goods', data)

export const updateGoods = (id: number, data: any) => request.put<any, GoodsVO>(`/goods/${id}`, data)

export const deleteGoods = (id: number) => request.delete(`/goods/${id}`)

export const getHotGoods = () => request.get<any, PageResult<GoodsVO>>('/goods/hot')

export const getRecommendGoods = () => request.get<any, PageResult<GoodsVO>>('/goods/recommend')

export const submitAudit = (id: number) => request.put(`/goods/${id}/submit`)

export const onlineGoods = (id: number) => request.put(`/goods/${id}/online`)

export const offlineGoods = (id: number) => request.put(`/goods/${id}/offline`)

export const favoriteGoods = (id: number) => request.post(`/goods/${id}/favorite`)

export const unfavoriteGoods = (id: number) => request.delete(`/goods/${id}/favorite`)

export const getFavoriteList = (params: any) => request.get<any, PageResult<GoodsVO>>('/goods/favorites', { params })
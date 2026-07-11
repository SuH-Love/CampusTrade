import request from '@/utils/request'
import type { PageResult, GoodsQueryParams, GoodsCreateParams } from '@/types'

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
  condition: string
  status: string
  viewCount: number
  favoriteCount: number
  stock: number
  isFavorited: boolean
  sellerRealVerified: number
  rejectReason: string | null
  createTime: string
}

export const getGoodsList = (params: GoodsQueryParams) => request.get<never, PageResult<GoodsVO>>('/goods', { params })

export const getGoodsDetail = (id: number) => request.get<never, GoodsVO>(`/goods/${id}`)

export const createGoods = (data: GoodsCreateParams) => request.post<never, GoodsVO>('/goods', data)

export const updateGoods = (id: number, data: Partial<GoodsCreateParams>) => request.put<never, GoodsVO>(`/goods/${id}`, data)

export const deleteGoods = (id: number) => request.delete(`/goods/${id}`)

export const getHotGoods = () => request.get<never, PageResult<GoodsVO>>('/goods/hot')

export const getRecommendGoods = () => request.get<never, PageResult<GoodsVO>>('/goods/recommend')

export const submitAudit = (id: number) => request.put(`/goods/${id}/submit`)

export const onlineGoods = (id: number) => request.put(`/goods/${id}/online`)

export const offlineGoods = (id: number) => request.put(`/goods/${id}/offline`)

export const favoriteGoods = (id: number) => request.post(`/goods/${id}/favorite`)

export const unfavoriteGoods = (id: number) => request.delete(`/goods/${id}/favorite`)

export const getFavoriteList = (params: GoodsQueryParams) => request.get<never, PageResult<GoodsVO>>('/goods/favorites', { params })

export const getMyGoods = (params: { pageNum: number; pageSize: number; status?: string }) =>
  request.get<never, PageResult<GoodsVO>>('/goods/mine', { params })

export const getHotKeywords = () => request.get<never, string[]>('/goods/hot-keywords')

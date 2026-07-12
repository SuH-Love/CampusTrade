import request from '@/utils/request'
import type { PageResult } from '@/types'

export interface RatingDistribution {
  distribution: Record<string, number>
  totalCount: number
  avgRating: number
}

export interface SellerRatingVO {
  id: number
  orderId: number
  buyerId: number
  buyerName: string
  buyerAvatar: string
  sellerId: number
  rating: number
  comment: string
  goodsTitle: string
  createTime: string
  images?: string
}

export const rateSeller = (data: { orderId: number; sellerId: number; rating: number; comment?: string }) => request.post('/rating', data)
export const getAverageRating = (sellerId: number) => request.get<never, number>(`/rating/average/${sellerId}`)
export const getRatingList = (sellerId: number, params: { pageNum: number; pageSize: number }) =>
  request.get<never, PageResult<SellerRatingVO>>(`/rating/list/${sellerId}`, { params })
export const getRatingDistribution = (sellerId: number) =>
  request.get<never, RatingDistribution>(`/rating/distribution/${sellerId}`)

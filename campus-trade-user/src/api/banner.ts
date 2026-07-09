import request from '@/utils/request'

export interface BannerVO {
  id: number
  title: string
  subtitle: string
  imageUrl: string
  linkUrl: string
  bgColor: string
  sortOrder: number
  status: number
  createTime: string
}

export const getActiveBanners = () => request.get<never, BannerVO[]>('/banner/active')
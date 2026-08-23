import request from '@/utils/request'

export interface GoodsCategory {
  id: number
  categoryName: string
  parentId: number
  sortOrder: number
  icon: string
  goodsCount?: number
}

export const getCategoryList = () => request.get<never, GoodsCategory[]>('/goods-category')
export const listCategories = getCategoryList

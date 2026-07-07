import request from '@/utils/request'

export interface GoodsCategory {
  id: number
  categoryName: string
  parentId: number
  sortOrder: number
  icon: string
}

export const getCategoryList = () => request.get<any, GoodsCategory[]>('/goods-category')
export const listCategories = getCategoryList
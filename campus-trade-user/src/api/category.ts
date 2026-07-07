import request from '@/utils/request'

export interface GoodsCategory {
  id: number
  categoryName: string
  parentId: number
  sortOrder: number
  icon: string
}

export const listCategories = () => request.get<any, GoodsCategory[]>('/goods-category')
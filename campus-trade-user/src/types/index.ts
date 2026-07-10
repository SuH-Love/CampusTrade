export interface PageResult<T> {
  list: T[]
  total: number
}

export interface PageQueryParams {
  pageNum: number
  pageSize: number
  [key: string]: unknown
}

export interface GoodsQueryParams extends PageQueryParams {
  keyword?: string
  categoryId?: number
  status?: string
  sortBy?: string
}

export interface GoodsCreateParams {
  title: string
  categoryId: number
  description: string
  price: number
  originalPrice?: number
  coverImage?: string
  images?: string
  condition?: string
  stock?: number
}

export interface UserUpdateParams {
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
}

export interface OrderQueryParams extends PageQueryParams {
  status?: string
}

export interface UploadResponse {
  code: number
  data: string
  message?: string
}

export interface ContactVO {
  senderId: number
  receiverId: number
  senderName: string
  senderAvatar: string
  receiverName: string
  receiverAvatar: string
  content: string
}
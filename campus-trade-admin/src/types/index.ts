export interface PageResult<T> {
  list: T[]
  total: number
}

export interface PageQueryParams {
  pageNum: number
  pageSize: number
  [key: string]: unknown
}

export interface AdminUserVO {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  avatar: string
  realName: string
  studentId: string
  realVerified: number
  status: number
  createTime: string
}

export interface AdminGoodsVO {
  id: number
  userId: number
  username: string
  categoryId: number
  categoryName: string
  title: string
  description: string
  price: number
  originalPrice: number
  coverImage: string
  images: string
  status: string
  rejectReason: string
  viewCount: number
  favoriteCount: number
  createTime: string
}

export interface AdminOrderVO {
  id: number
  orderNo: string
  buyerId: number
  buyerName: string
  sellerId: number
  sellerName: string
  totalAmount: number
  status: string
  deliveryMethod: string
  remark: string
  createTime: string
}

export interface AdminReportVO {
  id: number
  reporterId: number
  reporterName: string
  targetType: number
  targetId: number
  reason: string
  description: string
  evidenceImages: string
  status: string
  handleResult: string
  createTime: string
}

export interface OperationLogVO {
  id: number
  username: string
  operation: string
  module: string
  ip: string
  detail: string
  createTime: string
}

export interface SecurityLogVO {
  id: number
  eventType: string
  username: string
  ip: string
  detail: string
  createTime: string
}

export interface AdminLoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface DashboardStats {
  userCount: number
  goodsCount: number
  orderCount: number
  pendingAudit: number
  goodsStatusMap?: Record<string, number>
  orderStatusMap?: Record<string, number>
  todayActive: number
  todayNewUsers: number
  todayOrders: number
  bannedUsers: number
}

export interface AdminInfoVO {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}
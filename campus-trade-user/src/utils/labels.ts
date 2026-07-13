export const moduleLabel = (mod: string): string => {
  const map: Record<string, string> = {
    Auth: '认证', User: '用户', Goods: '商品', GoodsCategory: '商品分类',
    Order: '订单', Report: '举报', Notification: '通知', Chat: '聊天',
    StompChat: '聊天', FileUpload: '文件上传', Admin: '管理',
    UserFollow: '关注', DeliveryAddress: '收货地址', Cart: '购物车',
    SellerRating: '商家评价', NotificationPreference: '通知偏好',
    Banner: '横幅', OrderItem: '订单项', GoodsFavorite: '商品收藏',
    OperationLog: '操作日志', SecurityLog: '安全日志',
    Announcement: '系统公告', UserBlacklist: '黑名单'
  }
  return map[mod] || mod
}

export const operationLabel = (op: string): string => {
  const map: Record<string, string> = {
    register: '注册', login: '登录', logout: '退出', refreshToken: '刷新令牌',
    getUserInfo: '获取用户信息', getUserPublicInfo: '获取公开信息', updateUserInfo: '更新用户信息',
    updatePassword: '修改密码', realNameVerify: '实名认证', uploadAvatar: '上传头像', getUserStats: '用户统计',
    getAdminInfo: '获取管理员信息', dashboardStats: '仪表盘统计', banUser: '封禁用户', unbanUser: '解封用户',
    listUsers: '用户列表', listOrders: '订单列表', listReports: '举报列表',
    listOperationLogs: '操作日志', listSecurityLogs: '安全日志',
    resolveReport: '通过举报', dismissReport: '驳回举报', rejectRefund: '拒绝退款',
    createGoods: '发布商品', updateGoods: '编辑商品', deleteGoods: '删除商品', getGoodsDetail: '商品详情',
    listGoods: '商品列表', hotGoods: '热门商品', recommendGoods: '推荐商品',
    submitAudit: '提交审核', auditGoods: '审核商品', onlineGoods: '上架商品', offlineGoods: '下架商品',
    favoriteGoods: '收藏商品', unfavoriteGoods: '取消收藏', listFavorites: '收藏列表', listMyGoods: '我的商品',
    createOrder: '创建订单', cancelOrder: '取消订单', payOrder: '支付订单',
    shipOrder: '发货', finishOrder: '确认收货', refundOrder: '退款',
    approveRefund: '同意退款', modifyPrice: '修改价格', getOrderDetail: '订单详情',
    listBuyerOrders: '买家订单', listSellerOrders: '卖家订单', rateOrder: '评价订单',
    listCart: '购物车列表', addToCart: '加入购物车', updateQuantity: '修改数量',
    removeFromCart: '移出购物车', clearCart: '清空购物车',
    sendMessage: '发送消息', getHistory: '聊天记录', getRecentContacts: '最近联系人',
    getUnreadCount: '未读消息数', markAsRead: '标记已读',
    getOnlineUsers: '在线用户', getTotalUnreadCount: '总未读数',
    markAllAsRead: '全部已读', deleteNotification: '删除通知', listNotifications: '通知列表',
    getMyPreferences: '通知偏好', setPreference: '设置偏好',
    list: '地址列表', getById: '地址详情', add: '新增地址', update: '修改地址',
    delete: '删除地址', setDefault: '设为默认',
    createReport: '提交举报', handleReport: '处理举报', listMyReports: '我的举报',
    listActiveBanners: '活跃轮播图', listAllBanners: '轮播图管理列表',
    createBanner: '创建轮播图', updateBanner: '编辑轮播图',
    deleteBanner: '删除轮播图', toggleBannerStatus: '切换轮播图状态',
    listAll: '分类列表', deleteImage: '删除图片', uploadImage: '上传图片',
    toggleFollow: '关注/取关', isFollowing: '是否关注', getFollowCounts: '关注数',
    listFollowing: '关注列表', listFollowers: '粉丝列表',
    getAverageRating: '卖家评分', getRatingList: '评价列表', getRatingDistribution: '评分分布',
    hotKeywords: '热门搜索词',
    blockUser: '屏蔽用户', unblockUser: '取消屏蔽', getBlacklist: '黑名单列表', isBlocked: '是否已屏蔽',
    getActiveAnnouncements: '获取公告', listAnnouncements: '公告列表', createAnnouncement: '创建公告', updateAnnouncement: '编辑公告', deleteAnnouncement: '删除公告',
    exportUsers: '导出用户CSV', exportOrders: '导出订单CSV',
    recallMessage: '撤回消息', resetPassword: '重置密码',
    createCategory: '创建分类', updateCategory: '编辑分类', deleteCategory: '删除分类'
  }
  return map[op] || op
}

export const eventTypeLabel = (type: string): string => {
  const map: Record<string, string> = {
    LOGIN_FAIL: '登录失败', LOGIN_SUCCESS: '登录成功', ACCESS_DENIED: '访问拒绝',
    TOKEN_EXPIRED: 'Token过期', RATE_LIMIT: '频率限制', MALICIOUS_INPUT: '恶意输入',
    LOGOUT: '退出登录', REGISTER: '注册'
  }
  return map[type] || type
}

export const orderStatusLabel = (status: string): string => {
  const map: Record<string, string> = {
    PENDING_PAYMENT: '待支付', PENDING_PAY: '待支付', PENDING_SHIPMENT: '待发货',
    PAID: '已支付', SHIPPED: '已发货', SHIPPING: '配送中',
    PENDING_REVIEW: '待评价', FINISHED: '已完成', CANCELLED: '已取消',
    REFUNDING: '退款中', REFUND: '退款', REFUNDED: '已退款'
  }
  return map[status] || status
}

export const goodsStatusLabel = (status: string): string => {
  const map: Record<string, string> = {
    DRAFT: '草稿', PENDING: '待审核', PENDING_REVIEW: '待审核', APPROVED: '已审核',
    ONLINE: '在售', OFFLINE: '已下架', SOLD: '已售出', REJECTED: '审核拒绝'
  }
  return map[status] || status
}

export const reportStatusLabel = (status: string): string => {
  const map: Record<string, string> = {
    PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已通过',
    DISMISSED: '已驳回', FINISHED: '已完成'
  }
  return map[status] || status
}

export const deliveryMethodLabel = (method: number | string): string => {
  if (method === 1 || method === 'DELIVERY') return '快递配送'
  if (method === 0 || method === 'PICKUP') return '线下自提'
  return String(method)
}

export function formatPrice(price: number): string {
  return '¥' + price.toFixed(2)
}

export function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(' ', 'T'))
  if (isNaN(d.getTime())) return dateStr
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, '0'), day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export const orderStatusTagType = (status: string): string => {
  const map: Record<string, string> = {
    PENDING_PAY: 'warning', PAID: 'primary', SHIPPING: '',
    PENDING_REVIEW: 'warning', FINISHED: 'success', CANCELLED: 'info', REFUND: 'danger'
  }
  return map[status] || ''
}

export const goodsStatusTagType = (status: string): string => {
  const map: Record<string, string> = {
    DRAFT: 'info', PENDING: 'warning', PENDING_REVIEW: 'warning', APPROVED: 'success',
    REJECTED: 'danger', ONLINE: '', OFFLINE: 'info', SOLD: 'success'
  }
  return map[status] || 'info'
}

export function debounce<T extends (...args: unknown[]) => void>(fn: T, delay: number): T {
  let timer: ReturnType<typeof setTimeout>
  return ((...args: unknown[]) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }) as T
}
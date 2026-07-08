import request from '@/utils/request'
import type { PageResult, OrderQueryParams } from '@/types'

export interface OrderItemVO {
  id: number
  orderId: number
  goodsId: number
  goodsTitle: string
  goodsImage: string | null
  price: number
}

export interface OrderVO {
  id: number
  orderNo: string
  buyerId: number
  buyerName: string
  sellerId: number
  sellerName: string
  totalAmount: number
  status: string
  remark: string
  payTime: string | null
  shipTime: string | null
  finishTime: string | null
  cancelTime: string | null
  cancelReason: string | null
  createTime: string
  items: OrderItemVO[]
}

export const createOrder = (data: { goodsId: number; remark?: string }) =>
  request.post<never, OrderVO>('/order', data)

export const cancelOrder = (id: number, reason?: string) =>
  request.put(`/order/${id}/cancel`, null, { params: { reason } })

export const payOrder = (id: number) => request.put(`/order/${id}/pay`)

export const shipOrder = (id: number) => request.put(`/order/${id}/ship`)

export const finishOrder = (id: number) => request.put(`/order/${id}/finish`)

export const refundOrder = (id: number, reason?: string) =>
  request.put(`/order/${id}/refund`, null, { params: { reason } })

export const getOrderDetail = (id: number) => request.get<never, OrderVO>(`/order/${id}`)

export const getBuyerOrders = (params: OrderQueryParams) => request.get<never, PageResult<OrderVO>>('/order/buyer', { params })

export const getSellerOrders = (params: OrderQueryParams) => request.get<never, PageResult<OrderVO>>('/order/seller', { params })

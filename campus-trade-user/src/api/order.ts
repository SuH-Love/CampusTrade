import request from '@/utils/request'
import type { PageResult, OrderQueryParams } from '@/types'

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
  createTime: string
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

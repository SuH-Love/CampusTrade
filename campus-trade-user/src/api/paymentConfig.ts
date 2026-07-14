import request from '@/utils/request'

export interface PaymentConfigVO {
  id: number
  userId: number
  paymentType: string
  alipayAccount: string
  realName: string
  isDefault: number
  status: string
  createTime: string
}

export const getPaymentConfigs = () => request.get<never, PaymentConfigVO[]>('/payment-config')

export const getDefaultPaymentConfig = () => request.get<never, PaymentConfigVO>('/payment-config/default')

export const createPaymentConfig = (alipayAccount: string, realName: string, isDefault?: number) =>
  request.post<never, PaymentConfigVO>('/payment-config', null, { params: { alipayAccount, realName, isDefault: isDefault ?? 0 } })

export const updatePaymentConfig = (id: number, alipayAccount?: string, realName?: string, isDefault?: number) =>
  request.put<never, PaymentConfigVO>(`/payment-config/${id}`, null, { params: { alipayAccount, realName, isDefault } })

export const deletePaymentConfig = (id: number) => request.delete(`/payment-config/${id}`)

export const setDefaultPaymentConfig = (id: number) => request.put(`/payment-config/${id}/default`)
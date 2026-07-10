import request from '@/utils/request'

export interface DeliveryAddressVO {
  id: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string | null
  city: string | null
  district: string | null
  detailAddress: string
  isDefault: number
}

export const getAddressList = () => request.get<never, DeliveryAddressVO[]>('/address')
export const getAddressById = (id: number) => request.get<never, DeliveryAddressVO>(`/address/${id}`)
export const addAddress = (data: Partial<DeliveryAddressVO>) => request.post('/address', data)
export const updateAddress = (id: number, data: Partial<DeliveryAddressVO>) => request.put(`/address/${id}`, data)
export const deleteAddress = (id: number) => request.delete(`/address/${id}`)
export const setDefaultAddress = (id: number) => request.put(`/address/${id}/default`)
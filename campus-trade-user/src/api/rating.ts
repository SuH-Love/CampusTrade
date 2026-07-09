import request from '@/utils/request'

export const rateSeller = (data: { orderId: number; sellerId: number; rating: number; comment?: string }) => request.post('/rating', data)
export const getAverageRating = (sellerId: number) => request.get<never, number>(`/rating/average/${sellerId}`)
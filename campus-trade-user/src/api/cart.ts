import request from '@/utils/request'

export interface CartVO {
  id: number
  goodsId: number
  title: string
  coverImage: string
  price: number
  quantity: number
  stock: number
  status: string
  sellerId: number
}

export const getCartList = () => request.get<never, CartVO[]>('/cart')
export const addToCart = (goodsId: number) => request.post(`/cart/${goodsId}`)
export const updateCartQuantity = (id: number, quantity: number) => request.put(`/cart/${id}`, null, { params: { quantity } })
export const removeFromCart = (id: number) => request.delete(`/cart/${id}`)
export const clearCart = () => request.delete('/cart')
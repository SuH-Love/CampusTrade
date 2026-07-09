import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCartList } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  const cartCount = ref(0)
  const fetchCartCount = async () => {
    try { const list = await getCartList(); cartCount.value = list ? list.length : 0 } catch { /* */ }
  }
  return { cartCount, fetchCartCount }
})
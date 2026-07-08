import { defineStore } from 'pinia'
import { ref } from 'vue'
import { adminLogin as loginApi } from '@/api/admin'

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const refreshToken = ref<string>(localStorage.getItem('admin_refresh_token') || '')
  const username = ref<string>('')

  const setAuth = (accessToken: string, refresh: string) => {
    token.value = accessToken
    refreshToken.value = refresh
    localStorage.setItem('admin_token', accessToken)
    localStorage.setItem('admin_refresh_token', refresh)
  }

  const clearAuth = () => {
    token.value = ''
    refreshToken.value = ''
    username.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_refresh_token')
  }

  const login = async (params: { username: string; password: string }) => {
    const data = await loginApi(params)
    setAuth(data.accessToken, data.refreshToken)
    username.value = params.username
  }

  const logout = () => {
    clearAuth()
  }

  return { token, refreshToken, username, login, logout, clearAuth, setAuth }
})
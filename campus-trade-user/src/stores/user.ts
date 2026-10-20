import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, logout as logoutApi, refreshToken as refreshTokenApi } from '@/api/auth'
import type { LoginParams, RegisterParams, TokenVO } from '@/api/auth'
import { getUserInfo } from '@/api/user'
import type { UserVO } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserVO | null>(null)
  let refreshTimer: ReturnType<typeof setTimeout> | null = null

  const scheduleRefresh = (expiresIn: number) => {
    if (refreshTimer) clearTimeout(refreshTimer)
    const delay = Math.max((expiresIn - 300) * 1000, 60 * 1000)
    refreshTimer = setTimeout(async () => {
      if (!refreshToken.value) return
      try {
        const data = await refreshTokenApi(refreshToken.value)
        setAuth(data)
      } catch { /* 401拦截器会处理 */ }
    }, delay)
  }

  const setAuth = (data: TokenVO) => {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    if (data.expiresIn) scheduleRefresh(data.expiresIn)
  }

  const clearAuth = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    if (refreshTimer) { clearTimeout(refreshTimer); refreshTimer = null }
  }

  const login = async (params: LoginParams) => {
    const data = await loginApi(params)
    setAuth(data)
    await fetchUserInfo()
  }

  const register = async (params: RegisterParams) => {
    await registerApi(params)
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch { /* ignore */ }
    clearAuth()
    window.location.href = '/login'
  }

  const fetchUserInfo = async () => {
    const data = await getUserInfo()
    userInfo.value = data
  }

  return { token, refreshToken, userInfo, login, register, logout, clearAuth, fetchUserInfo, setAuth }
})
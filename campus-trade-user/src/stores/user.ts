import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import type { LoginParams, RegisterParams, TokenVO } from '@/api/auth'
import { getUserInfo } from '@/api/user'
import type { UserVO } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserVO | null>(null)

  const setAuth = (data: TokenVO) => {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    localStorage.setItem('token', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
  }

  const clearAuth = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  const login = async (params: LoginParams) => {
    const data = await loginApi(params)
    setAuth(data)
    await fetchUserInfo()
  }

  const register = async (params: RegisterParams) => {
    await registerApi(params)
  }

  const logout = () => {
    clearAuth()
    window.location.href = '/login'
  }

  const fetchUserInfo = async () => {
    const data = await getUserInfo()
    userInfo.value = data
  }

  return { token, refreshToken, userInfo, login, register, logout, clearAuth, fetchUserInfo, setAuth }
})
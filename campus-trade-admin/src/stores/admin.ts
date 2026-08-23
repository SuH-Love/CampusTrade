import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminLogin as loginApi, getAdminInfo } from '@/api/admin'

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const refreshToken = ref<string>(localStorage.getItem('admin_refresh_token') || '')
  const username = ref<string>('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const avatar = ref<string>('')
  const nickname = ref<string>('')

  const isSuperAdmin = computed(() => roles.value.includes('ROLE_SUPER_ADMIN'))

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
    roles.value = []
    permissions.value = []
    avatar.value = ''
    nickname.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_refresh_token')
  }

  const login = async (params: { username: string; password: string }) => {
    const data = await loginApi(params)
    setAuth(data.accessToken, data.refreshToken)
    username.value = params.username
    await fetchAdminInfo()
  }

  const fetchAdminInfo = async () => {
    try {
      const data = await getAdminInfo()
      username.value = data.username
      nickname.value = data.nickname
      avatar.value = data.avatar
      roles.value = data.roles || []
      permissions.value = data.permissions || []
    } catch {
      clearAuth()
    }
  }

  const hasPermission = (perm: string | string[]) => {
    if (isSuperAdmin.value) return true
    if (Array.isArray(perm)) {
      return perm.some(p => permissions.value.includes(p))
    }
    return permissions.value.includes(perm)
  }

  const hasRole = (role: string | string[]) => {
    if (Array.isArray(role)) {
      return role.some(r => roles.value.includes(r))
    }
    return roles.value.includes(role)
  }

  const logout = () => {
    clearAuth()
  }

  return {
    token, refreshToken, username, roles, permissions, avatar, nickname,
    isSuperAdmin,
    login, logout, clearAuth, setAuth, fetchAdminInfo, hasPermission, hasRole
  }
})

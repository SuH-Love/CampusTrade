import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { adminLogin as loginApi, getAdminInfo } from '@/api/admin'
import axios from 'axios'

const getExpiresFromToken = (t: string): number => {
  try {
    const payload = JSON.parse(atob(t.split('.')[1]))
    if (payload.exp) return payload.exp - Math.floor(Date.now() / 1000)
  } catch {}
  return 0
}

export const useAdminStore = defineStore('admin', () => {
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const refreshToken = ref<string>(localStorage.getItem('admin_refresh_token') || '')
  const id = ref<number>(0)
  const username = ref<string>('')
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const avatar = ref<string>('')
  const nickname = ref<string>('')
  let refreshTimer: ReturnType<typeof setTimeout> | null = null

  const isSuperAdmin = computed(() => roles.value.includes('ROLE_SUPER_ADMIN'))

  const scheduleRefresh = (expiresIn: number) => {
    if (refreshTimer) clearTimeout(refreshTimer)
    const delay = Math.max((expiresIn - 300) * 1000, 60 * 1000)
    refreshTimer = setTimeout(async () => {
      if (!refreshToken.value) return
      try {
        const res = await axios.post('/api/auth/refresh', { refreshToken: refreshToken.value })
        if (res.data.code === 200 && res.data.data) {
          setAuth(res.data.data.accessToken, res.data.data.refreshToken, res.data.data.expiresIn)
        }
      } catch { /* 401拦截器会处理 */ }
    }, delay)
  }

  const setAuth = (accessToken: string, refresh: string, expiresIn?: number) => {
    token.value = accessToken
    refreshToken.value = refresh
    localStorage.setItem('admin_token', accessToken)
    localStorage.setItem('admin_refresh_token', refresh)
    if (expiresIn) scheduleRefresh(expiresIn)
  }

  const clearAuth = () => {
    token.value = ''
    refreshToken.value = ''
    id.value = 0
    username.value = ''
    roles.value = []
    permissions.value = []
    avatar.value = ''
    nickname.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_refresh_token')
    if (refreshTimer) { clearTimeout(refreshTimer); refreshTimer = null }
  }

  if (token.value) {
    const remaining = getExpiresFromToken(token.value)
    if (remaining > 300) scheduleRefresh(remaining)
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
      if (e.key === 'admin_token' && e.newValue && e.newValue !== token.value) {
        token.value = e.newValue
        const remaining = getExpiresFromToken(e.newValue)
        if (remaining > 300) scheduleRefresh(remaining)
      }
      if (e.key === 'admin_refresh_token' && e.newValue) {
        refreshToken.value = e.newValue
      }
    })
  }

  const login = async (params: { username: string; password: string }) => {
    const data = await loginApi(params)
    setAuth(data.accessToken, data.refreshToken, data.expiresIn)
    username.value = params.username
    await fetchAdminInfo()
  }

  const fetchAdminInfo = async () => {
    try {
      const data = await getAdminInfo()
      id.value = data.id
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
    token, refreshToken, id, username, roles, permissions, avatar, nickname,
    isSuperAdmin,
    login, logout, clearAuth, setAuth, fetchAdminInfo, hasPermission, hasRole
  }
})

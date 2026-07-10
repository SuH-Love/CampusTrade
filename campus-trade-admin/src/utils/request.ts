import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import router from '@/router'

const service: AxiosInstance = axios.create({ baseURL: '/api', timeout: 15000 })

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

const processPendingRequests = (token: string) => {
  pendingRequests.forEach(cb => cb(token))
  pendingRequests = []
}

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const adminStore = useAdminStore()
    const noAuthUrls = ['/auth/login', '/auth/refresh']
    const isNoAuth = noAuthUrls.some(url => config.url?.includes(url))
    if (adminStore.token && !isNoAuth) {
      config.headers.Authorization = `Bearer ${adminStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }
    if (error.response) {
      const { status } = error.response
      if (status === 401 && !originalRequest._retry) {
        const adminStore = useAdminStore()
        if (!adminStore.refreshToken) {
          adminStore.clearAuth()
          router.push('/login')
          ElMessage.error('请先登录')
          return Promise.reject(error)
        }
        if (isRefreshing) {
          return new Promise((resolve) => {
            pendingRequests.push((token: string) => {
              originalRequest.headers = originalRequest.headers || {}
              originalRequest.headers.Authorization = `Bearer ${token}`
              originalRequest._retry = true
              resolve(service(originalRequest))
            })
          })
        }
        isRefreshing = true
        originalRequest._retry = true
        try {
          const res = await axios.post('/api/auth/refresh', { refreshToken: adminStore.refreshToken })
          const { accessToken, refreshToken: newRefresh } = res.data.data
          adminStore.setAuth(accessToken, newRefresh)
          processPendingRequests(accessToken)
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return service(originalRequest)
        } catch {
          adminStore.clearAuth()
          router.push('/login')
          ElMessage.error('登录已过期')
          pendingRequests = []
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      } else if (status === 403) {
        ElMessage.error('无权限')
      } else if (status === 429) {
        ElMessage.error('请求过于频繁')
      } else {
        ElMessage.error(error.response.data?.message || '系统异常')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service

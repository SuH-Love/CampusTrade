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

const MAX_NETWORK_RETRY = 3
const RETRY_DELAY = 500

const isNetworkError = (error: unknown): boolean => {
  const err = error as { response?: unknown; code?: string; message?: string }
  return !err.response && (
    err.code === 'ERR_NETWORK' ||
    err.code === 'ECONNABORTED' ||
    err.code === 'ECONNRESET' ||
    (!!err.message && err.message.includes('Network Error')) ||
    (!!err.message && err.message.includes('timeout')) ||
    (!!err.message && err.message.includes('ERR_CONNECTION_RESET'))
  )
}

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

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
    if (response.config.responseType === 'blob') return response.data
    const { code, message, data } = response.data
    if (code === 200) return data
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean; _networkRetryCount?: number }
    if (error.response) {
      const { status } = error.response
      if (status === 401 && !originalRequest._retry) {
        const adminStore = useAdminStore()
        const respMsg = error.response.data?.message || ''
        if (respMsg.includes('其他设备')) {
          adminStore.clearAuth()
          router.push('/login')
          ElMessage.error({ message: '账号在其他设备登录，您已被自动退出', duration: 5000, grouping: true })
          return Promise.reject(error)
        }
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
    } else if (isNetworkError(error)) {
      const retryCount = originalRequest._networkRetryCount || 0
      if (retryCount < MAX_NETWORK_RETRY) {
        originalRequest._networkRetryCount = retryCount + 1
        await sleep(RETRY_DELAY)
        return service(originalRequest)
      }
      ElMessage.error('网络异常，请检查网络连接')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default service

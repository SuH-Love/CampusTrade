import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { refreshToken as refreshTokenApi } from '@/api/auth'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

const processPendingRequests = (token: string) => {
  pendingRequests.forEach(cb => cb(token))
  pendingRequests = []
}

const MAX_NETWORK_RETRY = 3
const RETRY_DELAY = 500

const isRetryableError = (error: unknown): boolean => {
  const err = error as { response?: { status?: number }; code?: string; message?: string }
  if (!err.response) {
    return err.code === 'ERR_NETWORK' ||
      err.code === 'ECONNABORTED' ||
      err.code === 'ECONNRESET' ||
      (!!err.message && err.message.includes('Network Error')) ||
      (!!err.message && err.message.includes('timeout')) ||
      (!!err.message && err.message.includes('ERR_CONNECTION_RESET'))
  }
  const status = err.response.status
  return status === 502 || status === 503 || status === 504
}

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    const noAuthUrls = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/send-code', '/auth/reset-password']
    const isNoAuth = noAuthUrls.some(url => config.url?.includes(url))
    if (userStore.token && !isNoAuth) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean; _networkRetryCount?: number }
    if (error.response) {
      const { status } = error.response
      if (status === 401 && !originalRequest._retry) {
        const userStore = useUserStore()
        if (!userStore.refreshToken) {
          userStore.clearAuth()
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
          const data = await refreshTokenApi(userStore.refreshToken)
          userStore.setAuth(data)
          processPendingRequests(data.accessToken)
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
          return service(originalRequest)
        } catch {
          userStore.clearAuth()
          router.push('/login')
          ElMessage.error('登录已过期，请重新登录')
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
    } else if (isRetryableError(error)) {
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

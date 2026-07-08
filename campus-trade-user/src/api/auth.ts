import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  phone?: string
  email?: string
}

export interface TokenVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export const login = (data: LoginParams) => request.post<never, TokenVO>('/auth/login', data)

export const register = (data: RegisterParams) => request.post<never, TokenVO>('/auth/register', data)

export const logout = () => request.post('/auth/logout')

export const refreshToken = (refreshToken: string) => request.post<never, TokenVO>('/auth/refresh', { refreshToken })

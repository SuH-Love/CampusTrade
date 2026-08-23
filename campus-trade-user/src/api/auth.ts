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

export interface SendCodeParams {
  username: string
  phone: string
}

export interface ResetPasswordParams {
  username: string
  phone: string
  code: string
  newPassword: string
}

export const sendResetCode = (data: SendCodeParams) => request.post('/auth/send-code', data)

export const resetPassword = (data: ResetPasswordParams) => request.post('/auth/reset-password', data)

import request from '@/utils/request'
import type { Result, LoginVO, LoginRequest, RegisterRequest } from '@/types'

export function login(data: LoginRequest) {
  return request.post<any, Result<LoginVO>>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<any, Result<void>>('/auth/register', data)
}

export function refreshToken(token: string) {
  return request.post<any, Result<{ accessToken: string; refreshToken: string }>>('/auth/refresh', { refreshToken: token })
}

export function logout() {
  return request.post<any, Result<void>>('/auth/logout')
}

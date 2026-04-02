import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, refreshToken as refreshTokenApi } from '@/api/auth'
import type { LoginVO } from '@/types'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('accessToken') || localStorage.getItem('token') || '')
  const refreshTokenVal = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<LoginVO | null>(null)

  const savedUser = localStorage.getItem('user')
  if (savedUser) {
    try {
      userInfo.value = JSON.parse(savedUser)
    } catch { /* ignore */ }
  }

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    const loginData = res.data as any
    token.value = loginData.accessToken || loginData.token
    refreshTokenVal.value = loginData.refreshToken || ''
    userInfo.value = loginData
    // 同时存储 accessToken 和 refreshToken
    localStorage.setItem('accessToken', token.value)
    localStorage.setItem('refreshToken', refreshTokenVal.value)
    localStorage.setItem('token', token.value)
    localStorage.setItem('user', JSON.stringify(loginData))
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      refreshTokenVal.value = ''
      userInfo.value = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }

  async function refreshAccessToken(): Promise<boolean> {
    if (!refreshTokenVal.value) return false
    try {
      const res = await refreshTokenApi(refreshTokenVal.value)
      const data = res.data as any
      token.value = data.accessToken || data.token
      refreshTokenVal.value = data.refreshToken || refreshTokenVal.value
      localStorage.setItem('accessToken', token.value)
      localStorage.setItem('refreshToken', refreshTokenVal.value)
      localStorage.setItem('token', token.value)
      return true
    } catch {
      return false
    }
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { token, refreshToken: refreshTokenVal, userInfo, login, logout, refreshAccessToken, isLoggedIn }
})

import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// Token 刷新相关状态
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

// 请求拦截器：自动携带 accessToken
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：401 自动刷新 token
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        handleUnauthorized(response.config)
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      return handleUnauthorized(error.config)
    }
    ElMessage.error(error.response?.data?.message || '网络错误')
    return Promise.reject(error)
  }
)

// 处理 401：尝试用 refreshToken 刷新
function handleUnauthorized(config: AxiosRequestConfig) {
  // 如果正在刷新，将请求加入队列等待
  if (isRefreshing) {
    return new Promise((resolve) => {
      pendingRequests.push((newToken: string) => {
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${newToken}`
        resolve(request(config))
      })
    })
  }

  isRefreshing = true
  const refreshToken = localStorage.getItem('refreshToken')

  if (!refreshToken) {
    clearAuthAndRedirect()
    return Promise.reject(new Error('未登录'))
  }

  return axios.post('/api/auth/refresh', { refreshToken })
    .then((res) => {
      const data = res.data
      if (data.code === 200 && data.data) {
        const newAccessToken = data.data.accessToken || data.data.token
        const newRefreshToken = data.data.refreshToken || refreshToken
        localStorage.setItem('accessToken', newAccessToken)
        localStorage.setItem('refreshToken', newRefreshToken)

        // 更新 Pinia store（如果有）
        try {
          const piniaState = JSON.parse(localStorage.getItem('user') || '{}')
          if (piniaState.token) {
            piniaState.token = newAccessToken
            localStorage.setItem('user', JSON.stringify(piniaState))
          }
        } catch { /* ignore */ }

        // 重试所有排队请求
        pendingRequests.forEach((cb) => cb(newAccessToken))
        pendingRequests = []

        // 重试原请求
        config.headers = config.headers || {}
        config.headers.Authorization = `Bearer ${newAccessToken}`
        return request(config)
      } else {
        clearAuthAndRedirect()
        return Promise.reject(new Error('token 刷新失败'))
      }
    })
    .catch(() => {
      clearAuthAndRedirect()
      return Promise.reject(new Error('token 刷新失败'))
    })
    .finally(() => {
      isRefreshing = false
    })
}

// 清除认证信息并跳转登录页
function clearAuthAndRedirect() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  pendingRequests = []
  window.location.href = '/login'
}

export default request

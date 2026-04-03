import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage, ElNotification } from 'element-plus'

// 扩展 AxiosRequestConfig 添加重试计数
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    __retryCount?: number
  }
}

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// Token 刷新相关状态
let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []
let refreshTimer: ReturnType<typeof setTimeout> | null = null

// M5-09: Token 过期前 5 分钟自动刷新
function scheduleTokenRefresh() {
  if (refreshTimer) clearTimeout(refreshTimer)
  try {
    const token = localStorage.getItem('accessToken')
    if (!token) return
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // ms
    const now = Date.now()
    const refreshAt = exp - 5 * 60 * 1000 // 5 min before expiry
    if (refreshAt <= now) {
      // Already near expiry, refresh now
      proactiveRefresh()
    } else {
      refreshTimer = setTimeout(proactiveRefresh, refreshAt - now)
    }
  } catch {
    // Invalid token, ignore
  }
}

function proactiveRefresh() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return
  if (isRefreshing) return
  isRefreshing = true
  axios.post('/api/auth/refresh', { refreshToken })
    .then(res => {
      const data = res.data
      if (data.code === 200 && data.data) {
        const newToken = data.data.accessToken || data.data.token
        const newRefresh = data.data.refreshToken || refreshToken
        localStorage.setItem('accessToken', newToken)
        localStorage.setItem('refreshToken', newRefresh)
        scheduleTokenRefresh()
      }
    })
    .catch(() => {})
    .finally(() => { isRefreshing = false })
}

// Initial schedule
scheduleTokenRefresh()
// Re-schedule on visibility change
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible') scheduleTokenRefresh()
})

// 网络异常自动重试配置
const MAX_RETRY = 2
const RETRY_DELAY = 1000
const RETRYABLE_STATUS = [408, 429, 500, 502, 503, 504]
const RETRYABLE_ERRORS = ['ECONNABORTED', 'ECONNRESET', 'ETIMEDOUT', 'ERR_NETWORK']

// 请求拦截器：自动携带 accessToken + 初始化重试计数
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 初始化重试计数
  config.__retryCount = 0
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
    const config = error.config
    const retryCount = config?.__retryCount || 0

    // 网络断开检测（不重试）
    if (!navigator.onLine) {
      ElNotification.error({ title: '网络已断开', message: '请检查网络连接后重试', duration: 0 })
      return Promise.reject(error)
    }

    // 401 走 token 刷新逻辑（不重试）
    if (error.response?.status === 401) {
      return handleUnauthorized(config)
    }

    // 判断是否可重试
    const isRetryable =
      retryCount < MAX_RETRY &&
      (RETRYABLE_STATUS.includes(error.response?.status) || RETRYABLE_ERRORS.includes(error.code))

    if (isRetryable) {
      config.__retryCount = retryCount + 1
      console.warn(`请求重试 (${config.__retryCount}/${MAX_RETRY}): ${config.url}`)
      return new Promise(resolve => {
        setTimeout(() => resolve(request(config)), RETRY_DELAY * retryCount)
      })
    }

    // 超过重试次数，显示最终错误
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络')
    } else if (error.response?.status >= 500) {
      ElMessage.error('服务器异常，请稍后重试')
    } else if (error.response?.status === 429) {
      ElMessage.error('请求频率过高，请稍后再试')
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
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

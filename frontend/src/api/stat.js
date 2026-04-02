import request from './request'

export function getMonthlyStat(month) {
  return request.get('/api/stat/monthly', { params: { month } })
}

export function getCategoryRatio(month, type) {
  return request.get('/api/stat/category-ratio', { params: { month, type } })
}

export function getTrend(months) {
  return request.get('/api/stat/trend', { params: { months } })
}

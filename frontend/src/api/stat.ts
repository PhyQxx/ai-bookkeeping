import request from '@/utils/request'
import type { Result, MonthlyStat, CategoryRatio, TrendItem } from '@/types'

export function getMonthlyStat(month?: string) {
  return request.get<any, Result<MonthlyStat>>('/stat/monthly', { params: { month } })
}

export function getCategoryRatio(month?: string, type?: number) {
  return request.get<any, Result<CategoryRatio[]>>('/stat/category-ratio', { params: { month, type } })
}

export function getTrend(months?: number) {
  return request.get<any, Result<TrendItem[]>>('/stat/trend', { params: { months } })
}

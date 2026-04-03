import request from '@/utils/request'
import type { Result } from '@/types'

export interface DashboardSummary {
  month: string
  totalIncome: number
  totalExpense: number
  balance: number
  billCount: number
}

export interface DailyTrend {
  date: string
  income: number
  expense: number
}

export interface CategoryRanking {
  categoryId: number
  categoryName: string
  amount: number
  count: number
  percentage: number
}

export interface BudgetProgress {
  budgetId: number
  categoryName: string
  budgetAmount: number
  usedAmount: number
  remainingAmount: number
  usagePercent: number
  overBudget: boolean
}

export function getDashboardSummary() {
  return request.get<any, Result<DashboardSummary>>('/dashboard/summary')
}

export function getDashboardTrend() {
  return request.get<any, Result<DailyTrend[]>>('/dashboard/trend')
}

export function getCategoryRanking() {
  return request.get<any, Result<CategoryRanking[]>>('/dashboard/category-ranking')
}

export function getBudgetProgress() {
  return request.get<any, Result<BudgetProgress[]>>('/dashboard/budget-progress')
}

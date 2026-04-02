import request from '@/utils/request'

export function getBudgetList(month: string) {
  return request.get('/budget/list', { params: { month } })
}

export function saveBudget(data: { categoryId?: number; month: string; amount: number }) {
  return request.post('/budget', data)
}

export function deleteBudget(id: number) {
  return request.delete(`/budget/${id}`)
}

export function getBudgetUsage(month: string) {
  return request.get('/budget/usage', { params: { month } })
}

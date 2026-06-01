import request from '@/utils/request'
import type { Result, RecurringBill } from '@/types'

export function listRecurringBills() {
  return request.get<any, Result<RecurringBill[]>>('/recurring/list')
}

export function createRecurringBill(data: any) {
  return request.post<any, Result<RecurringBill>>('/recurring', data)
}

export function updateRecurringBill(id: number, data: any) {
  return request.put<any, Result<RecurringBill>>(`/recurring/${id}`, data)
}

export function deleteRecurringBill(id: number) {
  return request.delete<any, Result<void>>(`/recurring/${id}`)
}

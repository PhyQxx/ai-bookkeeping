import request from '@/utils/request'
import type { Result, AiParseVO, AiParseRequest, Bill, BillRequest, Page } from '@/types'

export function aiParseBill(data: AiParseRequest) {
  return request.post<any, Result<AiParseVO>>('/bill/ai-parse', data)
}

export function createBill(data: BillRequest) {
  return request.post<any, Result<Bill>>('/bill', data)
}

export function updateBill(id: number, data: BillRequest) {
  return request.put<any, Result<Bill>>(`/bill/${id}`, data)
}

export function deleteBill(id: number) {
  return request.delete<any, Result<void>>(`/bill/${id}`)
}

export function listBills(params: {
  type?: number
  categoryId?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}) {
  return request.get<any, Result<Page<Bill>>>('/bill/list', { params })
}

export function getBill(id: number) {
  return request.get<any, Result<Bill>>(`/bill/${id}`)
}

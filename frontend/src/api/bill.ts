import request from '@/utils/request'
import type { Result, Page, Bill } from '@/types'

export function exportBills(params: {
  startDate?: string
  endDate?: string
  categoryId?: number
  type?: number
  format?: string
}) {
  return request.get('/bills/export', {
    params: { ...params },
    responseType: 'blob'
  })
}

export function listBills(params: {
  type?: number
  categoryId?: number
  startDate?: string
  endDate?: string
  searchText?: string
  minAmount?: number
  maxAmount?: number
  pageNum?: number
  pageSize?: number
}) {
  return request.get<any, Result<Page<Bill>>>('/bill/list', { params })
}

export function createBill(data: any) {
  return request.post<any, Result<Bill>>('/bill', data)
}

export function updateBill(id: number, data: any) {
  return request.put<any, Result<Bill>>(`/bill/${id}`, data)
}

export function deleteBill(id: number) {
  return request.delete<any, Result<void>>(`/bill/${id}`)
}

export function getBill(id: number) {
  return request.get<any, Result<Bill>>(`/bill/${id}`)
}

export function getBillDetail(id: number) {
  return request.get<any, Result<Bill>>(`/bill/${id}`)
}

export function batchDeleteBills(ids: number[]) {
  return request.delete<any, Result<void>>('/bill/batch', { data: { ids } })
}

export function aiParseBill(data: any) {
  return request.post<any, Result<any>>('/bill/ai-parse', data)
}

export function aiParsePreview(input: string) {
  return request.post<any, Result<any>>('/bill/ai-parse-preview', { input })
}

export function aiBatchParsePreview(input: string) {
  return request.post<any, Result<any>>('/bill/ai-batch-parse-preview', { input })
}

export function aiOcrPreview(imageBase64: string) {
  return request.post<any, Result<any>>('/bill/ai-ocr-preview', { imageBase64 })
}

export function aiConfirmBill(data: any) {
  return request.post<any, Result<Bill>>('/bill/ai-confirm', data)
}

export function aiBatchConfirm(data: any[]) {
  return request.post<any, Result<Bill[]>>('/bill/ai-batch-confirm', data)
}

export function importAlipay(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, Result<number>>('/bill/import/alipay', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function importWechat(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, Result<number>>('/bill/import/wechat', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

import request from '@/utils/request'
import type { Result, Ledger } from '@/types'

export function listLedgers() {
  return request.get<any, Result<Ledger[]>>('/ledger/list')
}

export function createLedger(data: { name: string, description?: string }) {
  return request.post<any, Result<Ledger>>('/ledger', data)
}

export function updateLedger(id: number, data: { name: string, description?: string }) {
  return request.put<any, Result<Ledger>>(`/ledger/${id}`, data)
}

export function deleteLedger(id: number) {
  return request.delete<any, Result<void>>(`/ledger/${id}`)
}

export function switchLedger(id: number) {
  return request.post<any, Result<void>>(`/ledger/switch/${id}`)
}

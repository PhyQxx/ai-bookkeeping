import request from '@/utils/request'
import type { Result, Page } from '@/types'

export interface AuditLog {
  id: number
  userId: number
  username: string
  action: string
  module: string
  detail: string
  ip: string
  method: string
  path: string
  createdAt: string
}

export function listMyAuditLogs(params: { pageNum: number, pageSize: number }) {
  return request.get<any, Result<Page<AuditLog>>>('/audit/my', { params })
}

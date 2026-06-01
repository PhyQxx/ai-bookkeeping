import request from '@/utils/request'
import type { Result } from '@/types'

export interface Notification {
  id: number
  userId: number
  title: string
  content: string
  type: string
  isRead: number
  createdAt: string
}

export function listUnreadNotifications() {
  return request.get<any, Result<Notification[]>>('/notification/unread')
}

export function markNotificationAsRead(id: number) {
  return request.post<any, Result<void>>(`/notification/${id}/read`)
}

export function markAllAsRead() {
  return request.post<any, Result<void>>('/notification/read-all')
}

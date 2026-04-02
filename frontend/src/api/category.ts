import request from '@/utils/request'
import type { Result, Category, CategoryRequest } from '@/types'

export function listCategories(type?: number) {
  return request.get<any, Result<Category[]>>('/category/list', { params: { type } })
}

export function createCategory(data: CategoryRequest) {
  return request.post<any, Result<Category>>('/category', data)
}

export function updateCategory(id: number, data: CategoryRequest) {
  return request.put<any, Result<Category>>(`/category/${id}`, data)
}

export function deleteCategory(id: number) {
  return request.delete<any, Result<void>>(`/category/${id}`)
}

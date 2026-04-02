import request from './request'

export function getCategoryList(type) {
  return request.get('/api/category/list', { params: { type } })
}

export function createCategory(data) {
  return request.post('/api/category', data)
}

export function updateCategory(id, data) {
  return request.put(`/api/category/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/api/category/${id}`)
}

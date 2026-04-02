import request from './request'

export function aiParse(data) {
  return request.post('/api/bill/ai-parse', data)
}

export function createBill(data) {
  return request.post('/api/bill', data)
}

export function updateBill(id, data) {
  return request.put(`/api/bill/${id}`, data)
}

export function deleteBill(id) {
  return request.delete(`/api/bill/${id}`)
}

export function getBillList(params) {
  return request.get('/api/bill/list', { params })
}

export function getBillDetail(id) {
  return request.get(`/api/bill/${id}`)
}

import request from '@/utils/request'

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUserInfo(data: { nickname?: string; avatar?: string }) {
  return request.put('/user/info', data)
}

export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put('/user/password', data)
}

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi } from '../api/auth'
import router from '../router'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).username : '')
  const nickname = ref(localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).nickname : '')
  const avatar = ref(localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).avatar : '')

  async function login(form) {
    const data = await loginApi(form)
    token.value = data.token
    username.value = data.username
    nickname.value = data.nickname
    avatar.value = data.avatar
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify({ username: data.username, nickname: data.nickname, avatar: data.avatar }))
    router.push('/')
  }

  async function logout() {
    try { await logoutApi() } catch {}
    token.value = ''
    username.value = ''
    nickname.value = ''
    avatar.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }

  function isLoggedIn() {
    return !!token.value
  }

  return { token, username, nickname, avatar, login, logout, isLoggedIn }
})

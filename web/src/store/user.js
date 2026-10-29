import { defineStore } from 'pinia'
import http from '../api/http'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('cb_token') || '',
    info: JSON.parse(localStorage.getItem('cb_user') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.info && s.info.role === 'ADMIN',
    displayName: (s) => (s.info ? s.info.nickname || s.info.username : '')
  },
  actions: {
    save(data) {
      this.token = data.token
      this.info = { userId: data.userId, username: data.username, nickname: data.nickname, role: data.role }
      localStorage.setItem('cb_token', data.token)
      localStorage.setItem('cb_user', JSON.stringify(this.info))
    },
    async login(payload) {
      const data = await http.post('/user/login', payload)
      this.save(data)
      return data
    },
    async register(payload) {
      const data = await http.post('/user/register', payload)
      this.save(data)
      return data
    },
    logout() {
      this.token = ''
      this.info = null
      localStorage.removeItem('cb_token')
      localStorage.removeItem('cb_user')
    }
  }
})
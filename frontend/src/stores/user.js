import { defineStore } from 'pinia'
import { authApi, messageApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('campus_token') || '',
    user: JSON.parse(localStorage.getItem('campus_user') || 'null'),
    unreadCount: 0
  }),

  getters: {
    isLogin: (state) => Boolean(state.token),
    isAdmin: (state) => state.user && state.user.role === 'ADMIN',
    nickname: (state) => (state.user ? state.user.nickname : '')
  },

  actions: {
    /** 登录：拿到 token 后写入本地存储，并拉取一次未读数 */
    async login(form) {
      const res = await authApi.login(form)
      this.token = res.data.token
      this.user = res.data.user
      localStorage.setItem('campus_token', this.token)
      localStorage.setItem('campus_user', JSON.stringify(this.user))
      this.refreshUnread()
      return res
    },

    async register(form) {
      return authApi.register(form)
    },

    /** 刷新当前用户信息（页面刷新后保持登录态） */
    async fetchMe() {
      if (!this.token) return null
      try {
        const res = await authApi.me()
        this.user = res.data
        localStorage.setItem('campus_user', JSON.stringify(this.user))
        this.refreshUnread()
        return this.user
      } catch (e) {
        this.clear()
        return null
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } catch (e) {
        // 退出接口失败不影响本地清理
      }
      this.clear()
    },

    clear() {
      this.token = ''
      this.user = null
      this.unreadCount = 0
      localStorage.removeItem('campus_token')
      localStorage.removeItem('campus_user')
    },

    /** 未读消息数：导航栏红点，每 30 秒轮询一次 */
    async refreshUnread() {
      if (!this.token) {
        this.unreadCount = 0
        return
      }
      try {
        const res = await messageApi.unreadCount()
        this.unreadCount = res.data.count
      } catch (e) {
        // 静默失败，不打扰用户
      }
    }
  }
})

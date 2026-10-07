import { http } from '@/api/http'
import type { Session } from './auth.types'

// el token viaja en una cookie HttpOnly que setea y borra el backend: este codigo nunca lo ve
export const authApi = {
  me: async () => {
    const res = await http.get<Session>('/auth/me')
    return res.data
  },

  logout: async () => {
    await http.post('/auth/logout')
  },

  loginUrl: '/api/auth/login?cliente=web',
}

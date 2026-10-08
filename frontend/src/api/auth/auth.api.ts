import { http } from '@/api/http'
import type { AuthConfig, Session } from './auth.types'

// el token viaja en una cookie HttpOnly que setea y borra el backend: este codigo nunca lo ve
export const authApi = {
  me: async () => {
    const res = await http.get<Session>('/usuarios/me')
    return res.data
  },

  logout: async () => {
    await http.post('/auth/logout')
  },

  getConfig: async () => {
    const res = await http.get<AuthConfig>('/auth/config')
    return res.data
  },

  loginUrl: '/api/auth/login?cliente=web',

  mockLoginUrl: (cedula: string) => `/api/auth/login?cliente=web&cedula=${encodeURIComponent(cedula)}`,
}

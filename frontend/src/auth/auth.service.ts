import api from '../common/api.ts'

export type Sesion = {
  id: number
  cedula: string
  correo: string
  roles: string[]
}

// el token viaja en una cookie HttpOnly que setea y borra el backend: este codigo nunca lo ve
const auth = {
  me: async () => {
    const res = await api.get<Sesion>('/auth/me')
    return res.data
  },

  salir: async () => {
    await api.post('/auth/logout')
  },

  urlLogin: '/api/auth/login?cliente=web',
}

export default auth

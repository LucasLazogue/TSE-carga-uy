import api from '../common/api.ts'

export type Sesion = {
  id: number
  cedula: string
  correo: string
  roles: string[]
}

const auth = {
  me: async () => {
    const res = await api.get<Sesion>('/auth/me')
    return res.data
  },

  urlLogin: '/api/auth/login',
  urlLogout: '/api/auth/logout',
}

export default auth

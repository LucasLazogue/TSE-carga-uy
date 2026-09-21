import api from '../../common/api.ts'
import type { Permiso, PermisoNuevo } from './permisos.types.ts'

const permisos = {
  getAll: async (idVehiculo?: number) => {
    const res = await api.get<Permiso[]>('/permisos', {
      params: { idVehiculo },
    })
    return res.data
  },

  getById: async (id: number) => {
    const res = await api.get<Permiso>(`/permisos/${id}`)
    return res.data
  },

  create: async (permiso: PermisoNuevo) => {
    await api.post('/permisos', permiso)
  },

  update: async (id: number, permiso: Permiso) => {
    await api.put(`/permisos/${id}`, permiso)
  },

  remove: async (id: number) => {
    await api.delete(`/permisos/${id}`)
  },
}

export default permisos

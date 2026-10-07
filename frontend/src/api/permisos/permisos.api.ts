import { http } from '@/api/http'
import type { Permiso, PermisoNuevo } from './permisos.types'

export const permisosApi = {
  getAll: async (idVehiculo?: number) => {
    const res = await http.get<Permiso[]>('/permisos', {
      params: { idVehiculo },
    })
    return res.data
  },

  create: async (permiso: PermisoNuevo) => {
    await http.post('/permisos', permiso)
  },
}

import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Permiso, PermisoNuevo } from './permisos.types'

export const permisosApi = {
  getAll: async (idVehiculo?: number, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Permiso>>('/permisos', {
      params: { idVehiculo, ...paginacion },
    })
    return res.data
  },

  create: async (permiso: PermisoNuevo) => {
    await http.post('/permisos', permiso)
  },
}

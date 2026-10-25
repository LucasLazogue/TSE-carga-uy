import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { FiltroPermisos, Permiso, PermisoNuevo } from './permisos.types'

export const PermisosApi = {
  findAll: async (filtro: FiltroPermisos = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Permiso>>('/permisos', { params: { ...filtro, ...paginacion } })).data,

  create: async (permiso: PermisoNuevo) => {
    await http.post('/permisos', permiso)
  },

  update: async (id: number, permiso: PermisoNuevo) => {
    await http.put(`/permisos/${id}`, permiso)
  },

  remove: async (id: number) => {
    await http.delete(`/permisos/${id}`)
  },
}

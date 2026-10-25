import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { AprobacionPermiso, FiltroPermisos, Permiso, PermisoNuevo } from './permisos.types'

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

  aprobar: async (id: number, aprobacion: AprobacionPermiso) => {
    await http.post(`/permisos/${id}/aprobacion`, aprobacion)
  },

  rechazar: async (id: number, motivo: string) => {
    await http.post(`/permisos/${id}/rechazo`, { motivo })
  },
}

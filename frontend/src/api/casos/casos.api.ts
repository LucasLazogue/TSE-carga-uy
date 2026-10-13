import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Caso, FiltroCasos, PistaAuditoria, ResolucionNueva } from './casos.types'

export const casosApi = {
  getAll: async (filtro: FiltroCasos = {}, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Caso>>('/casos', { params: { ...filtro, ...paginacion } })
    return res.data
  },

  getById: async (id: number) => {
    const res = await http.get<Caso>(`/casos/${id}`)
    return res.data
  },

  getAuditoria: async (id: number) => {
    const res = await http.get<PistaAuditoria[]>(`/casos/${id}/auditoria`)
    return res.data
  },

  resolver: async (id: number, resolucion: ResolucionNueva) => {
    const res = await http.post<Caso>(`/casos/${id}/resolucion`, resolucion)
    return res.data
  },
}

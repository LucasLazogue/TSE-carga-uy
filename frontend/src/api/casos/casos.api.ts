import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Caso, FiltroCasos, PistaAuditoria, ResolucionNueva } from './casos.types'

export const CasosApi = {
  findAll: async (filtro: FiltroCasos = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Caso>>('/casos', { params: { ...filtro, ...paginacion } })).data,

  find: async (id: number) => (await http.get<Caso>(`/casos/${id}`)).data,

  findAuditoria: async (id: number) => (await http.get<PistaAuditoria[]>(`/casos/${id}/auditoria`)).data,

  createResolucion: async (id: number, resolucion: ResolucionNueva) =>
    (await http.post<Caso>(`/casos/${id}/resolucion`, resolucion)).data,
}

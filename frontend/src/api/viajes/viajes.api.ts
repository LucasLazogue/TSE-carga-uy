import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { FiltroViajes, Viaje, ViajeNuevo } from './viajes.types'

export const ViajesApi = {
  findAll: async (idEmpresa: number, filtro: FiltroViajes = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Viaje>>(`/empresas/${idEmpresa}/viajes`, { params: { ...filtro, ...paginacion } })).data,

  create: async (idEmpresa: number, viaje: ViajeNuevo) =>
    (await http.post<Viaje>(`/empresas/${idEmpresa}/viajes`, viaje)).data,
}

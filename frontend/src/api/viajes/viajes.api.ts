import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { FiltroViajes, Viaje, ViajeNuevo } from './viajes.types'

export const viajesApi = {
  getAll: async (idEmpresa: number, filtro: FiltroViajes = {}, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Viaje>>(`/empresas/${idEmpresa}/viajes`, { params: { ...filtro, ...paginacion } })
    return res.data
  },

  create: async (idEmpresa: number, viaje: ViajeNuevo) => {
    const res = await http.post<Viaje>(`/empresas/${idEmpresa}/viajes`, viaje)
    return res.data
  },
}

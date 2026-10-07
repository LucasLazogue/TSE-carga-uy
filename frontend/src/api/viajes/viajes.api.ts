import { http } from '@/api/http'
import type { Page } from '@/api/page'
import type { FiltroViajes, Viaje, ViajeNuevo } from './viajes.types'

export const viajesApi = {
  getAll: async (filtro: FiltroViajes = {}) => {
    const res = await http.get<Page<Viaje>>('/viajes', { params: filtro })
    return res.data
  },

  create: async (viaje: ViajeNuevo) => {
    const res = await http.post<Viaje>('/viajes', viaje)
    return res.data
  },
}

import { http } from '@/api/http'
import type { Viaje, ViajeNuevo } from './viajes.types'

export const viajesApi = {
  getAll: async (idEmpresa?: number) => {
    const res = await http.get<Viaje[]>('/viajes', {
      params: { idEmpresa },
    })
    return res.data
  },

  create: async (viaje: ViajeNuevo) => {
    const res = await http.post<Viaje>('/viajes', viaje)
    return res.data
  },
}

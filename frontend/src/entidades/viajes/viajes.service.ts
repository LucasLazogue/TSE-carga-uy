import api from '../../common/api.ts'
import type { Viaje, ViajeNuevo } from './viajes.types.ts'

const viajes = {
  getAll: async (idEmpresa?: number) => {
    const res = await api.get<Viaje[]>('/viajes', {
      params: { idEmpresa },
    })
    return res.data
  },

  getById: async (id: number) => {
    const res = await api.get<Viaje>(`/viajes/${id}`)
    return res.data
  },

  create: async (viaje: ViajeNuevo) => {
    const res = await api.post<Viaje>('/viajes', viaje)
    return res.data
  },

  update: async (id: number, viaje: ViajeNuevo) => {
    await api.put(`/viajes/${id}`, viaje)
  },

  remove: async (id: number) => {
    await api.delete(`/viajes/${id}`)
  },
}

export default viajes

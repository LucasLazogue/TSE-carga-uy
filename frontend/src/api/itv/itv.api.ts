import { http } from '@/api/http'
import type { Itv, ItvNueva } from './itv.types'

export const ItvApi = {
  findAll: async (idVehiculo: number) => (await http.get<Itv[]>(`/vehiculos/${idVehiculo}/itv`)).data,

  create: async (idVehiculo: number, itv: ItvNueva) => {
    await http.post(`/vehiculos/${idVehiculo}/itv`, itv)
  },

  remove: async (idVehiculo: number, id: number) => {
    await http.delete(`/vehiculos/${idVehiculo}/itv/${id}`)
  },
}

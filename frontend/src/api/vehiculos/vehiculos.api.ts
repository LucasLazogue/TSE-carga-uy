import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { FiltroVehiculos, Vehiculo, VehiculoNuevo } from './vehiculos.types'

export const VehiculosApi = {
  findAll: async (filtro: FiltroVehiculos = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Vehiculo>>('/vehiculos', { params: { ...filtro, ...paginacion } })).data,

  create: async (vehiculo: VehiculoNuevo) => {
    await http.post('/vehiculos', vehiculo)
  },
}

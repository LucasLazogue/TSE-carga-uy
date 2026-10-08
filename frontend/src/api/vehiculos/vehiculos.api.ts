import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Vehiculo, VehiculoNuevo } from './vehiculos.types'

export const vehiculosApi = {
  getAll: async (idEmpresa?: number, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Vehiculo>>('/vehiculos', {
      params: { idEmpresa, ...paginacion },
    })
    return res.data
  },

  create: async (vehiculo: VehiculoNuevo) => {
    await http.post('/vehiculos', vehiculo)
  },
}

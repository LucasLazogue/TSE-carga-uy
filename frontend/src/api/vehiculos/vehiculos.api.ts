import { http } from '@/api/http'
import type { Vehiculo, VehiculoNuevo } from './vehiculos.types'

export const vehiculosApi = {
  getAll: async (idEmpresa?: number) => {
    const res = await http.get<Vehiculo[]>('/vehiculos', {
      params: { idEmpresa },
    })
    return res.data
  },

  create: async (vehiculo: VehiculoNuevo) => {
    await http.post('/vehiculos', vehiculo)
  },
}

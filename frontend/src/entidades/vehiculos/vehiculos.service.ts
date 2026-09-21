import api from '../../common/api.ts'
import type { Vehiculo, VehiculoNuevo } from './vehiculos.types.ts'

const vehiculos = {
  getAll: async (idEmpresa?: number) => {
    const res = await api.get<Vehiculo[]>('/vehiculos', {
      params: { idEmpresa },
    })
    return res.data
  },

  getById: async (id: number) => {
    const res = await api.get<Vehiculo>(`/vehiculos/${id}`)
    return res.data
  },

  create: async (vehiculo: VehiculoNuevo) => {
    await api.post('/vehiculos', vehiculo)
  },

  update: async (id: number, vehiculo: Vehiculo) => {
    await api.put(`/vehiculos/${id}`, vehiculo)
  },

  remove: async (id: number) => {
    await api.delete(`/vehiculos/${id}`)
  },
}

export default vehiculos

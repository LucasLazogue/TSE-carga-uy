import api from '../../common/api.ts'
import type { Empresa, EmpresaNueva } from './empresas.types.ts'

const empresas = {
  getAll: async (nombre?: string) => {
    const res = await api.get<Empresa[]>('/empresas', {
      params: { nombre },
    })
    return res.data
  },

  getById: async (id: number) => {
    const res = await api.get<Empresa>(`/empresas/${id}`)
    return res.data
  },

  create: async (empresa: EmpresaNueva) => {
    await api.post('/empresas', empresa)
  },

  update: async (id: number, empresa: Empresa) => {
    await api.put(`/empresas/${id}`, empresa)
  },

  remove: async (id: number) => {
    await api.delete(`/empresas/${id}`)
  },
}

export default empresas

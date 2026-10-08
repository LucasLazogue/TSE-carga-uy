import { http } from '@/api/http'
import type { Empresa, EmpresaNueva } from './empresas.types'

export const empresasApi = {
  getAll: async (nombre?: string) => {
    const res = await http.get<Empresa[]>('/empresas', {
      params: { nombre },
    })
    return res.data
  },

  getById: async (id: number) => {
    const res = await http.get<Empresa>(`/empresas/${id}`)
    return res.data
  },

  create: async (empresa: EmpresaNueva) => {
    await http.post('/empresas', empresa)
  },
}

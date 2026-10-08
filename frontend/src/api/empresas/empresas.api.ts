import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Empresa, EmpresaNueva } from './empresas.types'

export const empresasApi = {
  getAll: async (nombre?: string, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Empresa>>('/empresas', {
      params: { nombre, ...paginacion },
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

import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Empresa, EmpresaNueva, FiltroEmpresas } from './empresas.types'

export const EmpresasApi = {
  findAll: async (filtro: FiltroEmpresas = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Empresa>>('/empresas', { params: { ...filtro, ...paginacion } })).data,

  find: async (id: number) => (await http.get<Empresa>(`/empresas/${id}`)).data,

  create: async (empresa: EmpresaNueva) => {
    await http.post('/empresas', empresa)
  },
}

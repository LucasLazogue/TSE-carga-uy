import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Chofer, FiltroChoferes } from './choferes.types'

export const ChoferesApi = {
  findAll: async (idEmpresa: number, filtro: FiltroChoferes = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Chofer>>(`/empresas/${idEmpresa}/choferes`, { params: { ...filtro, ...paginacion } })).data,
}

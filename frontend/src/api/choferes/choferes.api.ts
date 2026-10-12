import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Chofer, FiltroChoferes } from './choferes.types'

export const choferesApi = {
  getAll: async (idEmpresa: number, filtro: FiltroChoferes = {}, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Chofer>>(`/empresas/${idEmpresa}/choferes`, { params: { ...filtro, ...paginacion } })
    return res.data
  },
}

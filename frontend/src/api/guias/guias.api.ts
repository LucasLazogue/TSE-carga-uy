import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { FiltroGuias, Guia, GuiaNueva, Rubro, TipoCarga } from './guias.types'

export const guiasApi = {
  getAll: async (idEmpresa: number, filtro: FiltroGuias = {}, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Guia>>(`/empresas/${idEmpresa}/guias`, { params: { ...filtro, ...paginacion } })
    return res.data
  },

  create: async (idEmpresa: number, guia: GuiaNueva) => {
    const res = await http.post<Guia>(`/empresas/${idEmpresa}/guias`, guia)
    return res.data
  },

  getRubros: async () => {
    const res = await http.get<Rubro[]>('/rubros')
    return res.data
  },

  getTiposCarga: async () => {
    const res = await http.get<TipoCarga[]>('/tipos-carga')
    return res.data
  },
}

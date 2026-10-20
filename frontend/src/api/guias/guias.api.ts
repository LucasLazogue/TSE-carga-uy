import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { FiltroGuias, Guia, GuiaNueva, Rubro, TipoCarga } from './guias.types'

export const GuiasApi = {
  findAll: async (idEmpresa: number, filtro: FiltroGuias = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Guia>>(`/empresas/${idEmpresa}/guias`, { params: { ...filtro, ...paginacion } })).data,

  create: async (idEmpresa: number, guia: GuiaNueva) =>
    (await http.post<Guia>(`/empresas/${idEmpresa}/guias`, guia)).data,

  getRubros: async () => (await http.get<Rubro[]>('/rubros')).data,

  getTiposCarga: async () => (await http.get<TipoCarga[]>('/tipos-carga')).data,
}

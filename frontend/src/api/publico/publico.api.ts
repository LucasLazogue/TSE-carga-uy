import { http } from '@/api/http'
import type { Rubro, TipoCarga } from '@/api/guias/guias.types'
import type { ConsultaHabilitacion, FiltroIndicadores, HabilitacionEmpresa, IndicadoresPublicos } from './publico.types'

export const PublicoApi = {
  validarEmpresa: async (consulta: ConsultaHabilitacion) =>
    (await http.get<HabilitacionEmpresa>('/publico/empresas/habilitacion', { params: consulta })).data,

  getIndicadores: async (filtro: FiltroIndicadores) =>
    (await http.get<IndicadoresPublicos>('/publico/indicadores', { params: filtro })).data,

  getRubros: async () => (await http.get<Rubro[]>('/publico/rubros')).data,

  getTiposCarga: async () => (await http.get<TipoCarga[]>('/publico/tipos-carga')).data,
}

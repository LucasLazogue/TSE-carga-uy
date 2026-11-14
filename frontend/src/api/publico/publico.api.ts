import { http } from '@/api/http'
import type { ConsultaHabilitacion, HabilitacionEmpresa } from './publico.types'

export const PublicoApi = {
  validarEmpresa: async (consulta: ConsultaHabilitacion) =>
    (await http.get<HabilitacionEmpresa>('/publico/empresas/habilitacion', { params: consulta })).data,
}

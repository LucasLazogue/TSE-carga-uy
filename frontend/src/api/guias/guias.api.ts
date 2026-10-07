import { http } from '@/api/http'
import type { Guia, GuiaNueva, Rubro, TipoCarga } from './guias.types'

export const guiasApi = {
  getAll: async (idEmpresa?: number) => {
    const res = await http.get<Guia[]>('/guias', {
      params: { idEmpresa },
    })
    return res.data
  },

  create: async (guia: GuiaNueva) => {
    const res = await http.post<Guia>('/guias', guia)
    return res.data
  },

  getRubros: async () => {
    const res = await http.get<Rubro[]>('/guias/rubros')
    return res.data
  },

  getTiposCarga: async () => {
    const res = await http.get<TipoCarga[]>('/guias/tipos-carga')
    return res.data
  },
}

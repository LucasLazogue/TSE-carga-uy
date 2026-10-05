import api from '../../common/api.ts'
import type { Guia, GuiaNueva, Rubro, TipoCarga } from './guias.types.ts'

const guias = {
  getAll: async (idEmpresa?: number) => {
    const res = await api.get<Guia[]>('/guias', {
      params: { idEmpresa },
    })
    return res.data
  },

  getById: async (id: number) => {
    const res = await api.get<Guia>(`/guias/${id}`)
    return res.data
  },

  create: async (guia: GuiaNueva) => {
    const res = await api.post<Guia>('/guias', guia)
    return res.data
  },

  update: async (id: number, guia: Guia) => {
    await api.put(`/guias/${id}`, guia)
  },

  remove: async (id: number) => {
    await api.delete(`/guias/${id}`)
  },

  getRubros: async () => {
    const res = await api.get<Rubro[]>('/guias/rubros')
    return res.data
  },

  getTiposCarga: async () => {
    const res = await api.get<TipoCarga[]>('/guias/tipos-carga')
    return res.data
  },
}

export default guias

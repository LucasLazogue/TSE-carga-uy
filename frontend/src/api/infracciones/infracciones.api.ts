import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Caso } from '@/api/casos/casos.types'
import type { FiltroInfracciones } from './infracciones.types'

export const infraccionesApi = {
  getAll: async (filtro: FiltroInfracciones = {}, paginacion: Paginacion = {}) => {
    const res = await http.get<Page<Caso>>('/infracciones', { params: { ...filtro, ...paginacion } })
    return res.data
  },

  getById: async (id: number) => {
    const res = await http.get<Caso>(`/infracciones/${id}`)
    return res.data
  },

  presentarDescargo: async (id: number, texto: string) => {
    const res = await http.post<Caso>(`/infracciones/${id}/descargo`, { texto })
    return res.data
  },
}

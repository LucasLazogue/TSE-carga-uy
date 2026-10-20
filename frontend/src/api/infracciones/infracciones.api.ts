import { http } from '@/api/http'
import type { Page, Paginacion } from '@/api/page'
import type { Caso } from '@/api/casos/casos.types'
import type { FiltroInfracciones } from './infracciones.types'

export const InfraccionesApi = {
  findAll: async (filtro: FiltroInfracciones = {}, paginacion: Paginacion = {}) =>
    (await http.get<Page<Caso>>('/infracciones', { params: { ...filtro, ...paginacion } })).data,

  find: async (id: number) => (await http.get<Caso>(`/infracciones/${id}`)).data,

  createDescargo: async (id: number, texto: string) =>
    (await http.post<Caso>(`/infracciones/${id}/descargo`, { texto })).data,
}

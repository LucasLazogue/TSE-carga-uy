import { http } from '@/api/http'
import type { Page } from '@/api/page'
import type { Notificacion } from './notificaciones.types'

// base es /casos para el funcionario y /infracciones para el responsable
export const NotificacionesApi = {
  findAll: async (base: string) => (await http.get<Page<Notificacion>>(`${base}/notificaciones`)).data,

  marcarLeidas: async (base: string) => {
    await http.post(`${base}/notificaciones/lectura`)
  },
}

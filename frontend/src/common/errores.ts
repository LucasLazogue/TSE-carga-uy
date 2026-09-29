import axios from 'axios'

export type ErrorApi = {
  codigo: number
  mensaje: string
}

export function mensajeDeError(e: unknown) {
  if (axios.isAxiosError<ErrorApi>(e) && e.response?.data?.mensaje) {
    return e.response.data.mensaje
  }
  return 'Ocurrio un error inesperado.'
}

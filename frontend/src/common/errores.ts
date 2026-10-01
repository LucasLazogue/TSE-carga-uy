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

const mensajesPorCodigo: Record<string, string> = {
  '4001': 'gub.uy no devolvio la cedula del usuario.',
  '4002': 'La solicitud de ingreso no es valida o expiro.',
  '4003': 'No se pudo validar la identidad devuelta por gub.uy.',
  '4004': 'El servicio de autenticacion de gub.uy no esta disponible.',
  '4005': 'El ingreso con gub.uy no esta configurado.',
  '4006': 'La sesion no es valida o expiro. Vuelva a ingresar.',
  '4007': 'El ingreso no esta configurado.',
}

export function mensajeDeCodigo(codigo: string | null) {
  if (!codigo) {
    return null
  }
  return mensajesPorCodigo[codigo] ?? 'No se pudo ingresar.'
}

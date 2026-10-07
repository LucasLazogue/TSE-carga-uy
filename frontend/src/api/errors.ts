import axios from 'axios'

export type ApiError = {
  codigo: number
  mensaje: string
}

export function getErrorMessage(e: unknown) {
  if (axios.isAxiosError<ApiError>(e) && e.response?.data?.mensaje) {
    return e.response.data.mensaje
  }
  return 'Ocurrio un error inesperado.'
}

const loginMessages: Record<string, string> = {
  '4001': 'gub.uy no devolvio la cedula del usuario.',
  '4002': 'La solicitud de ingreso no es valida o expiro.',
  '4003': 'No se pudo validar la identidad devuelta por gub.uy.',
  '4004': 'El servicio de autenticacion de gub.uy no esta disponible.',
  '4005': 'El ingreso con gub.uy no esta configurado.',
  '4006': 'La sesion no es valida o expiro. Vuelva a ingresar.',
  '4007': 'El ingreso no esta configurado.',
}

export function getLoginErrorMessage(code: string | null) {
  if (!code) {
    return null
  }
  return loginMessages[code] ?? 'No se pudo ingresar.'
}

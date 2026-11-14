export type HabilitacionEmpresa = {
  nombrePublico: string
  razonSocial: string
  habilitada: boolean
  fecha: string
}

export type ConsultaHabilitacion = { nro: number } | { razonSocial: string }

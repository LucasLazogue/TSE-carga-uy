export type Chofer = {
  id: number
  idVinculo: number
  cedula: string
  correo?: string | null
  fechaDesde: string
  fechaHasta: string | null
}

export type FiltroChoferes = {
  cedula?: string
  fecha?: string
  historial?: boolean
}

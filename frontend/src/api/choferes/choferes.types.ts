export type Chofer = {
  id: number
  cedula: string
  correo?: string | null
}

export type FiltroChoferes = {
  cedula?: string
  fecha?: string
}

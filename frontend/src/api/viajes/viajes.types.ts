export type Viaje = {
  id: number
  estado: string
  fechaInicio: string | null
  fechaFin: string | null
  idGuia: number
  nroGuia: string
  idVehiculo: number
  matricula: string
  idChofer: number
  cedulaChofer: string
}

export type ViajeNuevo = Pick<Viaje, 'idGuia' | 'idVehiculo' | 'idChofer'>

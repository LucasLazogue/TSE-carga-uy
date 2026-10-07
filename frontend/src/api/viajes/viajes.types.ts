export type EstadoViaje = 'ASIGNADO' | 'EN_CURSO' | 'FINALIZADO'

export type Viaje = {
  id: number
  estado: EstadoViaje
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

export type FiltroViajes = {
  idEmpresa?: number
  idChofer?: number
  idVehiculo?: number
  estado?: EstadoViaje
  pagina?: number
  tamanio?: number
}

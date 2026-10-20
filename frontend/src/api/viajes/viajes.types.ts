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
  idChofer?: number
  idVehiculo?: number
  estado?: EstadoViaje
}

export type TipoEventoViaje = 'INICIO' | 'CARGA_PARCIAL' | 'DESCARGA_PARCIAL' | 'INCIDENTE' | 'FIN'

export type ResultadoEvento = 'APLICADO' | 'DUPLICADO' | 'CONFLICTO' | 'RECHAZADO'

export type EventoViaje = {
  idEvento: string
  idViaje: number
  tipo: TipoEventoViaje
  timestampGeneracion: string
  timestampRecepcion: string
  resultado: ResultadoEvento
  detalle: string | null
}

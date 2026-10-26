export type EstadoPermiso = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO'

export type Permiso = {
  id: number
  nroPermiso: string
  validoDesde: string
  validoHasta: string | null
  idVehiculo: number
  matricula: string
  estado: EstadoPermiso
  motivoRechazo: string | null
}

export type PermisoNuevo = Pick<Permiso, 'nroPermiso' | 'validoDesde' | 'idVehiculo'>

export type AprobacionPermiso = {
  validoDesde: string
  validoHasta: string
}

export type FiltroPermisos = {
  idEmpresa?: number
  idVehiculo?: number
  estado?: EstadoPermiso
}

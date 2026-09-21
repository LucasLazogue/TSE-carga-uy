export type Permiso = {
  id: number
  nroPermiso: string
  validoDesde: string
  validoHasta: string
  idVehiculo: number
  matricula: string
}

export type PermisoNuevo = Omit<Permiso, 'id' | 'matricula'>

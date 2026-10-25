import type { FilterOption } from '@/components/toolbar/FilterSelect'
import type { EstadoPermiso, PermisoNuevo } from '@/api/permisos/permisos.types'

export const PERMISO_VACIO: PermisoNuevo = {
  nroPermiso: '',
  validoDesde: '',
  idVehiculo: 0,
}

export const NOMBRES_ESTADO: Record<EstadoPermiso, string> = {
  PENDIENTE: 'Pendiente',
  APROBADO: 'Aprobado',
  RECHAZADO: 'Rechazado',
}

export const OPCIONES_ESTADO: FilterOption[] = Object.entries(NOMBRES_ESTADO).map(([value, label]) => ({
  value,
  label,
}))

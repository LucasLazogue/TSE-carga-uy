import type { FilterOption } from '@/components/toolbar/FilterSelect'
import type { EstadoViaje, ResultadoEvento, TipoEventoViaje } from '@/api/viajes/viajes.types'

export const NOMBRES_ESTADO: Record<EstadoViaje, string> = {
  ASIGNADO: 'Asignado',
  EN_CURSO: 'En curso',
  FINALIZADO: 'Finalizado',
}

export const OPCIONES_ESTADO: FilterOption[] = Object.entries(NOMBRES_ESTADO).map(([value, label]) => ({
  value,
  label,
}))

export const NOMBRES_TIPO_EVENTO: Record<TipoEventoViaje, string> = {
  INICIO: 'Inicio',
  CARGA_PARCIAL: 'Carga parcial',
  DESCARGA_PARCIAL: 'Descarga parcial',
  INCIDENTE: 'Incidente',
  FIN: 'Fin',
}

export const NOMBRES_RESULTADO: Record<ResultadoEvento, string> = {
  APLICADO: 'Aplicado',
  DUPLICADO: 'Duplicado',
  CONFLICTO: 'Conflicto',
  RECHAZADO: 'Rechazado',
}

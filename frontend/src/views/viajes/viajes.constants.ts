import type { FilterOption } from '@/components/toolbar/FilterSelect'
import type { EstadoViaje } from '@/api/viajes/viajes.types'

export const NOMBRES_ESTADO: Record<EstadoViaje, string> = {
  ASIGNADO: 'Asignado',
  EN_CURSO: 'En curso',
  FINALIZADO: 'Finalizado',
}

export const OPCIONES_ESTADO: FilterOption[] = Object.entries(NOMBRES_ESTADO).map(([value, label]) => ({
  value,
  label,
}))

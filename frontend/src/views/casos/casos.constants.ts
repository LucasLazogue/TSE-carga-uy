import type { ComponentProps } from 'react'
import type { FilterOption } from '@/components/toolbar/FilterSelect'
import type { Badge } from '@/components/ui/badge'
import type { AccionAuditoria, EstadoCaso, TipoInfraccion } from '@/api/casos/casos.types'

export const NOMBRES_ESTADO: Record<EstadoCaso, string> = {
  DETECTADA: 'Detectada',
  NOTIFICADA: 'Notificada',
  EN_DESCARGO: 'En descargo',
  CONFIRMADA: 'Confirmada',
  DESESTIMADA: 'Desestimada',
}

export const VARIANTES_ESTADO: Record<EstadoCaso, ComponentProps<typeof Badge>['variant']> = {
  DETECTADA: 'outline',
  NOTIFICADA: 'outline',
  EN_DESCARGO: 'default',
  CONFIRMADA: 'destructive',
  DESESTIMADA: 'secondary',
}

export const OPCIONES_ESTADO: FilterOption[] = Object.entries(NOMBRES_ESTADO).map(([value, label]) => ({
  value,
  label,
}))

export const NOMBRES_TIPO: Record<TipoInfraccion, string> = {
  EXCESO_CARGA: 'Exceso de carga',
  SIN_HABILITACION: 'Sin habilitación',
  VIAJE_SIN_GUIA: 'Viaje sin guía',
  DESVIO_RUTA: 'Desvío de ruta',
}

export const OPCIONES_TIPO: FilterOption[] = Object.entries(NOMBRES_TIPO).map(([value, label]) => ({
  value,
  label,
}))

export const NOMBRES_ACCION: Record<AccionAuditoria, string> = {
  CREAR_CASO: 'Alta del caso',
  NOTIFICAR: 'Notificación',
  PRESENTAR_DESCARGO: 'Descargo',
  CONFIRMAR: 'Confirmación',
  DESESTIMAR: 'Desestimación',
}

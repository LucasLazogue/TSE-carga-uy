import type { FilterOption } from '@/components/toolbar/FilterSelect'
import type { AccionAuditoria, Caso, EstadoCaso, TipoEvidencia, TipoInfraccion } from '@/api/casos/casos.types'

export const ESTADOS_CASO: Record<EstadoCaso, string> = {
  DETECTADA: 'Detectada',
  NOTIFICADA: 'Notificada',
  EN_DESCARGO: 'En descargo',
  CONFIRMADA: 'Confirmada',
  DESESTIMADA: 'Desestimada',
}

export const TIPOS_INFRACCION: Record<TipoInfraccion, string> = {
  EXCESO_CARGA: 'Exceso de carga',
  SIN_HABILITACION: 'Sin habilitación',
  VIAJE_SIN_GUIA: 'Viaje sin guía',
  DESVIO_RUTA: 'Desvío de ruta',
}

export const TIPOS_EVIDENCIA: Record<TipoEvidencia, string> = {
  PESAJE: 'Pesaje',
  POSICION: 'Posición',
  VIAJE: 'Viaje',
  PERMISO: 'Permiso',
  ITV: 'ITV',
}

export const ACCIONES_AUDITORIA: Record<AccionAuditoria, string> = {
  CREAR_CASO: 'Alta del caso',
  NOTIFICAR: 'Notificación',
  PRESENTAR_DESCARGO: 'Descargo',
  CONFIRMAR: 'Confirmación',
  DESESTIMAR: 'Desestimación',
}

function opciones(labels: Record<string, string>): FilterOption[] {
  return Object.entries(labels).map(([value, label]) => ({ value, label }))
}

export const OPCIONES_ESTADO = opciones(ESTADOS_CASO)

export const OPCIONES_TIPO = opciones(TIPOS_INFRACCION)

export const LARGO_MAXIMO_TEXTO = 4000

export function fechaHora(iso?: string | null) {
  if (!iso) {
    return '—'
  }
  const [fecha, hora] = iso.split('T')
  const [anio, mes, dia] = fecha.split('-')
  return `${dia}/${mes}/${anio} ${hora.slice(0, 5)}`
}

function plazoVencido(caso: Caso) {
  return !!caso.venceDescargo && new Date(caso.venceDescargo).getTime() < Date.now()
}

export function puedeDescargar(caso: Caso) {
  return caso.estado === 'NOTIFICADA' && !!caso.venceDescargo && !plazoVencido(caso)
}

// sin descargo, el caso se resuelve recien cuando vence el plazo
export function puedeResolver(caso: Caso) {
  return caso.estado === 'EN_DESCARGO' || (caso.estado === 'NOTIFICADA' && plazoVencido(caso))
}

export function plazoRestante(caso: Caso) {
  if (caso.estado !== 'NOTIFICADA' || !caso.venceDescargo) {
    return null
  }
  const restante = new Date(caso.venceDescargo).getTime() - Date.now()
  if (restante <= 0) {
    return 'Vencido'
  }
  const horas = Math.ceil(restante / 3_600_000)
  if (horas < 24) {
    return horas === 1 ? '1 hora' : `${horas} horas`
  }
  const dias = Math.ceil(restante / 86_400_000)
  return dias === 1 ? '1 día' : `${dias} días`
}

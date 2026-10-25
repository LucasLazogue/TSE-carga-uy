export type EstadoCaso = 'DETECTADA' | 'NOTIFICADA' | 'EN_DESCARGO' | 'CONFIRMADA' | 'DESESTIMADA'

export type TipoInfraccion = 'EXCESO_CARGA' | 'SIN_HABILITACION' | 'VIAJE_SIN_GUIA' | 'DESVIO_RUTA'

export type AccionAuditoria = 'CREAR_CASO' | 'NOTIFICAR' | 'PRESENTAR_DESCARGO' | 'CONFIRMAR' | 'DESESTIMAR'

export type Evidencia = {
  id: number
  fechaEvento: string
  valorObservado: string | null
  valorDeclarado: string | null
  descripcion: string | null
}

export type Descargo = {
  texto: string
  fecha: string
  cedulaPresentadoPor: string
}

export type Resolucion = {
  resultado: EstadoCaso
  fundamento: string
  fecha: string
}

export type Caso = {
  id: number
  tipo: TipoInfraccion
  estado: EstadoCaso
  fechaDeteccion: string
  venceDescargo: string | null
  matricula: string
  nombreEmpresa: string
  evidencias: Evidencia[]
  descargo: Descargo | null
  resolucion: Resolucion | null
}

export type ResolucionNueva = Pick<Resolucion, 'resultado' | 'fundamento'>

export type PistaAuditoria = {
  id: number
  fecha: string
  accion: AccionAuditoria
  estadoNuevo: EstadoCaso
  usuario: string
  fundamento: string | null
}

export type FiltroCasos = {
  estado?: EstadoCaso
  tipo?: TipoInfraccion
  empresa?: number
}

export type EstadoCaso = 'DETECTADA' | 'NOTIFICADA' | 'EN_DESCARGO' | 'CONFIRMADA' | 'DESESTIMADA'

export type TipoInfraccion = 'EXCESO_CARGA' | 'SIN_HABILITACION' | 'VIAJE_SIN_GUIA' | 'DESVIO_RUTA'

export type TipoEvidencia = 'PESAJE' | 'POSICION' | 'VIAJE' | 'PERMISO' | 'ITV'

export type AccionAuditoria = 'CREAR_CASO' | 'NOTIFICAR' | 'PRESENTAR_DESCARGO' | 'CONFIRMAR' | 'DESESTIMAR'

export type Evidencia = {
  id: number
  tipo: TipoEvidencia
  idOrigen?: string
  fechaEvento: string
  valorObservado?: string
  valorDeclarado?: string
  umbral?: string
  descripcion?: string
}

export type Descargo = {
  id: number
  texto: string
  fecha: string
  cedulaPresentadoPor: string
}

export type Resolucion = {
  id: number
  resultado: EstadoCaso
  fundamento: string
  fecha: string
  cedulaFuncionario?: string
}

export type ResolucionNueva = {
  resultado: EstadoCaso
  fundamento: string
}

export type Caso = {
  id: number
  tipo: TipoInfraccion
  estado: EstadoCaso
  fechaHecho: string
  fechaDeteccion: string
  fechaNotificacion?: string | null
  venceDescargo?: string | null
  idVehiculo: number
  matricula: string
  idEmpresa: number
  nombreEmpresa: string
  idViaje?: number | null
  evidencias: Evidencia[]
  descargo?: Descargo | null
  resolucion?: Resolucion | null
}

export type PistaAuditoria = {
  id: number
  idCaso: number
  usuario: string
  rol: string
  accion: AccionAuditoria
  estadoAnterior?: EstadoCaso | null
  estadoNuevo: EstadoCaso
  fundamento?: string
  fecha: string
}

export type FiltroCasos = {
  estado?: string
  tipo?: string
  empresa?: string
  desde?: string
  hasta?: string
}

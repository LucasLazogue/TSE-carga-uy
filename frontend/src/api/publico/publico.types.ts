export type HabilitacionEmpresa = {
  nombrePublico: string
  razonSocial: string
  habilitada: boolean
  fecha: string
}

export type ConsultaHabilitacion = { nro: number } | { razonSocial: string }

export type ViajesPorMes = {
  mes: string
  cantidad?: number
}

export type VolumenPorRubro = {
  rubro: string
  volumen?: number
}

export type IndicadoresPublicos = {
  umbral: number
  habilitadosAl: string
  empresasHabilitadas?: number
  vehiculosHabilitados?: number
  viajesPorMes: ViajesPorMes[]
  volumenPorRubro: VolumenPorRubro[]
}

export type FiltroIndicadores = {
  desde: string
  hasta: string
  rubro?: number
  tipoCarga?: number
}

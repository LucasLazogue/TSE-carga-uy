export type Guia = {
  id: number
  nroGuia: string
  fecha: string
  origenLat: number
  origenLon: number
  destinoLat: number
  destinoLon: number
  volumen: number
  idEmpresa: number
  nombreEmpresa: string
  idRegistradaPor: number
  idRubro: number
  nombreRubro: string
  idTipoCarga: number
  nombreTipoCarga: string
  idViaje: number | null
}

export type GuiaNueva = Omit<Guia, 'id' | 'nroGuia' | 'nombreEmpresa' | 'nombreRubro' | 'nombreTipoCarga' | 'idViaje'>

export type Rubro = {
  id: number
  nombre: string
}

export type TipoCarga = {
  id: number
  nombre: string
}

export type FiltroGuias = {
  busqueda?: string
  conViaje?: boolean
  desde?: string
  hasta?: string
}

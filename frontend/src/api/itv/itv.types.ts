export type Itv = {
  id: number
  fechaInspeccion: string
  fechaVencimiento: string
}

export type ItvNueva = Omit<Itv, 'id'>

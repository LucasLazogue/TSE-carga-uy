export type Page<T> = {
  items: T[]
  total: number
  pagina: number
  tamanio: number
}

export type Paginacion = {
  pagina?: number
  tamanio?: number
}

export const TAMANIO_MAXIMO = 100

export type Vehiculo = {
  id: number
  matricula: string
  marca: string
  modelo: string
  pesoVehiculo: number
  capacidadCarga: number
  idEmpresa: number
  nombreEmpresa: string
}

export type VehiculoNuevo = Omit<Vehiculo, 'id' | 'nombreEmpresa'>

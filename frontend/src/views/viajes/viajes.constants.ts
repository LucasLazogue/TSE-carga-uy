import type { EstadoViaje, ViajeNuevo } from '@/api/viajes/viajes.types'

export const VIAJE_VACIO: ViajeNuevo = {
  idGuia: 0,
  idVehiculo: 0,
  idChofer: 0,
}

export const ESTADOS_VIAJE: EstadoViaje[] = ['ASIGNADO', 'EN_CURSO', 'FINALIZADO']

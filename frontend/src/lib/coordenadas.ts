import type { LatLngTuple } from 'leaflet'

export function coordenadas(lat: number, lon: number) {
  return `${lat.toFixed(4)}, ${lon.toFixed(4)}`
}

// un campo vacio vale 0: el punto existe recien cuando estan las dos coordenadas y son validas
export function punto(lat: number, lon: number): LatLngTuple | null {
  return lat && lon && Math.abs(lat) <= 90 && Math.abs(lon) <= 180 ? [lat, lon] : null
}

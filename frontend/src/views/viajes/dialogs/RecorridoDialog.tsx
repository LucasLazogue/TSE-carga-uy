import { useEffect, useState } from 'react'
import { AlertCircle } from 'lucide-react'
import type { LatLngTuple } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { CircleMarker, MapContainer, Polyline, TileLayer, Tooltip } from 'react-leaflet'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { GuiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import { ViajesApi } from '@/api/viajes/viajes.api'
import type { Pesaje, PesajesViaje, Posicion, Viaje } from '@/api/viajes/viajes.types'
import { fechaHora, momento } from '@/lib/fechas'
import { NOMBRES_ESTADO } from '../viajes.constants'

const columnas: GridColumn<Pesaje>[] = [
  { header: 'Fecha', cell: (p) => fechaHora(p.fechaHora) },
  { header: 'Peaje', cell: (p) => p.nombrePeaje, className: 'font-medium' },
  { header: 'Balanza', cell: (p) => p.codigoBalanza },
  { header: 'Peso', cell: (p) => `${p.pesoRegistrado.toLocaleString('es-UY')} kg` },
]

// "hace 5 min", "hace 3 h" o "hace 2 días"
function hace(iso: string) {
  const minutos = Math.round((Date.now() - new Date(iso).getTime()) / 60000)
  if (minutos < 60) {
    return `hace ${minutos} min`
  }
  const horas = Math.round(minutos / 60)
  if (horas < 48) {
    return `hace ${horas} h`
  }
  return `hace ${Math.round(horas / 24)} días`
}

type Props = {
  idEmpresa: number
  viaje: Viaje
  close: () => void
}

function RecorridoDialog({ idEmpresa, viaje, close }: Props) {
  const [guia, setGuia] = useState<Guia | null>(null)
  const [posiciones, setPosiciones] = useState<Posicion[]>([])
  const [pesajes, setPesajes] = useState<PesajesViaje | null>(null)

  useEffect(() => {
    let vigente = true
    Promise.all([
      GuiasApi.find(idEmpresa, viaje.idGuia),
      ViajesApi.findPosiciones(idEmpresa, viaje.id),
      ViajesApi.findPesajes(idEmpresa, viaje.id),
    ]).then(([guiaViaje, posicionesViaje, pesajesViaje]) => {
      if (vigente) {
        setGuia(guiaViaje)
        setPosiciones(posicionesViaje)
        setPesajes(pesajesViaje)
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa, viaje.idGuia, viaje.id])

  if (!guia || !pesajes) {
    return null
  }

  const origen: LatLngTuple = [guia.origenLat, guia.origenLon]
  const destino: LatLngTuple = [guia.destinoLat, guia.destinoLon]
  const ruta: LatLngTuple[] = posiciones.map((p) => [p.latitud, p.longitud])
  const ultima = posiciones.at(-1)

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Guía {viaje.nroGuia}, vehículo {viaje.matricula}, {NOMBRES_ESTADO[viaje.estado].toLowerCase()}.{' '}
        {ultima
          ? `Última posición: ${momento(ultima.timestampGeneracion)} (${hace(ultima.timestampGeneracion)}).`
          : 'Todavía no hay posiciones de este viaje.'}
      </p>
      <div className="isolate h-80 overflow-hidden rounded-lg border">
        <MapContainer bounds={[origen, destino, ...ruta]} boundsOptions={{ padding: [20, 20] }} className="h-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Polyline positions={ruta} />
          <CircleMarker center={origen} pathOptions={{ color: 'green' }}>
            <Tooltip>Origen</Tooltip>
          </CircleMarker>
          <CircleMarker center={destino} pathOptions={{ color: 'red' }}>
            <Tooltip>Destino</Tooltip>
          </CircleMarker>
          {ultima && (
            <CircleMarker center={[ultima.latitud, ultima.longitud]} pathOptions={{ color: 'blue', fillOpacity: 1 }}>
              <Tooltip>Última posición</Tooltip>
            </CircleMarker>
          )}
        </MapContainer>
      </div>
      <div className="space-y-2">
        <p className="text-sm font-medium">Pesajes</p>
        {!pesajes.completo && (
          <Alert>
            <AlertCircle />
            <AlertDescription>
              No respondieron: {pesajes.nodosSinResponder.join(', ')}. Los pesajes pueden estar incompletos.
            </AlertDescription>
          </Alert>
        )}
        <Grid
          columns={columnas}
          items={[...pesajes.pesajes].sort((a, b) => a.fechaHora.localeCompare(b.fechaHora))}
          rowKey={(p) => p.idOrigen}
          empty="No hay pesajes de este viaje."
        />
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cerrar
        </Button>
      </DialogFooter>
    </div>
  )
}

export default RecorridoDialog

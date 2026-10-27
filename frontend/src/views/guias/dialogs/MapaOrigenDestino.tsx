import { useState } from 'react'
import type { KeyboardEvent } from 'react'
import type { LatLngTuple, Map as MapaLeaflet } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { MapPin } from 'lucide-react'
import { CircleMarker, MapContainer, Polyline, TileLayer, Tooltip, useMapEvents } from 'react-leaflet'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import type { GuiaNueva } from '@/api/guias/guias.types'
import { punto } from '@/lib/coordenadas'

export type Ubicaciones = Pick<GuiaNueva, 'origenLat' | 'origenLon' | 'destinoLat' | 'destinoLon'>

type Punto = 'origen' | 'destino'

// vista inicial cuando la guia todavia no tiene puntos
const URUGUAY: LatLngTuple = [-32.8, -56.0]

// 6 decimales son unos 10 cm: el clic no necesita mas
function redondear(valor: number) {
  return Number(valor.toFixed(6))
}

function AlHacerClic({ onClic }: { onClic: (lugar: LatLngTuple) => void }) {
  useMapEvents({ click: (e) => onClic([redondear(e.latlng.lat), redondear(e.latlng.lng)]) })
  return null
}

type Props = {
  valor: Ubicaciones
  onChange: (valor: Ubicaciones) => void
}

function MapaOrigenDestino({ valor, onChange }: Props) {
  const origen = punto(valor.origenLat, valor.origenLon)
  const destino = punto(valor.destinoLat, valor.destinoLon)
  const [marcando, setMarcando] = useState<Punto>(origen && !destino ? 'destino' : 'origen')
  const [mapa, setMapa] = useState<MapaLeaflet | null>(null)

  // center, zoom y bounds solo se leen al crear el mapa: marcar un punto no lo mueve
  const [vista] = useState(() =>
    origen && destino
      ? { bounds: [origen, destino], boundsOptions: { padding: [30, 30] as [number, number] } }
      : { center: origen ?? URUGUAY, zoom: origen ? 10 : 6 },
  )

  function marcar([lat, lon]: LatLngTuple) {
    if (marcando === 'origen') {
      onChange({ ...valor, origenLat: lat, origenLon: lon })
      if (!destino) {
        setMarcando('destino')
      }
    } else {
      onChange({ ...valor, destinoLat: lat, destinoLon: lon })
    }
  }

  function ir(lugar: LatLngTuple | null) {
    if (lugar && mapa) {
      mapa.flyTo(lugar, Math.max(mapa.getZoom(), 12))
    }
  }

  function fila(nombre: Punto, lat: number, lon: number, lugar: LatLngTuple | null) {
    const latitud = nombre === 'origen' ? 'origenLat' : 'destinoLat'
    const longitud = nombre === 'origen' ? 'origenLon' : 'destinoLon'
    // Enter va al punto en vez de enviar el formulario de la guia
    const alApretar = (e: KeyboardEvent) => {
      if (e.key === 'Enter') {
        e.preventDefault()
        ir(lugar)
      }
    }
    return (
      <div className="flex flex-wrap items-center gap-2">
        <Button
          type="button"
          size="sm"
          className="w-36"
          variant={marcando === nombre ? 'default' : 'outline'}
          onClick={() => setMarcando(nombre)}
        >
          Marcar {nombre}
        </Button>
        <Input
          aria-label={`Latitud del ${nombre}`}
          placeholder="Latitud"
          type="number"
          step="any"
          className="w-36 flex-1"
          value={lat || ''}
          onChange={(e) => onChange({ ...valor, [latitud]: Number(e.target.value) })}
          onKeyDown={alApretar}
        />
        <Input
          aria-label={`Longitud del ${nombre}`}
          placeholder="Longitud"
          type="number"
          step="any"
          className="w-36 flex-1"
          value={lon || ''}
          onChange={(e) => onChange({ ...valor, [longitud]: Number(e.target.value) })}
          onKeyDown={alApretar}
        />
        <Button type="button" size="sm" variant="outline" disabled={!lugar} onClick={() => ir(lugar)}>
          <MapPin />
          Ir
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {fila('origen', valor.origenLat, valor.origenLon, origen)}
      {fila('destino', valor.destinoLat, valor.destinoLon, destino)}
      <div className="isolate h-72 overflow-hidden rounded-lg border">
        <MapContainer ref={setMapa} {...vista} className="h-full">
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <AlHacerClic onClic={marcar} />
          {origen && destino && <Polyline positions={[origen, destino]} pathOptions={{ dashArray: '6 8' }} />}
          {origen && (
            <CircleMarker center={origen} pathOptions={{ color: 'green' }}>
              <Tooltip>Origen</Tooltip>
            </CircleMarker>
          )}
          {destino && (
            <CircleMarker center={destino} pathOptions={{ color: 'red' }}>
              <Tooltip>Destino</Tooltip>
            </CircleMarker>
          )}
        </MapContainer>
      </div>
      <p className="text-sm text-muted-foreground">
        Escribí las coordenadas y tocá Ir, o elegí qué marcar y hacé clic en el mapa.
      </p>
    </div>
  )
}

export default MapaOrigenDestino

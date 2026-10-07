import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { guiasApi } from '@/api/guias/guias.api'
import type { GuiaNueva, Rubro, TipoCarga } from '@/api/guias/guias.types'
import { useSession } from '@/auth/useSession'
import { GUIA_VACIA } from '../guias.constants'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  empresas: Empresa[]
  close: () => void
  onCreated: () => void
}

function NuevaGuiaDialog({ empresas, close, onCreated }: Props) {
  const { session } = useSession()
  const [rubros, setRubros] = useState<Rubro[]>([])
  const [tiposCarga, setTiposCarga] = useState<TipoCarga[]>([])
  const [nueva, setNueva] = useState<GuiaNueva>(GUIA_VACIA)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    guiasApi.getRubros().then(setRubros)
    guiasApi.getTiposCarga().then(setTiposCarga)
  }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await guiasApi.create({ ...nueva, idRegistradaPor: session?.id ?? 0 })
      onCreated()
      close()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form onSubmit={guardar} className="space-y-4">
      {error && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="empresa">Empresa</Label>
          <select
            id="empresa"
            className={selectClass}
            value={nueva.idEmpresa}
            onChange={(e) => setNueva({ ...nueva, idEmpresa: Number(e.target.value) })}
          >
            <option value={0}>Seleccione una empresa</option>
            {empresas.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nombrePublico}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="rubro">Rubro del cliente</Label>
          <select
            id="rubro"
            className={selectClass}
            value={nueva.idRubro}
            onChange={(e) => setNueva({ ...nueva, idRubro: Number(e.target.value) })}
          >
            <option value={0}>Seleccione un rubro</option>
            {rubros.map((r) => (
              <option key={r.id} value={r.id}>
                {r.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="tipoCarga">Tipo de carga</Label>
          <select
            id="tipoCarga"
            className={selectClass}
            value={nueva.idTipoCarga}
            onChange={(e) => setNueva({ ...nueva, idTipoCarga: Number(e.target.value) })}
          >
            <option value={0}>Seleccione un tipo de carga</option>
            {tiposCarga.map((t) => (
              <option key={t.id} value={t.id}>
                {t.nombre}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="volumen">Volumen (kg)</Label>
          <Input
            id="volumen"
            type="number"
            value={nueva.volumen || ''}
            onChange={(e) => setNueva({ ...nueva, volumen: Number(e.target.value) })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="fecha">Fecha</Label>
          <Input
            id="fecha"
            type="date"
            value={nueva.fecha}
            onChange={(e) => setNueva({ ...nueva, fecha: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="origenLat">Origen (latitud, longitud)</Label>
          <div className="flex gap-2">
            <Input
              id="origenLat"
              type="number"
              step="any"
              value={nueva.origenLat || ''}
              onChange={(e) => setNueva({ ...nueva, origenLat: Number(e.target.value) })}
            />
            <Input
              aria-label="Origen longitud"
              type="number"
              step="any"
              value={nueva.origenLon || ''}
              onChange={(e) => setNueva({ ...nueva, origenLon: Number(e.target.value) })}
            />
          </div>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="destinoLat">Destino (latitud, longitud)</Label>
          <div className="flex gap-2">
            <Input
              id="destinoLat"
              type="number"
              step="any"
              value={nueva.destinoLat || ''}
              onChange={(e) => setNueva({ ...nueva, destinoLat: Number(e.target.value) })}
            />
            <Input
              aria-label="Destino longitud"
              type="number"
              step="any"
              value={nueva.destinoLon || ''}
              onChange={(e) => setNueva({ ...nueva, destinoLon: Number(e.target.value) })}
            />
          </div>
        </div>
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          Guardar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default NuevaGuiaDialog

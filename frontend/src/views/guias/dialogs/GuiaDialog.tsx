import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import { TAMANIO_MAXIMO } from '@/api/page'
import type { Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { GuiasApi } from '@/api/guias/guias.api'
import type { Guia, GuiaNueva, Rubro, TipoCarga } from '@/api/guias/guias.types'
import { useSession } from '@/auth/useSession'
import { punto } from '@/lib/coordenadas'
import { GUIA_VACIA } from '../guias.constants'
import MapaOrigenDestino from './MapaOrigenDestino'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50'

type Props = {
  idEmpresa: number
  // sin guia es un alta
  guia?: Guia
  close: () => void
  onSaved: (guia: Guia) => void
}

function GuiaDialog({ idEmpresa, guia, close, onSaved }: Props) {
  const { session } = useSession()
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [rubros, setRubros] = useState<Rubro[]>([])
  const [tiposCarga, setTiposCarga] = useState<TipoCarga[]>([])
  const [nueva, setNueva] = useState<GuiaNueva>(guia ?? { ...GUIA_VACIA, idEmpresa })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    EmpresasApi.findAll({}, { tamanio: TAMANIO_MAXIMO }).then((page) => setEmpresas(page.items))
    GuiasApi.getRubros().then(setRubros)
    GuiasApi.getTiposCarga().then(setTiposCarga)
  }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault()
    // el central rechaza origen o destino nulos, pero un campo vacio llega como 0 y lo tomaria como valido
    if (!punto(nueva.origenLat, nueva.origenLon)) {
      setError('Indicá un origen válido: escribí sus coordenadas o marcalo en el mapa.')
      return
    }
    if (!punto(nueva.destinoLat, nueva.destinoLon)) {
      setError('Indicá un destino válido: escribí sus coordenadas o marcalo en el mapa.')
      return
    }
    setError('')
    setGuardando(true)
    try {
      onSaved(
        guia
          ? await GuiasApi.update(guia.idEmpresa, guia.id, nueva)
          : await GuiasApi.create(nueva.idEmpresa, { ...nueva, idRegistradaPor: session?.id ?? 0 }),
      )
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
            disabled={guia != null}
            onChange={(e) => setNueva({ ...nueva, idEmpresa: Number(e.target.value) })}
          >
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
        <div className="space-y-1.5 sm:col-span-2">
          <p className="text-sm font-medium">Origen y destino</p>
          <MapaOrigenDestino valor={nueva} onChange={(ubicaciones) => setNueva({ ...nueva, ...ubicaciones })} />
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

export default GuiaDialog

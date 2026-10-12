import { useCallback, useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle, ChevronsUpDown } from 'lucide-react'
import { cn } from 'cn'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import Lookup from '@/components/lookup/Lookup'
import type { LookupOption } from '@/components/lookup/Lookup'
import { choferesApi } from '@/api/choferes/choferes.api'
import type { Chofer } from '@/api/choferes/choferes.types'
import { getErrorMessage } from '@/api/errors'
import type { Guia } from '@/api/guias/guias.types'
import { TAMANIO_MAXIMO } from '@/api/page'
import { vehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { viajesApi } from '@/api/viajes/viajes.api'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  guia: Guia
  close: () => void
  onAssigned: () => void
}

function opcionChofer(c: Chofer): LookupOption {
  return { value: String(c.id), label: c.correo ? `${c.cedula} - ${c.correo}` : c.cedula }
}

function AsignarViajeDialog({ guia, close, onAssigned }: Props) {
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [idVehiculo, setIdVehiculo] = useState(0)
  const [idChofer, setIdChofer] = useState<string>()
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const conocidos = useRef(new Map<string, LookupOption>())

  useEffect(() => {
    vehiculosApi.getAll(guia.idEmpresa, { tamanio: TAMANIO_MAXIMO }).then((page) => setVehiculos(page.items))
  }, [guia.idEmpresa])

  // solo los choferes vigentes a la fecha de la guia, que son los que acepta la asignacion
  const buscarChoferes = useCallback(
    async (texto: string) => {
      const { items } = await choferesApi.getAll(guia.idEmpresa, { cedula: texto, fecha: guia.fecha })
      const opciones = items.map(opcionChofer)
      opciones.forEach((o) => conocidos.current.set(o.value, o))
      return opciones
    },
    [guia.idEmpresa, guia.fecha],
  )

  const choferPorId = useCallback(async (id: string) => conocidos.current.get(id) ?? null, [])

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await viajesApi.create(guia.idEmpresa, { idGuia: guia.id, idVehiculo, idChofer: Number(idChofer ?? 0) })
      onAssigned()
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
      <p className="text-sm text-muted-foreground">
        Guía {guia.nroGuia} del {guia.fecha}, {guia.volumen.toLocaleString('es-UY')} kg.
      </p>
      <div className="space-y-1.5">
        <Label htmlFor="vehiculo">Vehículo</Label>
        <select
          id="vehiculo"
          className={selectClass}
          value={idVehiculo}
          onChange={(e) => setIdVehiculo(Number(e.target.value))}
        >
          <option value={0}>Seleccione un vehículo</option>
          {vehiculos.map((v) => (
            <option key={v.id} value={v.id}>
              {v.matricula} - {v.marca} {v.modelo} ({v.capacidadCarga.toLocaleString('es-UY')} kg)
            </option>
          ))}
        </select>
      </div>
      <div className="space-y-1.5">
        <Label>Chofer</Label>
        <Lookup
          value={idChofer}
          onChange={setIdChofer}
          search={buscarChoferes}
          resolve={choferPorId}
          required
          placeholder="Seleccione un chofer"
          searchPlaceholder="Buscar por cédula..."
          trigger={(seleccion) => (
            <Button type="button" variant="outline" role="combobox" className="w-full justify-between font-normal">
              <span className={cn('truncate', !seleccion && 'text-muted-foreground')}>
                {seleccion?.label ?? 'Seleccione un chofer'}
              </span>
              <ChevronsUpDown className="opacity-50" />
            </Button>
          )}
        />
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          Asignar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default AsignarViajeDialog

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
import { ChoferesApi } from '@/api/choferes/choferes.api'
import type { Chofer } from '@/api/choferes/choferes.types'
import { getErrorMessage } from '@/api/errors'
import type { Guia } from '@/api/guias/guias.types'
import { TAMANIO_MAXIMO } from '@/api/page'
import { VehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { ViajesApi } from '@/api/viajes/viajes.api'
import type { Viaje } from '@/api/viajes/viajes.types'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  guia: Guia
  viaje?: Viaje
  close: () => void
  onAssigned: () => void
}

function opcionChofer(c: Chofer): LookupOption {
  return { value: String(c.id), label: c.correo ? `${c.cedula} - ${c.correo}` : c.cedula }
}

// el chofer actual se precarga para que el lookup lo muestre sin tener que buscarlo
function choferActual(viaje?: Viaje) {
  const conocidos = new Map<string, LookupOption>()
  if (viaje) {
    conocidos.set(String(viaje.idChofer), { value: String(viaje.idChofer), label: viaje.cedulaChofer })
  }
  return conocidos
}

function AsignarViajeDialog({ guia, viaje, close, onAssigned }: Props) {
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [idVehiculo, setIdVehiculo] = useState(viaje?.idVehiculo ?? 0)
  const [idChofer, setIdChofer] = useState(viaje ? String(viaje.idChofer) : undefined)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const conocidos = useRef(choferActual(viaje))

  useEffect(() => {
    VehiculosApi.findAll({ idEmpresa: guia.idEmpresa }, { tamanio: TAMANIO_MAXIMO }).then((page) => setVehiculos(page.items))
  }, [guia.idEmpresa])

  // solo los choferes vigentes a la fecha de la guia y sin baja, que son los que acepta la asignacion
  const buscarChoferes = useCallback(
    async (texto: string) => {
      const { items } = await ChoferesApi.findAll(guia.idEmpresa, { cedula: texto, fecha: guia.fecha })
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
      const datos = { idGuia: guia.id, idVehiculo, idChofer: Number(idChofer ?? 0) }
      if (viaje) {
        await ViajesApi.update(guia.idEmpresa, viaje.id, datos)
      } else {
        await ViajesApi.create(guia.idEmpresa, datos)
      }
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
          {viaje ? 'Reasignar' : 'Asignar'}
        </Button>
      </DialogFooter>
    </form>
  )
}

export default AsignarViajeDialog

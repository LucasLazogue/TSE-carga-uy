import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { getErrorMessage } from '@/api/errors'
import { PermisosApi } from '@/api/permisos/permisos.api'
import type { PermisoNuevo } from '@/api/permisos/permisos.types'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { PERMISO_VACIO } from '../permisos.constants'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  vehiculos: Vehiculo[]
  idVehiculo: number
  close: () => void
  onCreated: (idVehiculo: number) => void
}

function NuevoPermisoDialog({ vehiculos, idVehiculo, close, onCreated }: Props) {
  const [nuevo, setNuevo] = useState<PermisoNuevo>({ ...PERMISO_VACIO, idVehiculo })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await PermisosApi.create(nuevo)
      onCreated(nuevo.idVehiculo)
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
      <div className="space-y-1.5">
        <Label htmlFor="vehiculo">Vehículo</Label>
        <select
          id="vehiculo"
          className={selectClass}
          value={nuevo.idVehiculo}
          onChange={(e) => setNuevo({ ...nuevo, idVehiculo: Number(e.target.value) })}
        >
          <option value={0}>Seleccione un vehículo</option>
          {vehiculos.map((v) => (
            <option key={v.id} value={v.id}>
              {v.matricula}
            </option>
          ))}
        </select>
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="nroPermiso">Nro. permiso</Label>
        <Input
          id="nroPermiso"
          value={nuevo.nroPermiso}
          onChange={(e) => setNuevo({ ...nuevo, nroPermiso: e.target.value })}
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="validoDesde">Válido desde</Label>
          <Input
            id="validoDesde"
            type="date"
            value={nuevo.validoDesde}
            onChange={(e) => setNuevo({ ...nuevo, validoDesde: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="validoHasta">Válido hasta</Label>
          <Input
            id="validoHasta"
            type="date"
            value={nuevo.validoHasta}
            onChange={(e) => setNuevo({ ...nuevo, validoHasta: e.target.value })}
          />
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

export default NuevoPermisoDialog

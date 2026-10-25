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
import type { Permiso, PermisoNuevo } from '@/api/permisos/permisos.types'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { PERMISO_VACIO } from '../permisos.constants'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  vehiculos: Vehiculo[]
  idVehiculo: number
  permiso?: Permiso
  close: () => void
  onSaved: (idVehiculo: number) => void
}

function SolicitudPermisoDialog({ vehiculos, idVehiculo, permiso, close, onSaved }: Props) {
  const [datos, setDatos] = useState<PermisoNuevo>(
    permiso
      ? { nroPermiso: permiso.nroPermiso, validoDesde: permiso.validoDesde, idVehiculo: permiso.idVehiculo }
      : { ...PERMISO_VACIO, idVehiculo },
  )
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      if (permiso) {
        await PermisosApi.update(permiso.id, datos)
      } else {
        await PermisosApi.create(datos)
      }
      onSaved(datos.idVehiculo)
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
      {permiso?.motivoRechazo && (
        <Alert>
          <AlertCircle />
          <AlertDescription>Motivo del rechazo: {permiso.motivoRechazo}</AlertDescription>
        </Alert>
      )}
      <div className="space-y-1.5">
        <Label htmlFor="vehiculo">Vehículo</Label>
        <select
          id="vehiculo"
          className={selectClass}
          value={datos.idVehiculo}
          onChange={(e) => setDatos({ ...datos, idVehiculo: Number(e.target.value) })}
        >
          <option value={0}>Seleccione un vehículo</option>
          {vehiculos.map((v) => (
            <option key={v.id} value={v.id}>
              {v.matricula}
            </option>
          ))}
        </select>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="nroPermiso">Nro. permiso</Label>
          <Input
            id="nroPermiso"
            value={datos.nroPermiso}
            onChange={(e) => setDatos({ ...datos, nroPermiso: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="validoDesde">Válido desde</Label>
          <Input
            id="validoDesde"
            type="date"
            value={datos.validoDesde}
            onChange={(e) => setDatos({ ...datos, validoDesde: e.target.value })}
          />
        </div>
      </div>
      <p className="text-sm text-muted-foreground">
        El permiso queda pendiente hasta que lo apruebe un funcionario, que define la fecha de fin.
      </p>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          {permiso ? 'Reenviar' : 'Enviar'}
        </Button>
      </DialogFooter>
    </form>
  )
}

export default SolicitudPermisoDialog

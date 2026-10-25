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
import type { AprobacionPermiso, Permiso } from '@/api/permisos/permisos.types'

type Props = {
  permiso: Permiso
  close: () => void
  onSaved: () => void
}

function AprobarPermisoDialog({ permiso, close, onSaved }: Props) {
  const [datos, setDatos] = useState<AprobacionPermiso>({ validoDesde: permiso.validoDesde, validoHasta: '' })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await PermisosApi.aprobar(permiso.id, datos)
      onSaved()
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
        Permiso {permiso.nroPermiso} del vehículo {permiso.matricula}.
      </p>
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="validoDesde">Válido desde</Label>
          <Input
            id="validoDesde"
            type="date"
            value={datos.validoDesde}
            onChange={(e) => setDatos({ ...datos, validoDesde: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="validoHasta">Válido hasta</Label>
          <Input
            id="validoHasta"
            type="date"
            min={datos.validoDesde}
            value={datos.validoHasta}
            onChange={(e) => setDatos({ ...datos, validoHasta: e.target.value })}
          />
        </div>
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando || !datos.validoHasta}>
          Aprobar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default AprobarPermisoDialog

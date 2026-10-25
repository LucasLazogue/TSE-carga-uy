import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { getErrorMessage } from '@/api/errors'
import { PermisosApi } from '@/api/permisos/permisos.api'
import type { Permiso } from '@/api/permisos/permisos.types'

type Props = {
  permiso: Permiso
  close: () => void
  onSaved: () => void
}

function RechazarPermisoDialog({ permiso, close, onSaved }: Props) {
  const [motivo, setMotivo] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await PermisosApi.rechazar(permiso.id, motivo)
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
      <div className="space-y-1.5">
        <Label htmlFor="motivo">Motivo del rechazo</Label>
        <Textarea id="motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" variant="destructive" disabled={guardando || !motivo.trim()}>
          Rechazar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default RechazarPermisoDialog

import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import { getErrorMessage } from '@/api/errors'
import { PermisosApi } from '@/api/permisos/permisos.api'
import type { Permiso } from '@/api/permisos/permisos.types'
import { fecha } from '@/lib/fechas'

type Decision = 'aprobar' | 'rechazar'

type Props = {
  permiso: Permiso
  close: () => void
  onSaved: () => void
}

function RevisarPermisoDialog({ permiso, close, onSaved }: Props) {
  const [decision, setDecision] = useState<Decision>('aprobar')
  const [validoDesde, setValidoDesde] = useState(permiso.validoDesde)
  const [validoHasta, setValidoHasta] = useState('')
  const [motivo, setMotivo] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const aprobar = decision === 'aprobar'
  const completo = aprobar ? Boolean(validoHasta) : Boolean(motivo.trim())

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      if (aprobar) {
        await PermisosApi.aprobar(permiso.id, { validoDesde, validoHasta })
      } else {
        await PermisosApi.rechazar(permiso.id, motivo)
      }
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
        Permiso {permiso.nroPermiso} del vehículo {permiso.matricula}, válido desde {fecha(permiso.validoDesde)}.
      </p>
      <RadioGroup value={decision} onValueChange={(v) => setDecision(v as Decision)} className="flex gap-6">
        <div className="flex items-center gap-2">
          <RadioGroupItem value="aprobar" id="aprobar" />
          <Label htmlFor="aprobar">Aprobar</Label>
        </div>
        <div className="flex items-center gap-2">
          <RadioGroupItem value="rechazar" id="rechazar" />
          <Label htmlFor="rechazar">Rechazar</Label>
        </div>
      </RadioGroup>
      {aprobar ? (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <Label htmlFor="validoDesde">Válido desde</Label>
            <Input
              id="validoDesde"
              type="date"
              value={validoDesde}
              onChange={(e) => setValidoDesde(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="validoHasta">Válido hasta</Label>
            <Input
              id="validoHasta"
              type="date"
              min={validoDesde}
              value={validoHasta}
              onChange={(e) => setValidoHasta(e.target.value)}
            />
          </div>
        </div>
      ) : (
        <div className="space-y-1.5">
          <Label htmlFor="motivo">Motivo del rechazo</Label>
          <Textarea id="motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} />
        </div>
      )}
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" variant={aprobar ? 'default' : 'destructive'} disabled={guardando || !completo}>
          {aprobar ? 'Aprobar' : 'Rechazar'}
        </Button>
      </DialogFooter>
    </form>
  )
}

export default RevisarPermisoDialog

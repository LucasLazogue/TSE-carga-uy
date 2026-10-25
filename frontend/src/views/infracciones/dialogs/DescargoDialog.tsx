import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { Caso } from '@/api/casos/casos.types'
import { getErrorMessage } from '@/api/errors'
import { InfraccionesApi } from '@/api/infracciones/infracciones.api'
import { fechaHora } from '@/lib/fechas'

type Props = {
  caso: Caso
  close: () => void
  onPresented: () => void
}

function DescargoDialog({ caso, close, onPresented }: Props) {
  const [texto, setTexto] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await InfraccionesApi.createDescargo(caso.id, texto)
      onPresented()
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
      {caso.venceDescargo && (
        <p className="text-sm text-muted-foreground">
          Se admite un solo descargo, hasta el {fechaHora(caso.venceDescargo)}.
        </p>
      )}
      <div className="space-y-1.5">
        <Label htmlFor="texto">Descargo</Label>
        <Textarea id="texto" required value={texto} onChange={(e) => setTexto(e.target.value)} />
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          Presentar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default DescargoDialog

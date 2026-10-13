import { useState } from 'react'
import type { FormEvent } from 'react'
import axios from 'axios'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import type { Caso } from '@/api/casos/casos.types'
import { getErrorMessage } from '@/api/errors'
import { infraccionesApi } from '@/api/infracciones/infracciones.api'
import { LARGO_MAXIMO_TEXTO, fechaHora } from '@/views/casos/casos.constants'

type Props = {
  caso: Caso
  close: () => void
  onPresented: (caso: Caso) => void
  onConflict: () => void
}

function DescargoDialog({ caso, close, onPresented, onConflict }: Props) {
  const [texto, setTexto] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      onPresented(await infraccionesApi.presentarDescargo(caso.id, texto))
      close()
    } catch (err) {
      setError(getErrorMessage(err))
      // el caso cambio mientras el dialogo estaba abierto: se recarga la pagina de atras
      if (axios.isAxiosError(err) && err.response?.status === 409) {
        onConflict()
      }
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
        Se admite un solo descargo por infracción, hasta el {fechaHora(caso.venceDescargo)}.
      </p>
      <div className="space-y-1.5">
        <Label htmlFor="texto">Descargo</Label>
        <Textarea
          id="texto"
          className="min-h-32 max-h-64 overflow-y-auto"
          maxLength={LARGO_MAXIMO_TEXTO}
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
        />
        <p className="text-xs text-muted-foreground">
          {texto.length} / {LARGO_MAXIMO_TEXTO} caracteres
        </p>
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando || !texto.trim()}>
          Presentar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default DescargoDialog

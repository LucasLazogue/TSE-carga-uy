import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { ChoferesApi } from '@/api/choferes/choferes.api'
import { getErrorMessage } from '@/api/errors'
import { formatCedula, onlyDigits } from '@/lib/cedula'

type Props = {
  idEmpresa: number
  close: () => void
  onCreated: () => void
}

function NuevoChoferDialog({ idEmpresa, close, onCreated }: Props) {
  const [cedula, setCedula] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await ChoferesApi.create(idEmpresa, cedula)
      onCreated()
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
        <Label htmlFor="cedula">Cédula</Label>
        <Input
          id="cedula"
          inputMode="numeric"
          placeholder="Ingrese cédula"
          value={formatCedula(cedula)}
          onChange={(e) => setCedula(onlyDigits(e.target.value))}
        />
        <p className="text-xs text-muted-foreground">El ingreso a la empresa queda con la fecha de hoy.</p>
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

export default NuevoChoferDialog

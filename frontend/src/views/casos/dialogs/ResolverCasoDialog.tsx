import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { CasosApi } from '@/api/casos/casos.api'
import type { Caso, EstadoCaso } from '@/api/casos/casos.types'
import { getErrorMessage } from '@/api/errors'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  caso: Caso
  close: () => void
  onResolved: () => void
}

function ResolverCasoDialog({ caso, close, onResolved }: Props) {
  const [resultado, setResultado] = useState<EstadoCaso>('CONFIRMADA')
  const [fundamento, setFundamento] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await CasosApi.createResolucion(caso.id, { resultado, fundamento })
      onResolved()
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
        Caso {caso.id} de {caso.nombreEmpresa}, vehículo {caso.matricula}.
      </p>
      <div className="space-y-1.5">
        <Label htmlFor="resultado">Resultado</Label>
        <select
          id="resultado"
          className={selectClass}
          value={resultado}
          onChange={(e) => setResultado(e.target.value as EstadoCaso)}
        >
          <option value="CONFIRMADA">Confirmar la infracción</option>
          <option value="DESESTIMADA">Desestimar el caso</option>
        </select>
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="fundamento">Fundamento</Label>
        <Textarea id="fundamento" required value={fundamento} onChange={(e) => setFundamento(e.target.value)} />
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          Resolver
        </Button>
      </DialogFooter>
    </form>
  )
}

export default ResolverCasoDialog

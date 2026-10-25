import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { EmpresaNueva } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { formatCedula, onlyDigits } from '@/lib/cedula'
import { EMPRESA_VACIA } from '../empresas.constants'

type Props = {
  close: () => void
  onCreated: () => void
}

function NuevaEmpresaDialog({ close, onCreated }: Props) {
  const [nueva, setNueva] = useState<EmpresaNueva>(EMPRESA_VACIA)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await EmpresasApi.create(nueva)
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
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5">
          <Label htmlFor="nroEmpresa">Nro. empresa</Label>
          <Input
            id="nroEmpresa"
            type="number"
            value={nueva.nroEmpresa || ''}
            onChange={(e) => setNueva({ ...nueva, nroEmpresa: Number(e.target.value) })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="cedulaResponsable">Cédula del responsable</Label>
          <Input
            id="cedulaResponsable"
            inputMode="numeric"
            placeholder="Ingrese cédula"
            value={formatCedula(nueva.cedulaResponsable)}
            onChange={(e) => setNueva({ ...nueva, cedulaResponsable: onlyDigits(e.target.value) })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="nombrePublico">Nombre público</Label>
          <Input
            id="nombrePublico"
            value={nueva.nombrePublico}
            onChange={(e) => setNueva({ ...nueva, nombrePublico: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="razonSocial">Razón social</Label>
          <Input
            id="razonSocial"
            value={nueva.razonSocial}
            onChange={(e) => setNueva({ ...nueva, razonSocial: e.target.value })}
          />
        </div>
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="direccionPrincipal">Dirección principal</Label>
          <Input
            id="direccionPrincipal"
            value={nueva.direccionPrincipal}
            onChange={(e) => setNueva({ ...nueva, direccionPrincipal: e.target.value })}
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

export default NuevaEmpresaDialog

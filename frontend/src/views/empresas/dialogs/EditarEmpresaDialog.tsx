import { useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { DatosEmpresa, Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'

type Props = {
  empresa: Empresa
  close: () => void
  onSaved: () => void
}

function EditarEmpresaDialog({ empresa, close, onSaved }: Props) {
  const [datos, setDatos] = useState<DatosEmpresa>({
    nroEmpresa: empresa.nroEmpresa,
    nombrePublico: empresa.nombrePublico,
    razonSocial: empresa.razonSocial,
    direccionPrincipal: empresa.direccionPrincipal,
  })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await EmpresasApi.update(empresa.id, datos)
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
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="nroEmpresa">Nro. empresa</Label>
          <Input
            id="nroEmpresa"
            type="number"
            value={datos.nroEmpresa || ''}
            onChange={(e) => setDatos({ ...datos, nroEmpresa: Number(e.target.value) })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="nombrePublico">Nombre público</Label>
          <Input
            id="nombrePublico"
            value={datos.nombrePublico}
            onChange={(e) => setDatos({ ...datos, nombrePublico: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="razonSocial">Razón social</Label>
          <Input
            id="razonSocial"
            value={datos.razonSocial}
            onChange={(e) => setDatos({ ...datos, razonSocial: e.target.value })}
          />
        </div>
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="direccionPrincipal">Dirección principal</Label>
          <Input
            id="direccionPrincipal"
            value={datos.direccionPrincipal}
            onChange={(e) => setDatos({ ...datos, direccionPrincipal: e.target.value })}
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

export default EditarEmpresaDialog

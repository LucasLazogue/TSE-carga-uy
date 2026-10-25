import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AlertCircle, ArrowLeft } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { DatosEmpresa, Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { fecha } from '@/lib/fechas'

function PerfilEmpresa() {
  const { id } = useParams()
  const [empresa, setEmpresa] = useState<Empresa | null>(null)
  const [datos, setDatos] = useState<DatosEmpresa | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const sinCambios =
    !!empresa &&
    !!datos &&
    empresa.nroEmpresa === datos.nroEmpresa &&
    empresa.nombrePublico === datos.nombrePublico &&
    empresa.razonSocial === datos.razonSocial &&
    empresa.direccionPrincipal === datos.direccionPrincipal

  useEffect(() => {
    let vigente = true
    EmpresasApi.find(Number(id))
      .then((e) => {
        if (vigente) {
          setEmpresa(e)
          setDatos(e)
        }
      })
      .catch((err) => {
        if (vigente) {
          setError(getErrorMessage(err))
        }
      })
    return () => {
      vigente = false
    }
  }, [id])

  async function guardar(e: FormEvent) {
    e.preventDefault()
    if (!empresa || !datos) {
      return
    }
    setError('')
    setGuardando(true)
    try {
      await EmpresasApi.update(empresa.id, datos)
      setEmpresa({ ...empresa, ...datos })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <div>
      <PageHeader
        title={empresa?.nombrePublico ?? 'Empresa'}
        description={empresa && `Alta el ${fecha(empresa.fechaAlta)}`}
        actions={
          <Button variant="outline" asChild>
            <Link to="/empresas">
              <ArrowLeft />
              Volver
            </Link>
          </Button>
        }
      />
      {error && (
        <Alert variant="destructive" className="mb-4 max-w-2xl">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {datos && (
        <form onSubmit={guardar} className="max-w-2xl space-y-4">
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
          <div className="flex justify-end">
            <Button type="submit" disabled={guardando || sinCambios}>
              Guardar
            </Button>
          </div>
        </form>
      )}
    </div>
  )
}

export default PerfilEmpresa

import { useEffect, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AlertCircle, ArrowLeft, Pencil } from 'lucide-react'
import { cn } from 'cn'
import PageHeader from '@/components/PageHeader'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { DatosEmpresa, Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { fecha } from '@/lib/fechas'

type CampoProps = {
  id: string
  etiqueta: string
  className?: string
  children: ReactNode
}

function Campo({ id, etiqueta, className, children }: CampoProps) {
  return (
    <div className={cn('space-y-1.5', className)}>
      <Label htmlFor={id}>{etiqueta}</Label>
      {children}
    </div>
  )
}

function PerfilEmpresa() {
  const { id } = useParams()
  const [empresa, setEmpresa] = useState<Empresa | null>(null)
  const [datos, setDatos] = useState<DatosEmpresa | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const { confirm } = useDialogs()
  const editando = datos !== null
  const conCambios =
    !!empresa &&
    !!datos &&
    (empresa.nroEmpresa !== datos.nroEmpresa ||
      empresa.nombrePublico !== datos.nombrePublico ||
      empresa.razonSocial !== datos.razonSocial ||
      empresa.direccionPrincipal !== datos.direccionPrincipal)

  useEffect(() => {
    let vigente = true
    EmpresasApi.find(Number(id))
      .then((e) => {
        if (vigente) {
          setEmpresa(e)
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

  function editar(e: Empresa) {
    setError('')
    setDatos({
      nroEmpresa: e.nroEmpresa,
      nombrePublico: e.nombrePublico,
      razonSocial: e.razonSocial,
      direccionPrincipal: e.direccionPrincipal,
    })
  }

  async function cancelar() {
    if (
      conCambios &&
      !(await confirm({
        title: 'Descartar cambios',
        description: 'Los cambios que hizo no se van a guardar.',
        confirmText: 'Descartar',
        destructive: true,
      }))
    ) {
      return
    }
    setError('')
    setDatos(null)
  }

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
      setDatos(null)
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
          <>
            <Button variant="outline" asChild>
              <Link to="/empresas">
                <ArrowLeft />
                Volver
              </Link>
            </Button>
            {empresa && !editando && (
              <Button onClick={() => editar(empresa)}>
                <Pencil />
                Editar
              </Button>
            )}
          </>
        }
      />
      {error && (
        <Alert variant="destructive" className="mb-4 max-w-2xl">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {empresa && (
        <form onSubmit={guardar} className="max-w-2xl space-y-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo id="nroEmpresa" etiqueta="Nro. empresa" className="sm:col-span-2">
              {datos ? (
                <Input
                  id="nroEmpresa"
                  type="number"
                  autoFocus
                  value={datos.nroEmpresa || ''}
                  onChange={(e) => setDatos({ ...datos, nroEmpresa: Number(e.target.value) })}
                />
              ) : (
                <p className="text-sm">{empresa.nroEmpresa}</p>
              )}
            </Campo>
            <Campo id="nombrePublico" etiqueta="Nombre público">
              {datos ? (
                <Input
                  id="nombrePublico"
                  value={datos.nombrePublico}
                  onChange={(e) => setDatos({ ...datos, nombrePublico: e.target.value })}
                />
              ) : (
                <p className="text-sm">{empresa.nombrePublico}</p>
              )}
            </Campo>
            <Campo id="razonSocial" etiqueta="Razón social">
              {datos ? (
                <Input
                  id="razonSocial"
                  value={datos.razonSocial}
                  onChange={(e) => setDatos({ ...datos, razonSocial: e.target.value })}
                />
              ) : (
                <p className="text-sm">{empresa.razonSocial}</p>
              )}
            </Campo>
            <Campo id="direccionPrincipal" etiqueta="Dirección principal" className="sm:col-span-2">
              {datos ? (
                <Input
                  id="direccionPrincipal"
                  value={datos.direccionPrincipal}
                  onChange={(e) => setDatos({ ...datos, direccionPrincipal: e.target.value })}
                />
              ) : (
                <p className="text-sm">{empresa.direccionPrincipal}</p>
              )}
            </Campo>
          </div>
          {editando && (
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={cancelar}>
                Cancelar
              </Button>
              <Button type="submit" disabled={guardando || !conCambios}>
                Guardar
              </Button>
            </div>
          )}
        </form>
      )}
    </div>
  )
}

export default PerfilEmpresa

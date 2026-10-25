import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AlertCircle, ArrowLeft, Pencil } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { fecha } from '@/lib/fechas'
import EditarEmpresaDialog from './dialogs/EditarEmpresaDialog'

function PerfilEmpresa() {
  const { id } = useParams()
  const [empresa, setEmpresa] = useState<Empresa | null>(null)
  const [error, setError] = useState('')
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()

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
  }, [id, version])

  function editar(e: Empresa) {
    open({
      title: 'Editar empresa',
      size: 'lg',
      content: (close) => <EditarEmpresaDialog empresa={e} close={close} onSaved={() => setVersion((v) => v + 1)} />,
    })
  }

  const datos = empresa && [
    ['Nro. empresa', empresa.nroEmpresa],
    ['Nombre público', empresa.nombrePublico],
    ['Razón social', empresa.razonSocial],
    ['Dirección principal', empresa.direccionPrincipal],
    ['Fecha de alta', fecha(empresa.fechaAlta)],
  ]

  return (
    <div>
      <PageHeader
        title={empresa?.nombrePublico ?? 'Empresa'}
        description="Datos de la empresa transportista."
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/empresas">
                <ArrowLeft />
                Volver
              </Link>
            </Button>
            {empresa && (
              <Button onClick={() => editar(empresa)}>
                <Pencil />
                Editar
              </Button>
            )}
          </>
        }
      />
      {error && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {datos && (
        <Card>
          <CardContent>
            <dl className="grid gap-4 sm:grid-cols-2">
              {datos.map(([etiqueta, valor]) => (
                <div key={etiqueta} className="space-y-1">
                  <dt className="text-sm text-muted-foreground">{etiqueta}</dt>
                  <dd className="font-medium">{valor}</dd>
                </div>
              ))}
            </dl>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default PerfilEmpresa

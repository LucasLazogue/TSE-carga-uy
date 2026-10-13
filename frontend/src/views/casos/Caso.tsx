import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { AlertCircle, ArrowLeft, Gavel } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { casosApi } from '@/api/casos/casos.api'
import type { Caso as CasoFiscalizacion, PistaAuditoria } from '@/api/casos/casos.types'
import { getErrorMessage } from '@/api/errors'
import { ACCIONES_AUDITORIA, ESTADOS_CASO, TIPOS_INFRACCION, fechaHora, puedeResolver } from './casos.constants'
import DetalleCaso from './DetalleCaso'
import ResolverCasoDialog from './dialogs/ResolverCasoDialog'

const columnasAuditoria: GridColumn<PistaAuditoria>[] = [
  { header: 'Fecha', cell: (p) => fechaHora(p.fecha) },
  { header: 'Acción', cell: (p) => ACCIONES_AUDITORIA[p.accion], className: 'font-medium' },
  {
    header: 'Estado',
    cell: (p) =>
      p.estadoAnterior
        ? `${ESTADOS_CASO[p.estadoAnterior]} → ${ESTADOS_CASO[p.estadoNuevo]}`
        : ESTADOS_CASO[p.estadoNuevo],
  },
  { header: 'Usuario', cell: (p) => (p.usuario === p.rol ? p.usuario : `${p.usuario} (${p.rol})`) },
  { header: 'Fundamento', cell: (p) => p.fundamento ?? '—', className: 'whitespace-normal' },
]

function Caso() {
  const { id } = useParams()
  const location = useLocation()
  const [caso, setCaso] = useState<CasoFiscalizacion | null>(null)
  const [auditoria, setAuditoria] = useState<PistaAuditoria[]>([])
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const { open } = useDialogs()

  useEffect(() => {
    let vigente = true
    Promise.all([casosApi.getById(Number(id)), casosApi.getAuditoria(Number(id))])
      .then(([datos, pista]) => {
        if (vigente) {
          setCaso(datos)
          setAuditoria(pista)
          setError('')
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

  function resolver(actual: CasoFiscalizacion) {
    open({
      title: 'Resolver caso',
      content: (close) => (
        <ResolverCasoDialog
          caso={actual}
          close={close}
          onResolved={() => setVersion((v) => v + 1)}
          onConflict={() => setVersion((v) => v + 1)}
        />
      ),
    })
  }

  const volver = (
    <Button variant="outline" asChild>
      <Link to={`/casos${(location.state as { volver?: string } | null)?.volver ?? ''}`}>
        <ArrowLeft />
        Volver
      </Link>
    </Button>
  )

  if (error) {
    return (
      <div>
        <PageHeader title="Caso" actions={volver} />
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      </div>
    )
  }

  if (!caso) {
    return null
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title={`Caso ${caso.id}`}
        description={`${TIPOS_INFRACCION[caso.tipo]} · ${caso.nombreEmpresa} · ${caso.matricula}`}
        actions={
          <>
            {volver}
            {puedeResolver(caso) && (
              <Button onClick={() => resolver(caso)}>
                <Gavel />
                Resolver
              </Button>
            )}
          </>
        }
      />
      {caso.estado === 'NOTIFICADA' && !puedeResolver(caso) && (
        <Alert>
          <AlertCircle />
          <AlertDescription>
            La empresa puede presentar un descargo hasta el {fechaHora(caso.venceDescargo)}. El caso se podrá resolver
            cuando presente el descargo o venza ese plazo.
          </AlertDescription>
        </Alert>
      )}
      <DetalleCaso caso={caso} />
      <Card>
        <CardHeader>
          <CardTitle>Auditoría</CardTitle>
          <CardDescription>Registro de todas las acciones sobre el caso.</CardDescription>
        </CardHeader>
        <CardContent>
          <Grid columns={columnasAuditoria} items={auditoria} rowKey={(p) => p.id} empty="Sin registros." />
        </CardContent>
      </Card>
    </div>
  )
}

export default Caso

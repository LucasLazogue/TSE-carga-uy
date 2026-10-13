import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { AlertCircle, ArrowLeft, MessageSquareText } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import type { Caso } from '@/api/casos/casos.types'
import { getErrorMessage } from '@/api/errors'
import { infraccionesApi } from '@/api/infracciones/infracciones.api'
import { TIPOS_INFRACCION, fechaHora, plazoRestante, puedeDescargar } from '@/views/casos/casos.constants'
import DetalleCaso from '@/views/casos/DetalleCaso'
import DescargoDialog from './dialogs/DescargoDialog'

function Infraccion() {
  const { id } = useParams()
  const location = useLocation()
  const [caso, setCaso] = useState<Caso | null>(null)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const { open } = useDialogs()

  useEffect(() => {
    let vigente = true
    infraccionesApi
      .getById(Number(id))
      .then((datos) => {
        if (vigente) {
          setCaso(datos)
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

  function presentarDescargo(actual: Caso) {
    open({
      title: 'Presentar descargo',
      size: 'lg',
      content: (close) => (
        <DescargoDialog
          caso={actual}
          close={close}
          onPresented={setCaso}
          onConflict={() => setVersion((v) => v + 1)}
        />
      ),
    })
  }

  const volver = (
    <Button variant="outline" asChild>
      <Link to={`/infracciones${(location.state as { volver?: string } | null)?.volver ?? ''}`}>
        <ArrowLeft />
        Volver
      </Link>
    </Button>
  )

  if (error) {
    return (
      <div>
        <PageHeader title="Infracción" actions={volver} />
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

  const plazo = plazoRestante(caso)

  return (
    <div className="space-y-4">
      <PageHeader
        title={`Infracción ${caso.id}`}
        description={`${TIPOS_INFRACCION[caso.tipo]} · ${caso.nombreEmpresa} · ${caso.matricula}`}
        actions={
          <>
            {volver}
            {puedeDescargar(caso) && (
              <Button onClick={() => presentarDescargo(caso)}>
                <MessageSquareText />
                Presentar descargo
              </Button>
            )}
          </>
        }
      />
      {plazo && (
        <Alert variant={puedeDescargar(caso) ? 'default' : 'destructive'}>
          <AlertCircle />
          <AlertDescription>
            {puedeDescargar(caso)
              ? `Plazo restante para presentar un descargo: ${plazo} (vence el ${fechaHora(caso.venceDescargo)}).`
              : `El plazo para presentar un descargo venció el ${fechaHora(caso.venceDescargo)}.`}
          </AlertDescription>
        </Alert>
      )}
      <DetalleCaso caso={caso} />
    </div>
  )
}

export default Infraccion

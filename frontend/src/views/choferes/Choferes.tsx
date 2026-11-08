import { useEffect, useState } from 'react'
import { AlertCircle, Plus, RefreshCw, UserMinus } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import LookupFilter from '@/components/toolbar/LookupFilter'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { ChoferesApi } from '@/api/choferes/choferes.api'
import type { Chofer } from '@/api/choferes/choferes.types'
import { getErrorMessage } from '@/api/errors'
import type { Page } from '@/api/page'
import { useEmpresaInicial } from '@/hooks/useEmpresaInicial'
import { useListParams } from '@/hooks/useListParams'
import { fecha } from '@/lib/fechas'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import NuevoChoferDialog from './dialogs/NuevoChoferDialog'

const columnas: GridColumn<Chofer>[] = [
  { header: 'Cédula', cell: (c) => c.cedula, className: 'font-medium' },
  { header: 'Correo', cell: (c) => c.correo ?? '-' },
  { header: 'Ingreso', cell: (c) => fecha(c.fechaDesde) },
  { header: 'Baja', cell: (c) => (c.fechaHasta ? fecha(c.fechaHasta) : '-') },
  {
    header: 'Estado',
    cell: (c) =>
      c.fechaHasta ? <Badge variant="outline">Dado de baja</Badge> : <Badge variant="secondary">Activo</Badge>,
  },
]

const FILTROS = ['idEmpresa'] as const

function Choferes() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Chofer> | null>(null)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const { open, confirm } = useDialogs()
  const { idEmpresa } = values

  useEmpresaInicial(idEmpresa, set)

  // con historial: tambien los dados de baja, y un reingreso aparece como otra fila
  useEffect(() => {
    if (!idEmpresa) {
      return
    }
    let vigente = true
    ChoferesApi.findAll(Number(idEmpresa), { historial: true }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa, pagina, version])

  function recargar() {
    setVersion((v) => v + 1)
  }

  function nuevoChofer() {
    if (!idEmpresa) {
      return
    }
    open({
      title: 'Nuevo chofer',
      content: (close) => <NuevoChoferDialog idEmpresa={Number(idEmpresa)} close={close} onCreated={recargar} />,
    })
  }

  async function darDeBaja(chofer: Chofer) {
    const ok = await confirm({
      title: 'Dar de baja',
      description: `El chofer ${chofer.cedula} deja de ser chofer de la empresa a partir de hoy y ya no se le pueden asignar viajes.`,
      confirmText: 'Dar de baja',
      destructive: true,
    })
    if (!ok) {
      return
    }
    setError('')
    try {
      await ChoferesApi.baja(Number(idEmpresa), chofer.idVinculo)
      recargar()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  const columnasConAcciones: GridColumn<Chofer>[] = [
    ...columnas,
    {
      header: 'Acciones',
      hideHeader: true,
      className: 'sticky right-0 w-px bg-background text-right',
      cell: (c) =>
        !c.fechaHasta && (
          <Button size="sm" variant="outline" onClick={() => darDeBaja(c)}>
            <UserMinus />
            <span className="sr-only md:not-sr-only md:whitespace-nowrap">Dar de baja</span>
          </Button>
        ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Choferes"
        description="Choferes de la empresa: altas, bajas y su historial."
        actions={
          <Button onClick={nuevoChofer} disabled={!idEmpresa}>
            <Plus />
            Nuevo chofer
          </Button>
        }
      />
      <DataToolbar
        actions={
          <Button variant="outline" size="icon" aria-label="Recargar" onClick={recargar}>
            <RefreshCw />
          </Button>
        }
      >
        <LookupFilter
          label="Empresa"
          value={idEmpresa}
          onChange={(v) => v && set('idEmpresa', v)}
          {...fuenteEmpresas}
          required
        />
      </DataToolbar>
      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <Grid
        columns={columnasConAcciones}
        items={resultado?.items ?? []}
        rowKey={(c) => c.idVinculo}
        empty="No hay choferes registrados."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Choferes

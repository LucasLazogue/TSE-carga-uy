import { useEffect, useState } from 'react'
import { Eye, MessageSquareText, RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import FilterSelect from '@/components/toolbar/FilterSelect'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Caso, EstadoCaso } from '@/api/casos/casos.types'
import { InfraccionesApi } from '@/api/infracciones/infracciones.api'
import type { Page } from '@/api/page'
import { useListParams } from '@/hooks/useListParams'
import { fechaHora } from '@/lib/fechas'
import { NOMBRES_ESTADO, NOMBRES_TIPO, OPCIONES_ESTADO, VARIANTES_ESTADO } from '@/views/casos/casos.constants'
import CasoDialog from '@/views/casos/dialogs/CasoDialog'
import DescargoDialog from './dialogs/DescargoDialog'

const columnas: GridColumn<Caso>[] = [
  { header: 'Nro.', cell: (c) => c.id, className: 'font-medium' },
  { header: 'Tipo', cell: (c) => NOMBRES_TIPO[c.tipo] },
  { header: 'Vehículo', cell: (c) => c.matricula },
  { header: 'Vence descargo', cell: (c) => (c.venceDescargo ? fechaHora(c.venceDescargo) : '-') },
  { header: 'Estado', cell: (c) => <Badge variant={VARIANTES_ESTADO[c.estado]}>{NOMBRES_ESTADO[c.estado]}</Badge> },
]

const FILTROS = ['estado'] as const

function Infracciones() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Caso> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { estado } = values

  useEffect(() => {
    let vigente = true
    InfraccionesApi.findAll({ estado: estado as EstadoCaso | undefined }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [estado, pagina, version])

  function recargar() {
    setVersion((v) => v + 1)
  }

  async function verInfraccion(id: number) {
    const caso = await InfraccionesApi.find(id)
    open({
      title: `Infracción ${id}`,
      size: 'lg',
      content: (close) => <CasoDialog caso={caso} close={close} />,
    })
  }

  function presentarDescargo(caso: Caso) {
    open({
      title: 'Presentar descargo',
      content: (close) => <DescargoDialog caso={caso} close={close} onPresented={recargar} />,
    })
  }

  const columnasConAcciones: GridColumn<Caso>[] = [
    ...columnas,
    {
      header: 'Acciones',
      hideHeader: true,
      className: 'sticky right-0 w-px bg-background text-right',
      cell: (c) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="outline" onClick={() => verInfraccion(c.id)}>
            <Eye />
            <span className="sr-only md:not-sr-only md:whitespace-nowrap">Ver</span>
          </Button>
          {c.estado === 'NOTIFICADA' && (
            <Button size="sm" variant="outline" onClick={() => presentarDescargo(c)}>
              <MessageSquareText />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Presentar descargo</span>
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="Infracciones" description="Infracciones notificadas a tu empresa." />
      <DataToolbar
        actions={
          <Button variant="outline" size="icon" aria-label="Recargar" onClick={recargar}>
            <RefreshCw />
          </Button>
        }
      >
        <FilterSelect label="Estado" value={estado} options={OPCIONES_ESTADO} onChange={(v) => set('estado', v)} />
      </DataToolbar>
      <Grid
        columns={columnasConAcciones}
        items={resultado?.items ?? []}
        rowKey={(c) => c.id}
        empty="No hay infracciones notificadas."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Infracciones

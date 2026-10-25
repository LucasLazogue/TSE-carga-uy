import { useEffect, useState } from 'react'
import { Eye, Gavel, RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import FilterSelect from '@/components/toolbar/FilterSelect'
import LookupFilter from '@/components/toolbar/LookupFilter'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { CasosApi } from '@/api/casos/casos.api'
import type { Caso, EstadoCaso, TipoInfraccion } from '@/api/casos/casos.types'
import type { Page } from '@/api/page'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import { useListParams } from '@/hooks/useListParams'
import { fechaHora } from '@/lib/fechas'
import CasoDialog from './dialogs/CasoDialog'
import ResolverCasoDialog from './dialogs/ResolverCasoDialog'
import { NOMBRES_ESTADO, NOMBRES_TIPO, OPCIONES_ESTADO, OPCIONES_TIPO, VARIANTES_ESTADO } from './casos.constants'

const columnas: GridColumn<Caso>[] = [
  { header: 'Nro.', cell: (c) => c.id, className: 'font-medium' },
  { header: 'Tipo', cell: (c) => NOMBRES_TIPO[c.tipo] },
  { header: 'Empresa', cell: (c) => c.nombreEmpresa },
  { header: 'Vehículo', cell: (c) => c.matricula },
  { header: 'Detectado', cell: (c) => fechaHora(c.fechaDeteccion) },
  { header: 'Vence descargo', cell: (c) => (c.venceDescargo ? fechaHora(c.venceDescargo) : '-') },
  { header: 'Estado', cell: (c) => <Badge variant={VARIANTES_ESTADO[c.estado]}>{NOMBRES_ESTADO[c.estado]}</Badge> },
]

const FILTROS = ['estado', 'tipo', 'empresa'] as const

function Casos() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Caso> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { estado, tipo, empresa } = values

  useEffect(() => {
    let vigente = true
    const filtro = {
      estado: estado as EstadoCaso | undefined,
      tipo: tipo as TipoInfraccion | undefined,
      empresa: empresa ? Number(empresa) : undefined,
    }
    CasosApi.findAll(filtro, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [estado, tipo, empresa, pagina, version])

  function recargar() {
    setVersion((v) => v + 1)
  }

  async function verCaso(id: number) {
    const [caso, auditoria] = await Promise.all([CasosApi.find(id), CasosApi.findAuditoria(id)])
    open({
      title: `Caso ${id}`,
      size: 'lg',
      content: (close) => <CasoDialog caso={caso} auditoria={auditoria} close={close} />,
    })
  }

  function resolver(caso: Caso) {
    open({
      title: 'Resolver caso',
      content: (close) => <ResolverCasoDialog caso={caso} close={close} onResolved={recargar} />,
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
          <Button size="sm" variant="outline" onClick={() => verCaso(c.id)}>
            <Eye />
            <span className="sr-only md:not-sr-only md:whitespace-nowrap">Ver caso</span>
          </Button>
          {(c.estado === 'NOTIFICADA' || c.estado === 'EN_DESCARGO') && (
            <Button size="sm" variant="outline" onClick={() => resolver(c)}>
              <Gavel />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Resolver</span>
            </Button>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader title="Casos" description="Casos de fiscalización detectados sobre las empresas." />
      <DataToolbar
        actions={
          <Button variant="outline" size="icon" aria-label="Recargar" onClick={recargar}>
            <RefreshCw />
          </Button>
        }
      >
        <FilterSelect label="Estado" value={estado} options={OPCIONES_ESTADO} onChange={(v) => set('estado', v)} />
        <FilterSelect label="Tipo" value={tipo} options={OPCIONES_TIPO} onChange={(v) => set('tipo', v)} />
        <LookupFilter label="Empresa" value={empresa} onChange={(v) => set('empresa', v)} {...fuenteEmpresas} />
      </DataToolbar>
      <Grid
        columns={columnasConAcciones}
        items={resultado?.items ?? []}
        rowKey={(c) => c.id}
        empty="No hay casos."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Casos

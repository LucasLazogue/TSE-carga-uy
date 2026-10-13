import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import DataToolbar from '@/components/toolbar/DataToolbar'
import DateFilter from '@/components/toolbar/DateFilter'
import FilterSelect from '@/components/toolbar/FilterSelect'
import LookupFilter from '@/components/toolbar/LookupFilter'
import { Button } from '@/components/ui/button'
import { casosApi } from '@/api/casos/casos.api'
import type { Caso } from '@/api/casos/casos.types'
import { empresasApi } from '@/api/empresas/empresas.api'
import type { Page } from '@/api/page'
import { useListParams } from '@/hooks/useListParams'
import { OPCIONES_ESTADO, OPCIONES_TIPO, TIPOS_INFRACCION, fechaHora } from './casos.constants'
import EstadoBadge from './EstadoBadge'

const columnas: GridColumn<Caso>[] = [
  { header: 'Nro.', cell: (c) => c.id, className: 'font-medium' },
  { header: 'Tipo', cell: (c) => TIPOS_INFRACCION[c.tipo] },
  { header: 'Empresa', cell: (c) => c.nombreEmpresa },
  { header: 'Vehículo', cell: (c) => c.matricula },
  { header: 'Detectado', cell: (c) => fechaHora(c.fechaDeteccion) },
  { header: 'Vence descargo', cell: (c) => fechaHora(c.venceDescargo) },
  { header: 'Estado', cell: (c) => <EstadoBadge estado={c.estado} /> },
]

const FILTROS = ['estado', 'tipo', 'empresa', 'desde', 'hasta'] as const

async function buscarEmpresas(texto: string) {
  const { items: empresas } = await empresasApi.getAll(texto)
  return empresas.map((e) => ({ value: String(e.id), label: e.nombrePublico }))
}

async function empresaPorId(id: string) {
  try {
    const empresa = await empresasApi.getById(Number(id))
    return { value: String(empresa.id), label: empresa.nombrePublico }
  } catch {
    return null
  }
}

function Casos() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Caso> | null>(null)
  const [version, setVersion] = useState(0)
  const navigate = useNavigate()
  const location = useLocation()
  const { estado, tipo, empresa, desde, hasta } = values

  useEffect(() => {
    let vigente = true
    casosApi.getAll({ estado, tipo, empresa, desde, hasta }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [estado, tipo, empresa, desde, hasta, pagina, version])

  return (
    <div>
      <PageHeader title="Casos" description="Bandeja de casos de fiscalización detectados sobre las empresas." />
      <DataToolbar
        actions={
          <Button variant="outline" size="icon" aria-label="Recargar" onClick={() => setVersion((v) => v + 1)}>
            <RefreshCw />
          </Button>
        }
      >
        <FilterSelect label="Estado" value={estado} options={OPCIONES_ESTADO} onChange={(v) => set('estado', v)} />
        <FilterSelect label="Tipo" value={tipo} options={OPCIONES_TIPO} onChange={(v) => set('tipo', v)} />
        <LookupFilter
          label="Empresa"
          value={empresa}
          onChange={(v) => set('empresa', v)}
          search={buscarEmpresas}
          resolve={empresaPorId}
        />
        <DateFilter label="Desde" value={desde} onChange={(v) => set('desde', v)} />
        <DateFilter label="Hasta" value={hasta} onChange={(v) => set('hasta', v)} />
      </DataToolbar>
      <Grid
        columns={columnas}
        items={resultado?.items ?? []}
        rowKey={(c) => c.id}
        empty="No hay casos para los filtros elegidos."
        page={resultado ?? undefined}
        onPageChange={setPagina}
        onSelect={(c) => c && navigate(`/casos/${c.id}`, { state: { volver: location.search } })}
      />
    </div>
  )
}

export default Casos

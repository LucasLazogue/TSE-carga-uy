import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import DataToolbar from '@/components/toolbar/DataToolbar'
import FilterSelect from '@/components/toolbar/FilterSelect'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Caso } from '@/api/casos/casos.types'
import { infraccionesApi } from '@/api/infracciones/infracciones.api'
import type { Page } from '@/api/page'
import { useSession } from '@/auth/useSession'
import { useListParams } from '@/hooks/useListParams'
import { OPCIONES_ESTADO, TIPOS_INFRACCION, fechaHora, plazoRestante } from '@/views/casos/casos.constants'
import EstadoBadge from '@/views/casos/EstadoBadge'

function Plazo({ caso }: { caso: Caso }) {
  const restante = plazoRestante(caso)
  if (!restante) {
    return <span className="text-muted-foreground">—</span>
  }
  return <Badge variant={restante === 'Vencido' ? 'destructive' : 'outline'}>{restante}</Badge>
}

const columnas: GridColumn<Caso>[] = [
  { header: 'Nro.', cell: (c) => c.id, className: 'font-medium' },
  { header: 'Tipo', cell: (c) => TIPOS_INFRACCION[c.tipo] },
  { header: 'Empresa', cell: (c) => c.nombreEmpresa },
  { header: 'Vehículo', cell: (c) => c.matricula },
  { header: 'Notificada', cell: (c) => fechaHora(c.fechaNotificacion) },
  { header: 'Vence descargo', cell: (c) => fechaHora(c.venceDescargo) },
  { header: 'Plazo restante', cell: (c) => <Plazo caso={c} /> },
  { header: 'Estado', cell: (c) => <EstadoBadge estado={c.estado} /> },
]

const FILTROS = ['estado', 'empresa'] as const

const OPCIONES_ESTADO_INFRACCION = OPCIONES_ESTADO.filter((o) => o.value !== 'DETECTADA')

function Infracciones() {
  const { session } = useSession()
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Caso> | null>(null)
  const [version, setVersion] = useState(0)
  const navigate = useNavigate()
  const location = useLocation()
  const { estado, empresa } = values
  const empresas = (session?.empresas ?? []).map((e) => ({ value: String(e.id), label: e.nombrePublico }))

  useEffect(() => {
    let vigente = true
    infraccionesApi.getAll({ estado, empresa }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [estado, empresa, pagina, version])

  return (
    <div>
      <PageHeader
        title="Infracciones"
        description="Infracciones notificadas a tu empresa. Podés presentar un descargo mientras el plazo esté vigente."
      />
      <DataToolbar
        actions={
          <Button variant="outline" size="icon" aria-label="Recargar" onClick={() => setVersion((v) => v + 1)}>
            <RefreshCw />
          </Button>
        }
      >
        <FilterSelect
          label="Estado"
          value={estado}
          options={OPCIONES_ESTADO_INFRACCION}
          onChange={(v) => set('estado', v)}
        />
        {empresas.length > 1 && (
          <FilterSelect label="Empresa" value={empresa} options={empresas} onChange={(v) => set('empresa', v)} />
        )}
      </DataToolbar>
      <Grid
        columns={columnas}
        items={resultado?.items ?? []}
        rowKey={(c) => c.id}
        empty="No hay infracciones notificadas."
        page={resultado ?? undefined}
        onPageChange={setPagina}
        onSelect={(c) => c && navigate(`/infracciones/${c.id}`, { state: { volver: location.search } })}
      />
    </div>
  )
}

export default Infracciones

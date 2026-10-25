import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus, RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import SearchInput from '@/components/toolbar/SearchInput'
import { Button } from '@/components/ui/button'
import type { Page } from '@/api/page'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { useSession } from '@/auth/useSession'
import { useListParams } from '@/hooks/useListParams'
import { fecha } from '@/lib/fechas'
import NuevaEmpresaDialog from './dialogs/NuevaEmpresaDialog'

const columnas: GridColumn<Empresa>[] = [
  {
    header: 'Nro. empresa',
    cell: (e) => (
      <Link to={`/empresas/${e.id}`} className="underline-offset-4 hover:underline">
        {e.nroEmpresa}
      </Link>
    ),
    className: 'font-medium',
  },
  { header: 'Nombre público', cell: (e) => e.nombrePublico },
  { header: 'Razón social', cell: (e) => e.razonSocial },
  { header: 'Dirección principal', cell: (e) => e.direccionPrincipal },
  { header: 'Fecha de alta', cell: (e) => fecha(e.fechaAlta) },
]

const FILTROS = ['nombre'] as const

function Empresas() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Empresa> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { profile } = useSession()
  const esFuncionario = profile === 'FUNCIONARIO'
  const { nombre } = values

  useEffect(() => {
    let vigente = true
    EmpresasApi.findAll({ nombre }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [nombre, pagina, version])

  function recargar() {
    setVersion((v) => v + 1)
  }

  function nuevaEmpresa() {
    open({
      title: 'Nueva empresa',
      size: 'lg',
      content: (close) => <NuevaEmpresaDialog close={close} onCreated={recargar} />,
    })
  }

  return (
    <div>
      <PageHeader
        title="Empresas"
        description={esFuncionario ? 'Empresas transportistas registradas.' : 'Empresas de las que es responsable.'}
        actions={
          esFuncionario && (
            <Button onClick={nuevaEmpresa}>
              <Plus />
              Nueva empresa
            </Button>
          )
        }
      />
      <DataToolbar
        actions={
          <Button variant="outline" size="icon" aria-label="Recargar" onClick={recargar}>
            <RefreshCw />
          </Button>
        }
      >
        {esFuncionario && (
          <SearchInput
            placeholder="Buscar por nombre o razón social..."
            value={nombre}
            onChange={(v) => set('nombre', v)}
          />
        )}
      </DataToolbar>
      <Grid
        columns={columnas}
        items={resultado?.items ?? []}
        rowKey={(e) => e.id}
        empty="No hay empresas registradas."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Empresas

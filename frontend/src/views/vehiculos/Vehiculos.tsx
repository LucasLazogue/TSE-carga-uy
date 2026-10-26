import { useEffect, useState } from 'react'
import { CalendarCheck, Plus, RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import LookupFilter from '@/components/toolbar/LookupFilter'
import { Button } from '@/components/ui/button'
import type { Page } from '@/api/page'
import { VehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { useSession } from '@/auth/useSession'
import { useEmpresaInicial } from '@/hooks/useEmpresaInicial'
import { useListParams } from '@/hooks/useListParams'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import ItvDialog from './dialogs/ItvDialog'
import NuevoVehiculoDialog from './dialogs/NuevoVehiculoDialog'

const columnas: GridColumn<Vehiculo>[] = [
  { header: 'Matrícula', cell: (v) => v.matricula, className: 'font-medium' },
  { header: 'Marca', cell: (v) => v.marca },
  { header: 'Modelo', cell: (v) => v.modelo },
  { header: 'Peso (kg)', cell: (v) => v.pesoVehiculo.toLocaleString('es-UY'), className: 'text-right' },
  {
    header: 'Capacidad de carga (kg)',
    cell: (v) => v.capacidadCarga.toLocaleString('es-UY'),
    className: 'text-right',
  },
]

const FILTROS = ['idEmpresa'] as const

function Vehiculos() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Vehiculo> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { profile } = useSession()
  const esFuncionario = profile === 'FUNCIONARIO'
  const { idEmpresa } = values

  useEmpresaInicial(idEmpresa, set)

  useEffect(() => {
    if (!idEmpresa) {
      return
    }
    let vigente = true
    VehiculosApi.findAll({ idEmpresa: Number(idEmpresa) }, { pagina }).then((page) => {
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

  function mostrarCreado(idEmpresaCreado: number) {
    if (String(idEmpresaCreado) === idEmpresa) {
      recargar()
    } else {
      set('idEmpresa', String(idEmpresaCreado))
    }
  }

  function nuevoVehiculo() {
    if (!idEmpresa) {
      return
    }
    open({
      title: 'Nuevo vehículo',
      size: 'lg',
      content: (close) => (
        <NuevoVehiculoDialog idEmpresa={Number(idEmpresa)} close={close} onCreated={mostrarCreado} />
      ),
    })
  }

  function verItv(vehiculo: Vehiculo) {
    open({
      title: 'ITV del vehículo',
      size: 'lg',
      content: (close) => <ItvDialog vehiculo={vehiculo} soloLectura={esFuncionario} close={close} />,
    })
  }

  const columnasConAcciones: GridColumn<Vehiculo>[] = [
    ...columnas,
    {
      header: 'Acciones',
      hideHeader: true,
      className: 'sticky right-0 w-px bg-background text-right',
      cell: (v) => (
        <Button size="sm" variant="outline" onClick={() => verItv(v)}>
          <CalendarCheck />
          <span className="sr-only md:not-sr-only md:whitespace-nowrap">ITV</span>
        </Button>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Vehículos"
        description="Vehículos de carga de la empresa con sus datos técnicos."
        actions={
          <Button onClick={nuevoVehiculo} disabled={!idEmpresa}>
            <Plus />
            Nuevo vehículo
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
      <Grid
        columns={columnasConAcciones}
        items={resultado?.items ?? []}
        rowKey={(v) => v.id}
        empty="No hay vehículos registrados."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Vehiculos

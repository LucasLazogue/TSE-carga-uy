import { useCallback, useEffect, useState } from 'react'
import { Plus, RefreshCw } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import LookupFilter from '@/components/toolbar/LookupFilter'
import { Button } from '@/components/ui/button'
import { TAMANIO_MAXIMO } from '@/api/page'
import type { Page } from '@/api/page'
import { PermisosApi } from '@/api/permisos/permisos.api'
import type { Permiso } from '@/api/permisos/permisos.types'
import { VehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { useEmpresaInicial } from '@/hooks/useEmpresaInicial'
import { useListParams } from '@/hooks/useListParams'
import { fecha } from '@/lib/fechas'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import NuevoPermisoDialog from './dialogs/NuevoPermisoDialog'

const columnas: GridColumn<Permiso>[] = [
  { header: 'Nro. permiso', cell: (p) => p.nroPermiso, className: 'font-medium' },
  { header: 'Vehículo', cell: (p) => p.matricula },
  { header: 'Válido desde', cell: (p) => fecha(p.validoDesde) },
  { header: 'Válido hasta', cell: (p) => fecha(p.validoHasta) },
]

const FILTROS = ['idEmpresa', 'idVehiculo'] as const

function opcionVehiculo(v: Vehiculo) {
  return { value: String(v.id), label: v.matricula }
}

function Permisos() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [resultado, setResultado] = useState<Page<Permiso> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { idEmpresa, idVehiculo } = values

  useEmpresaInicial(idEmpresa, set)

  useEffect(() => {
    if (!idEmpresa) {
      return
    }
    let vigente = true
    VehiculosApi.findAll({ idEmpresa: Number(idEmpresa) }, { tamanio: TAMANIO_MAXIMO }).then(({ items }) => {
      if (vigente) {
        setVehiculos(items)
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa])

  useEffect(() => {
    if (!vehiculos.some((v) => String(v.id) === idVehiculo) && vehiculos.length) {
      set('idVehiculo', String(vehiculos[0].id))
    }
  }, [vehiculos, idVehiculo, set])

  useEffect(() => {
    if (!idVehiculo) {
      return
    }
    let vigente = true
    PermisosApi.findAll({ idVehiculo: Number(idVehiculo) }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [idVehiculo, pagina, version])

  const buscarVehiculos = useCallback(
    async (texto: string) =>
      vehiculos.filter((v) => v.matricula.toLowerCase().includes(texto.toLowerCase())).map(opcionVehiculo),
    [vehiculos],
  )

  const vehiculoPorId = useCallback(
    async (id: string) => {
      const vehiculo = vehiculos.find((v) => String(v.id) === id)
      return vehiculo ? opcionVehiculo(vehiculo) : null
    },
    [vehiculos],
  )

  function recargar() {
    setVersion((v) => v + 1)
  }

  function mostrarCreado(idVehiculoCreado: number) {
    if (String(idVehiculoCreado) === idVehiculo) {
      recargar()
    } else {
      set('idVehiculo', String(idVehiculoCreado))
    }
  }

  function nuevoPermiso() {
    open({
      title: 'Nuevo permiso',
      content: (close) => (
        <NuevoPermisoDialog
          vehiculos={vehiculos}
          idVehiculo={Number(idVehiculo ?? 0)}
          close={close}
          onCreated={mostrarCreado}
        />
      ),
    })
  }

  const conVehiculo = Boolean(idVehiculo && vehiculos.length)

  return (
    <div>
      <PageHeader
        title="Permisos"
        description="Permisos nacionales de circulación de los vehículos de la empresa."
        actions={
          <Button onClick={nuevoPermiso} disabled={!vehiculos.length}>
            <Plus />
            Nuevo permiso
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
        <LookupFilter
          label="Vehículo"
          value={idVehiculo}
          onChange={(v) => v && set('idVehiculo', v)}
          search={buscarVehiculos}
          resolve={vehiculoPorId}
          required
        />
      </DataToolbar>
      <Grid
        columns={columnas}
        items={conVehiculo ? (resultado?.items ?? []) : []}
        rowKey={(p) => p.id}
        empty={vehiculos.length ? 'No hay permisos registrados.' : 'La empresa no tiene vehículos.'}
        page={conVehiculo ? (resultado ?? undefined) : undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Permisos

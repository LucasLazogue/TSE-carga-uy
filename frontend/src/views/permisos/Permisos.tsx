import { useCallback, useEffect, useState } from 'react'
import type { ComponentProps } from 'react'
import { AlertCircle, Check, Pencil, Plus, RefreshCw, Trash2, X } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import FilterSelect from '@/components/toolbar/FilterSelect'
import LookupFilter from '@/components/toolbar/LookupFilter'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { getErrorMessage } from '@/api/errors'
import { TAMANIO_MAXIMO } from '@/api/page'
import type { Page } from '@/api/page'
import { PermisosApi } from '@/api/permisos/permisos.api'
import type { EstadoPermiso, Permiso } from '@/api/permisos/permisos.types'
import { VehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { useSession } from '@/auth/useSession'
import { useEmpresaInicial } from '@/hooks/useEmpresaInicial'
import { useListParams } from '@/hooks/useListParams'
import { fecha } from '@/lib/fechas'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import AprobarPermisoDialog from './dialogs/AprobarPermisoDialog'
import RechazarPermisoDialog from './dialogs/RechazarPermisoDialog'
import SolicitudPermisoDialog from './dialogs/SolicitudPermisoDialog'
import { NOMBRES_ESTADO, OPCIONES_ESTADO } from './permisos.constants'

const VARIANTES_ESTADO: Record<EstadoPermiso, ComponentProps<typeof Badge>['variant']> = {
  PENDIENTE: 'outline',
  APROBADO: 'secondary',
  RECHAZADO: 'destructive',
}

const columnas: GridColumn<Permiso>[] = [
  { header: 'Nro. permiso', cell: (p) => p.nroPermiso, className: 'font-medium' },
  { header: 'Vehículo', cell: (p) => p.matricula },
  { header: 'Válido desde', cell: (p) => fecha(p.validoDesde) },
  { header: 'Válido hasta', cell: (p) => (p.validoHasta ? fecha(p.validoHasta) : '-') },
  {
    header: 'Estado',
    className: 'max-w-xs whitespace-normal',
    cell: (p) => (
      <div className="space-y-1">
        <Badge variant={VARIANTES_ESTADO[p.estado]}>{NOMBRES_ESTADO[p.estado]}</Badge>
        {p.motivoRechazo && <p className="text-xs text-muted-foreground">{p.motivoRechazo}</p>}
      </div>
    ),
  },
]

const FILTROS = ['idEmpresa', 'idVehiculo', 'estado'] as const

function opcionVehiculo(v: Vehiculo) {
  return { value: String(v.id), label: v.matricula }
}

function Permisos() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [resultado, setResultado] = useState<Page<Permiso> | null>(null)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const { open, confirm } = useDialogs()
  const { profile } = useSession()
  const esFuncionario = profile === 'FUNCIONARIO'
  const { idEmpresa, idVehiculo, estado } = values
  const vehiculoValido = Boolean(idEmpresa) && vehiculos.some((v) => String(v.id) === idVehiculo)
  const filtroVehiculo = vehiculoValido ? Number(idVehiculo) : undefined

  useEmpresaInicial(idEmpresa, set, !esFuncionario)

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
    if (!esFuncionario && !vehiculoValido && vehiculos.length) {
      set('idVehiculo', String(vehiculos[0].id))
    }
  }, [esFuncionario, vehiculoValido, vehiculos, set])

  useEffect(() => {
    if (!esFuncionario && !filtroVehiculo) {
      return
    }
    let vigente = true
    const filtro = {
      idEmpresa: esFuncionario && idEmpresa ? Number(idEmpresa) : undefined,
      idVehiculo: filtroVehiculo,
      estado: estado as EstadoPermiso | undefined,
    }
    PermisosApi.findAll(filtro, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [esFuncionario, idEmpresa, filtroVehiculo, estado, pagina, version])

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
        <SolicitudPermisoDialog
          vehiculos={vehiculos}
          idVehiculo={Number(idVehiculo ?? 0)}
          close={close}
          onSaved={mostrarCreado}
        />
      ),
    })
  }

  function corregir(permiso: Permiso) {
    open({
      title: 'Corregir permiso',
      content: (close) => (
        <SolicitudPermisoDialog
          vehiculos={vehiculos}
          idVehiculo={permiso.idVehiculo}
          permiso={permiso}
          close={close}
          onSaved={mostrarCreado}
        />
      ),
    })
  }

  async function retirar(permiso: Permiso) {
    const ok = await confirm({
      title: 'Retirar permiso',
      description: `Se va a eliminar la solicitud del permiso ${permiso.nroPermiso}.`,
      confirmText: 'Retirar',
      destructive: true,
    })
    if (!ok) {
      return
    }
    setError('')
    try {
      await PermisosApi.remove(permiso.id)
      recargar()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  function aprobar(permiso: Permiso) {
    open({
      title: 'Aprobar permiso',
      content: (close) => <AprobarPermisoDialog permiso={permiso} close={close} onSaved={recargar} />,
    })
  }

  function rechazar(permiso: Permiso) {
    open({
      title: 'Rechazar permiso',
      content: (close) => <RechazarPermisoDialog permiso={permiso} close={close} onSaved={recargar} />,
    })
  }

  function acciones(p: Permiso) {
    if (esFuncionario) {
      return (
        p.estado === 'PENDIENTE' && (
          <div className="flex justify-end gap-1">
            <Button size="sm" variant="outline" onClick={() => aprobar(p)}>
              <Check />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Aprobar</span>
            </Button>
            <Button size="sm" variant="outline" onClick={() => rechazar(p)}>
              <X />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Rechazar</span>
            </Button>
          </div>
        )
      )
    }
    return (
      p.estado !== 'APROBADO' && (
        <div className="flex justify-end gap-1">
          <Button size="sm" variant="outline" onClick={() => corregir(p)}>
            <Pencil />
            <span className="sr-only md:not-sr-only md:whitespace-nowrap">Corregir</span>
          </Button>
          <Button size="sm" variant="outline" onClick={() => retirar(p)}>
            <Trash2 />
            <span className="sr-only md:not-sr-only md:whitespace-nowrap">Retirar</span>
          </Button>
        </div>
      )
    )
  }

  const columnasConAcciones: GridColumn<Permiso>[] = [
    ...columnas,
    {
      header: 'Acciones',
      hideHeader: true,
      className: 'sticky right-0 w-px bg-background text-right',
      cell: acciones,
    },
  ]

  const mostrar = esFuncionario || Boolean(filtroVehiculo)

  return (
    <div>
      <PageHeader
        title="Permisos"
        description={
          esFuncionario
            ? 'Permisos nacionales de circulación de los vehículos.'
            : 'Permisos nacionales de circulación de los vehículos de la empresa.'
        }
        actions={
          !esFuncionario && (
            <Button onClick={nuevoPermiso} disabled={!vehiculos.length}>
              <Plus />
              Nuevo permiso
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
        <LookupFilter
          label="Empresa"
          value={idEmpresa}
          onChange={(v) => (esFuncionario || v) && set('idEmpresa', v)}
          {...fuenteEmpresas}
          required={!esFuncionario}
        />
        {idEmpresa && (
          <LookupFilter
            label="Vehículo"
            value={vehiculoValido ? idVehiculo : undefined}
            onChange={(v) => (esFuncionario || v) && set('idVehiculo', v)}
            search={buscarVehiculos}
            resolve={vehiculoPorId}
            required={!esFuncionario}
          />
        )}
        <FilterSelect label="Estado" value={estado} options={OPCIONES_ESTADO} onChange={(v) => set('estado', v)} />
      </DataToolbar>
      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <Grid
        columns={columnasConAcciones}
        items={mostrar ? (resultado?.items ?? []) : []}
        rowKey={(p) => p.id}
        empty={esFuncionario || vehiculos.length ? 'No hay permisos registrados.' : 'La empresa no tiene vehículos.'}
        page={mostrar ? (resultado ?? undefined) : undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Permisos

import { useEffect, useState } from 'react'
import type { ComponentProps } from 'react'
import { AlertCircle, History, MapPinned, RefreshCw, Repeat, Trash2 } from 'lucide-react'
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
import { GuiasApi } from '@/api/guias/guias.api'
import type { Page } from '@/api/page'
import { ViajesApi } from '@/api/viajes/viajes.api'
import type { EstadoViaje, Viaje } from '@/api/viajes/viajes.types'
import { useSession } from '@/auth/useSession'
import { useEmpresaInicial } from '@/hooks/useEmpresaInicial'
import { useListParams } from '@/hooks/useListParams'
import { fechaHora } from '@/lib/fechas'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import AsignarViajeDialog from '@/views/guias/dialogs/AsignarViajeDialog'
import EventosViajeDialog from './dialogs/EventosViajeDialog'
import RecorridoDialog from './dialogs/RecorridoDialog'
import { NOMBRES_ESTADO, OPCIONES_ESTADO } from './viajes.constants'

const VARIANTES_ESTADO: Record<EstadoViaje, ComponentProps<typeof Badge>['variant']> = {
  ASIGNADO: 'outline',
  EN_CURSO: 'default',
  FINALIZADO: 'secondary',
}

const columnas: GridColumn<Viaje>[] = [
  { header: 'Nro. guía', cell: (v) => v.nroGuia, className: 'font-medium' },
  { header: 'Vehículo', cell: (v) => v.matricula },
  { header: 'Chofer', cell: (v) => v.cedulaChofer },
  { header: 'Estado', cell: (v) => <Badge variant={VARIANTES_ESTADO[v.estado]}>{NOMBRES_ESTADO[v.estado]}</Badge> },
  { header: 'Inicio', cell: (v) => (v.fechaInicio ? fechaHora(v.fechaInicio) : '-') },
  { header: 'Fin', cell: (v) => (v.fechaFin ? fechaHora(v.fechaFin) : '-') },
]

const FILTROS = ['idEmpresa', 'estado'] as const

function Viajes() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Viaje> | null>(null)
  const [version, setVersion] = useState(0)
  const [error, setError] = useState('')
  const { open, confirm } = useDialogs()
  const { profile } = useSession()
  const esFuncionario = profile === 'FUNCIONARIO'
  const { idEmpresa, estado } = values

  useEmpresaInicial(idEmpresa, set)

  useEffect(() => {
    if (!idEmpresa) {
      return
    }
    let vigente = true
    ViajesApi.findAll(Number(idEmpresa), { estado: estado as EstadoViaje | undefined }, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa, estado, pagina, version])

  function recargar() {
    setVersion((v) => v + 1)
  }

  function verEventos(viaje: Viaje) {
    open({
      title: 'Eventos del viaje',
      size: 'xl',
      content: (close) => <EventosViajeDialog idEmpresa={Number(idEmpresa)} viaje={viaje} close={close} />,
    })
  }

  function verRecorrido(viaje: Viaje) {
    open({
      title: 'Recorrido del viaje',
      size: 'xl',
      content: (close) => <RecorridoDialog idEmpresa={Number(idEmpresa)} viaje={viaje} close={close} />,
    })
  }

  // el dialogo necesita la guia (fecha y volumen), que el viaje no trae
  async function reasignar(viaje: Viaje) {
    setError('')
    try {
      const guia = await GuiasApi.find(Number(idEmpresa), viaje.idGuia)
      open({
        title: 'Reasignar viaje',
        content: (close) => <AsignarViajeDialog guia={guia} viaje={viaje} close={close} onAssigned={recargar} />,
      })
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  async function eliminar(viaje: Viaje) {
    const ok = await confirm({
      title: 'Eliminar viaje',
      description: `Se va a eliminar el viaje de la guía ${viaje.nroGuia}, que queda sin asignar.`,
      confirmText: 'Eliminar',
      destructive: true,
    })
    if (!ok) {
      return
    }
    setError('')
    try {
      await ViajesApi.remove(Number(idEmpresa), viaje.id)
      recargar()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  const columnasConAcciones: GridColumn<Viaje>[] = [
    ...columnas,
    {
      header: 'Acciones',
      hideHeader: true,
      className: 'sticky right-0 w-px bg-background text-right',
      cell: (v) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="outline" onClick={() => verEventos(v)}>
            <History />
            <span className="sr-only md:not-sr-only md:whitespace-nowrap">Ver eventos</span>
          </Button>
          {v.estado !== 'ASIGNADO' && (
            <Button size="sm" variant="outline" onClick={() => verRecorrido(v)}>
              <MapPinned />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Ver recorrido</span>
            </Button>
          )}
          {!esFuncionario && v.estado === 'ASIGNADO' && (
            <>
              <Button size="sm" variant="outline" onClick={() => reasignar(v)}>
                <Repeat />
                <span className="sr-only md:not-sr-only md:whitespace-nowrap">Reasignar</span>
              </Button>
              <Button size="sm" variant="outline" onClick={() => eliminar(v)}>
                <Trash2 />
                <span className="sr-only md:not-sr-only md:whitespace-nowrap">Eliminar</span>
              </Button>
            </>
          )}
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Viajes"
        description="Viajes asignados a los vehículos y choferes de la empresa."
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
        items={resultado?.items ?? []}
        rowKey={(v) => v.id}
        empty="No hay viajes registrados."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Viajes

import { useEffect, useState } from 'react'
import { Pencil, Plus, RefreshCw, Truck } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import DataToolbar from '@/components/toolbar/DataToolbar'
import FilterSelect from '@/components/toolbar/FilterSelect'
import type { FilterOption } from '@/components/toolbar/FilterSelect'
import LookupFilter from '@/components/toolbar/LookupFilter'
import SearchInput from '@/components/toolbar/SearchInput'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { GuiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import type { Page } from '@/api/page'
import { useSession } from '@/auth/useSession'
import { useEmpresaInicial } from '@/hooks/useEmpresaInicial'
import { useListParams } from '@/hooks/useListParams'
import { coordenadas } from '@/lib/coordenadas'
import { fecha } from '@/lib/fechas'
import { fuenteEmpresas } from '@/components/lookup/fuente'
import AsignarViajeDialog from './dialogs/AsignarViajeDialog'
import GuiaDialog from './dialogs/GuiaDialog'

const columnas: GridColumn<Guia>[] = [
  { header: 'Nro. guía', cell: (g) => g.nroGuia, className: 'font-medium' },
  { header: 'Fecha', cell: (g) => fecha(g.fecha) },
  { header: 'Rubro', cell: (g) => g.nombreRubro },
  { header: 'Tipo de carga', cell: (g) => g.nombreTipoCarga },
  { header: 'Volumen (kg)', cell: (g) => g.volumen.toLocaleString('es-UY'), className: 'text-right' },
  { header: 'Origen', cell: (g) => coordenadas(g.origenLat, g.origenLon) },
  { header: 'Destino', cell: (g) => coordenadas(g.destinoLat, g.destinoLon) },
  {
    header: 'Viaje',
    cell: (g) =>
      g.idViaje == null ? <Badge variant="outline">Sin asignar</Badge> : <Badge variant="secondary">Asignado</Badge>,
  },
]

const FILTROS = ['busqueda', 'idEmpresa', 'conViaje', 'desde', 'hasta'] as const

const OPCIONES_VIAJE: FilterOption[] = [
  { value: 'true', label: 'Asignado' },
  { value: 'false', label: 'Sin asignar' },
]

function Guias() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Guia> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { profile } = useSession()
  const esFuncionario = profile === 'FUNCIONARIO'
  const { busqueda, idEmpresa, conViaje, desde, hasta } = values

  useEmpresaInicial(idEmpresa, set)

  useEffect(() => {
    if (!idEmpresa) {
      return
    }
    let vigente = true
    GuiasApi
      .findAll(
        Number(idEmpresa),
        {
          busqueda,
          conViaje: conViaje ? conViaje === 'true' : undefined,
          desde,
          hasta,
        },
        { pagina },
      )
      .then((page) => {
        if (vigente) {
          setResultado(page)
        }
      })
    return () => {
      vigente = false
    }
  }, [idEmpresa, busqueda, conViaje, desde, hasta, pagina, version])

  function mostrarCreada(guia: Guia) {
    if (String(guia.idEmpresa) === idEmpresa) {
      recargar()
    } else {
      set('idEmpresa', String(guia.idEmpresa))
    }
  }

  function recargar() {
    setVersion((v) => v + 1)
  }

  function asignarViaje(guia: Guia) {
    open({
      title: 'Asignar viaje',
      content: (close) => <AsignarViajeDialog guia={guia} close={close} onAssigned={recargar} />,
    })
  }

  // solo sin viaje: con un viaje asignado el central ya no deja cambiar la fecha ni el volumen
  function editarGuia(guia: Guia) {
    open({
      title: `Editar guía ${guia.nroGuia}`,
      size: 'lg',
      content: (close) => <GuiaDialog idEmpresa={guia.idEmpresa} guia={guia} close={close} onSaved={recargar} />,
    })
  }

  const columnasConAcciones: GridColumn<Guia>[] = [
    ...columnas,
    {
      header: 'Acciones',
      hideHeader: true,
      className: 'sticky right-0 w-px bg-background text-right',
      cell: (g) =>
        !esFuncionario && g.idViaje == null && (
          <div className="flex justify-end gap-2">
            <Button size="sm" variant="outline" onClick={() => editarGuia(g)}>
              <Pencil />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Editar</span>
            </Button>
            <Button size="sm" variant="outline" onClick={() => asignarViaje(g)}>
              <Truck />
              <span className="sr-only md:not-sr-only md:whitespace-nowrap">Asignar viaje</span>
            </Button>
          </div>
        ),
    },
  ]

  function nuevaGuia() {
    if (!idEmpresa) {
      return
    }
    open({
      title: 'Nueva guía',
      size: 'lg',
      content: (close) => <GuiaDialog idEmpresa={Number(idEmpresa)} close={close} onSaved={mostrarCreada} />,
    })
  }

  return (
    <div>
      <PageHeader
        title="Guías"
        description="Guías de carga registradas por la empresa."
        actions={
          !esFuncionario && (
            <Button onClick={nuevaGuia} disabled={!idEmpresa}>
              <Plus />
              Nueva guía
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
        <SearchInput placeholder="Buscar por nro. de guía..." value={busqueda} onChange={(v) => set('busqueda', v)} />
        <LookupFilter
          label="Empresa"
          value={idEmpresa}
          onChange={(v) => v && set('idEmpresa', v)}
          {...fuenteEmpresas}
          required
        />
        <FilterSelect label="Viaje" value={conViaje} options={OPCIONES_VIAJE} onChange={(v) => set('conViaje', v)} />
      </DataToolbar>
      <Grid
        columns={columnasConAcciones}
        items={resultado?.items ?? []}
        rowKey={(g) => g.id}
        empty="No hay guías registradas."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
    </div>
  )
}

export default Guias

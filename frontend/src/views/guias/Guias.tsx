import { useEffect, useState } from 'react'
import { Plus, RefreshCw } from 'lucide-react'
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
import { empresasApi } from '@/api/empresas/empresas.api'
import { guiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import type { Page } from '@/api/page'
import { useListParams } from '@/hooks/useListParams'
import NuevaGuiaDialog from './dialogs/NuevaGuiaDialog'

function fecha(iso: string) {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}

function coordenadas(lat: number, lon: number) {
  return `${lat.toFixed(4)}, ${lon.toFixed(4)}`
}

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
      g.idViaje === null ? <Badge variant="outline">Sin asignar</Badge> : <Badge variant="secondary">Asignado</Badge>,
  },
]

const FILTROS = ['busqueda', 'idEmpresa', 'conViaje', 'desde', 'hasta'] as const

const OPCIONES_VIAJE: FilterOption[] = [
  { value: 'true', label: 'Asignado' },
  { value: 'false', label: 'Sin asignar' },
]

async function buscarEmpresas(texto: string) {
  const empresas = await empresasApi.getAll(texto)
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

function Guias() {
  const { values, pagina, set, setPagina } = useListParams(FILTROS)
  const [resultado, setResultado] = useState<Page<Guia> | null>(null)
  const [version, setVersion] = useState(0)
  const { open } = useDialogs()
  const { busqueda, idEmpresa, conViaje, desde, hasta } = values

  useEffect(() => {
    if (idEmpresa) {
      return
    }
    let vigente = true
    empresasApi.getAll().then((empresas) => {
      if (vigente && empresas.length) {
        set('idEmpresa', String(empresas[0].id))
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa, set])

  useEffect(() => {
    if (!idEmpresa) {
      return
    }
    let vigente = true
    guiasApi
      .getAll(Number(idEmpresa), {
        busqueda,
        conViaje: conViaje ? conViaje === 'true' : undefined,
        desde,
        hasta,
        pagina,
      })
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

  function nuevaGuia() {
    if (!idEmpresa) {
      return
    }
    open({
      title: 'Nueva guía',
      size: 'lg',
      content: (close) => <NuevaGuiaDialog idEmpresa={Number(idEmpresa)} close={close} onCreated={mostrarCreada} />,
    })
  }

  return (
    <div>
      <PageHeader
        title="Guías"
        description="Guías de carga registradas por la empresa."
        actions={
          <Button onClick={nuevaGuia} disabled={!idEmpresa}>
            <Plus />
            Nueva guía
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
        <SearchInput placeholder="Buscar por nro. de guía..." value={busqueda} onChange={(v) => set('busqueda', v)} />
        <LookupFilter
          label="Empresa"
          value={idEmpresa}
          onChange={(v) => v && set('idEmpresa', v)}
          search={buscarEmpresas}
          resolve={empresaPorId}
          required
        />
        <FilterSelect label="Viaje" value={conViaje} options={OPCIONES_VIAJE} onChange={(v) => set('conViaje', v)} />
      </DataToolbar>
      <Grid
        columns={columnas}
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

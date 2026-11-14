import { useEffect, useState } from 'react'
import PageHeader from '@/components/PageHeader'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import DataToolbar from '@/components/toolbar/DataToolbar'
import FilterSelect from '@/components/toolbar/FilterSelect'
import type { FilterOption } from '@/components/toolbar/FilterSelect'
import { PublicoApi } from '@/api/publico/publico.api'
import type { IndicadoresPublicos, ViajesPorMes, VolumenPorRubro } from '@/api/publico/publico.types'
import { useListParams } from '@/hooks/useListParams'
import { fecha } from '@/lib/fechas'

const ANIO_ACTUAL = new Date().getFullYear()

const OPCIONES_ANIO: FilterOption[] = Array.from({ length: 5 }, (_, i) => String(ANIO_ACTUAL - i)).map((a) => ({
  value: a,
  label: a,
}))

const columnasViajes: GridColumn<ViajesPorMes>[] = [
  { header: 'Mes', cell: (v) => `${v.mes.slice(5)}/${v.mes.slice(0, 4)}`, className: 'font-medium' },
  { header: 'Viajes', cell: (v) => v.cantidad?.toLocaleString('es-UY') ?? '-', className: 'text-right' },
]

const columnasVolumen: GridColumn<VolumenPorRubro>[] = [
  { header: 'Rubro', cell: (v) => v.rubro, className: 'font-medium' },
  { header: 'Volumen (kg)', cell: (v) => v.volumen?.toLocaleString('es-UY') ?? '-', className: 'text-right' },
]

const FILTROS = ['anio', 'rubro', 'tipoCarga'] as const

function vacio(filas: unknown[]) {
  return filas.length > 0 ? 'La consulta es demasiado específica.' : 'No hay viajes en el período.'
}

function Estadisticas() {
  const { values, set } = useListParams(FILTROS)
  const [indicadores, setIndicadores] = useState<IndicadoresPublicos | null>(null)
  const [rubros, setRubros] = useState<FilterOption[]>([])
  const [tipos, setTipos] = useState<FilterOption[]>([])
  const { anio = String(ANIO_ACTUAL), rubro, tipoCarga } = values

  useEffect(() => {
    PublicoApi.getRubros().then((r) => setRubros(r.map((x) => ({ value: String(x.id), label: x.nombre }))))
    PublicoApi.getTiposCarga().then((t) => setTipos(t.map((x) => ({ value: String(x.id), label: x.nombre }))))
  }, [])

  useEffect(() => {
    let vigente = true
    PublicoApi.getIndicadores({
      desde: `${anio}-01-01`,
      hasta: `${anio}-12-31`,
      rubro: rubro ? Number(rubro) : undefined,
      tipoCarga: tipoCarga ? Number(tipoCarga) : undefined,
    }).then((r) => {
      if (vigente) {
        setIndicadores(r)
      }
    })
    return () => {
      vigente = false
    }
  }, [anio, rubro, tipoCarga])

  const viajes = indicadores?.viajesPorMes ?? []
  const volumen = indicadores?.volumenPorRubro ?? []

  return (
    <div>
      <PageHeader
        title="Estadísticas"
        description="Actividad del transporte de carga. No se publican datos de menos de 5 empresas o 5 vehículos."
      />
      <DataToolbar>
        <FilterSelect label="Año" value={anio} options={OPCIONES_ANIO} onChange={(v) => set('anio', v)} />
        <FilterSelect label="Rubro" value={rubro} options={rubros} onChange={(v) => set('rubro', v)} />
        <FilterSelect label="Tipo de carga" value={tipoCarga} options={tipos} onChange={(v) => set('tipoCarga', v)} />
      </DataToolbar>
      {indicadores?.empresasHabilitadas != null && (
        <p className="mb-4 text-sm text-muted-foreground">
          Al {fecha(indicadores.habilitadosAl)} hay {indicadores.empresasHabilitadas.toLocaleString('es-UY')} empresas
          y {indicadores.vehiculosHabilitados?.toLocaleString('es-UY')} vehículos habilitados.
        </p>
      )}
      <div className="grid gap-6 lg:grid-cols-2">
        <Grid
          columns={columnasViajes}
          items={viajes.some((v) => v.cantidad != null) ? viajes : []}
          rowKey={(v) => v.mes}
          empty={vacio(viajes)}
        />
        <Grid
          columns={columnasVolumen}
          items={volumen.some((v) => v.volumen != null) ? volumen : []}
          rowKey={(v) => v.rubro}
          empty={vacio(volumen)}
        />
      </div>
    </div>
  )
}

export default Estadisticas

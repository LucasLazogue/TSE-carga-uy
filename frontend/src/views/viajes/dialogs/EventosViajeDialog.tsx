import { useEffect, useState } from 'react'
import type { ComponentProps } from 'react'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import type { Page } from '@/api/page'
import { ViajesApi } from '@/api/viajes/viajes.api'
import type { EventoViaje, ResultadoEvento, Viaje } from '@/api/viajes/viajes.types'
import { momento } from '@/lib/fechas'
import { NOMBRES_RESULTADO, NOMBRES_TIPO_EVENTO } from '../viajes.constants'

const VARIANTES_RESULTADO: Record<ResultadoEvento, ComponentProps<typeof Badge>['variant']> = {
  APLICADO: 'secondary',
  DUPLICADO: 'outline',
  CONFLICTO: 'destructive',
  RECHAZADO: 'destructive',
}

// lo que cargo el chofer: "3.500 kg · Entregado en el deposito", o solo una de las dos partes
function detalleDelChofer(e: EventoViaje): string {
  const partes = [e.kilos != null ? `${e.kilos.toLocaleString('es-UY')} kg` : null, e.descripcion]
  return partes.filter(Boolean).join(' · ') || '-'
}

const columnas: GridColumn<EventoViaje>[] = [
  { header: 'Evento', cell: (e) => NOMBRES_TIPO_EVENTO[e.tipo], className: 'font-medium' },
  { header: 'Detalle', cell: detalleDelChofer, className: 'min-w-56 whitespace-normal' },
  // en un viaje reasignado hay eventos de los dos choferes
  { header: 'Chofer', cell: (e) => e.cedulaChofer ?? '-' },
  { header: 'Generado', cell: (e) => momento(e.timestampGeneracion) },
  { header: 'Recibido', cell: (e) => momento(e.timestampRecepcion) },
  {
    header: 'Resultado',
    cell: (e) => <Badge variant={VARIANTES_RESULTADO[e.resultado]}>{NOMBRES_RESULTADO[e.resultado]}</Badge>,
  },
  { header: 'Motivo', cell: (e) => e.detalle ?? '-', className: 'min-w-48 whitespace-normal text-muted-foreground' },
]

type Props = {
  idEmpresa: number
  viaje: Viaje
  close: () => void
}

function EventosViajeDialog({ idEmpresa, viaje, close }: Props) {
  const [resultado, setResultado] = useState<Page<EventoViaje> | null>(null)
  const [pagina, setPagina] = useState(0)

  useEffect(() => {
    let vigente = true
    ViajesApi.findEventos(idEmpresa, viaje.id, { pagina }).then((page) => {
      if (vigente) {
        setResultado(page)
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa, viaje.id, pagina])

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Guía {viaje.nroGuia}, vehículo {viaje.matricula}, chofer {viaje.cedulaChofer}.
      </p>
      <Grid
        columns={columnas}
        items={resultado?.items ?? []}
        rowKey={(e) => e.idEvento}
        empty="El chofer todavía no registró eventos."
        page={resultado ?? undefined}
        onPageChange={setPagina}
      />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cerrar
        </Button>
      </DialogFooter>
    </div>
  )
}

export default EventosViajeDialog

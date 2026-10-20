import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import type { Caso, Evidencia, PistaAuditoria } from '@/api/casos/casos.types'
import { fechaHora } from '@/lib/fechas'
import { NOMBRES_ACCION, NOMBRES_ESTADO, NOMBRES_TIPO, VARIANTES_ESTADO } from '../casos.constants'

const columnasEvidencia: GridColumn<Evidencia>[] = [
  { header: 'Fecha', cell: (e) => fechaHora(e.fechaEvento) },
  { header: 'Observado', cell: (e) => e.valorObservado ?? '-' },
  { header: 'Declarado', cell: (e) => e.valorDeclarado ?? '-' },
  { header: 'Descripción', cell: (e) => e.descripcion ?? '-', className: 'whitespace-normal' },
]

const columnasAuditoria: GridColumn<PistaAuditoria>[] = [
  { header: 'Fecha', cell: (p) => fechaHora(p.fecha) },
  { header: 'Acción', cell: (p) => NOMBRES_ACCION[p.accion], className: 'font-medium' },
  { header: 'Estado', cell: (p) => NOMBRES_ESTADO[p.estadoNuevo] },
  { header: 'Usuario', cell: (p) => p.usuario },
  { header: 'Fundamento', cell: (p) => p.fundamento ?? '-', className: 'whitespace-normal' },
]

type Props = {
  caso: Caso
  auditoria?: PistaAuditoria[]
  close: () => void
}

function CasoDialog({ caso, auditoria, close }: Props) {
  return (
    <div className="space-y-4 text-sm">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={VARIANTES_ESTADO[caso.estado]}>{NOMBRES_ESTADO[caso.estado]}</Badge>
        <span className="text-muted-foreground">
          {NOMBRES_TIPO[caso.tipo]} de {caso.nombreEmpresa}, vehículo {caso.matricula}.
          {caso.venceDescargo && ` Descargo hasta el ${fechaHora(caso.venceDescargo)}.`}
        </span>
      </div>
      <Grid columns={columnasEvidencia} items={caso.evidencias} rowKey={(e) => e.id} empty="Sin evidencia." />
      <div className="space-y-1">
        <p className="font-medium">Descargo</p>
        {caso.descargo ? (
          <p className="whitespace-pre-wrap">
            {caso.descargo.texto}
            <span className="block text-muted-foreground">
              {caso.descargo.cedulaPresentadoPor}, {fechaHora(caso.descargo.fecha)}
            </span>
          </p>
        ) : (
          <p className="text-muted-foreground">No se presentó descargo.</p>
        )}
      </div>
      {caso.resolucion && (
        <div className="space-y-1">
          <p className="font-medium">Resolución: {NOMBRES_ESTADO[caso.resolucion.resultado]}</p>
          <p className="whitespace-pre-wrap">{caso.resolucion.fundamento}</p>
        </div>
      )}
      {auditoria && (
        <div className="space-y-1">
          <p className="font-medium">Auditoría</p>
          <Grid columns={columnasAuditoria} items={auditoria} rowKey={(p) => p.id} empty="Sin registros." />
        </div>
      )}
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cerrar
        </Button>
      </DialogFooter>
    </div>
  )
}

export default CasoDialog

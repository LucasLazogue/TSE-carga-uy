import type { ReactNode } from 'react'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Caso, Evidencia } from '@/api/casos/casos.types'
import { TIPOS_EVIDENCIA, TIPOS_INFRACCION, fechaHora } from './casos.constants'
import EstadoBadge from './EstadoBadge'

const columnasEvidencia: GridColumn<Evidencia>[] = [
  { header: 'Tipo', cell: (e) => TIPOS_EVIDENCIA[e.tipo], className: 'font-medium' },
  { header: 'Fecha', cell: (e) => fechaHora(e.fechaEvento) },
  { header: 'Observado', cell: (e) => e.valorObservado ?? '—' },
  { header: 'Declarado', cell: (e) => e.valorDeclarado ?? '—' },
  { header: 'Umbral', cell: (e) => e.umbral ?? '—', className: 'min-w-32 whitespace-normal' },
  { header: 'Descripción', cell: (e) => e.descripcion ?? '—', className: 'min-w-48 whitespace-normal' },
  { header: 'Origen', cell: (e) => e.idOrigen ?? '—', className: 'min-w-36 break-all whitespace-normal text-muted-foreground' },
]

function Dato({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="space-y-1">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}

function DetalleCaso({ caso }: { caso: Caso }) {
  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Datos del caso</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <Dato label="Tipo">{TIPOS_INFRACCION[caso.tipo]}</Dato>
            <Dato label="Estado">
              <EstadoBadge estado={caso.estado} />
            </Dato>
            <Dato label="Empresa">{caso.nombreEmpresa}</Dato>
            <Dato label="Vehículo">{caso.matricula}</Dato>
            <Dato label="Fecha del hecho">{fechaHora(caso.fechaHecho)}</Dato>
            <Dato label="Detectado">{fechaHora(caso.fechaDeteccion)}</Dato>
            <Dato label="Notificado">{fechaHora(caso.fechaNotificacion)}</Dato>
            <Dato label="Vence el descargo">{fechaHora(caso.venceDescargo)}</Dato>
          </dl>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Evidencia</CardTitle>
          <CardDescription>Datos que dieron origen al caso.</CardDescription>
        </CardHeader>
        <CardContent>
          <Grid
            columns={columnasEvidencia}
            items={caso.evidencias}
            rowKey={(e) => e.id}
            empty="El caso no tiene evidencia registrada."
          />
        </CardContent>
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Descargo</CardTitle>
            {caso.descargo && (
              <CardDescription>
                Presentado por {caso.descargo.cedulaPresentadoPor} el {fechaHora(caso.descargo.fecha)}.
              </CardDescription>
            )}
          </CardHeader>
          <CardContent>
            {caso.descargo ? (
              <p className="whitespace-pre-wrap">{caso.descargo.texto}</p>
            ) : (
              <p className="text-muted-foreground">No se presentó descargo.</p>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Resolución</CardTitle>
            {caso.resolucion && (
              <CardDescription>
                Resuelto{caso.resolucion.cedulaFuncionario && ` por ${caso.resolucion.cedulaFuncionario}`} el{' '}
                {fechaHora(caso.resolucion.fecha)}.
              </CardDescription>
            )}
          </CardHeader>
          <CardContent>
            {caso.resolucion ? (
              <div className="space-y-2">
                <EstadoBadge estado={caso.resolucion.resultado} />
                <p className="whitespace-pre-wrap">{caso.resolucion.fundamento}</p>
              </div>
            ) : (
              <p className="text-muted-foreground">El caso todavía no fue resuelto.</p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

export default DetalleCaso

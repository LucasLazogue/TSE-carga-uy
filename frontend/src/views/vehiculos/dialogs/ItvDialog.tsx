import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle, Plus, Trash2 } from 'lucide-react'
import Grid from '@/components/grid/Grid'
import type { GridColumn } from '@/components/grid/Grid'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { getErrorMessage } from '@/api/errors'
import { ItvApi } from '@/api/itv/itv.api'
import type { Itv, ItvNueva } from '@/api/itv/itv.types'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { fecha } from '@/lib/fechas'

const ITV_VACIA: ItvNueva = { fechaInspeccion: '', fechaVencimiento: '' }

type Props = {
  vehiculo: Vehiculo
  soloLectura: boolean
  close: () => void
}

function ItvDialog({ vehiculo, soloLectura, close }: Props) {
  const [itvs, setItvs] = useState<Itv[]>([])
  const [version, setVersion] = useState(0)
  const [datos, setDatos] = useState<ItvNueva>(ITV_VACIA)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')
  const { confirm } = useDialogs()

  useEffect(() => {
    let vigente = true
    ItvApi.findAll(vehiculo.id).then((lista) => {
      if (vigente) {
        setItvs(lista)
      }
    })
    return () => {
      vigente = false
    }
  }, [vehiculo.id, version])

  function recargar() {
    setVersion((v) => v + 1)
  }

  async function agregar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await ItvApi.create(vehiculo.id, datos)
      setDatos(ITV_VACIA)
      recargar()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  async function quitar(itv: Itv) {
    const ok = await confirm({
      title: 'Quitar ITV',
      description: `Se va a eliminar la ITV que vence el ${fecha(itv.fechaVencimiento)}.`,
      confirmText: 'Quitar',
      destructive: true,
    })
    if (!ok) {
      return
    }
    setError('')
    try {
      await ItvApi.remove(vehiculo.id, itv.id)
      recargar()
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  // hoy como yyyy-mm-dd en hora local, para compararlo con las fechas de la ITV
  const hoy = new Date().toLocaleDateString('sv-SE')

  const columnas: GridColumn<Itv>[] = [
    { header: 'Inspección', cell: (i) => fecha(i.fechaInspeccion) },
    { header: 'Vence', cell: (i) => fecha(i.fechaVencimiento) },
    {
      header: 'Estado',
      cell: (i) =>
        i.fechaInspeccion <= hoy && hoy <= i.fechaVencimiento ? (
          <Badge variant="secondary">Vigente</Badge>
        ) : (
          <Badge variant="outline">Vencida</Badge>
        ),
    },
  ]
  if (!soloLectura) {
    columnas.push({
      header: 'Acciones',
      hideHeader: true,
      className: 'w-px text-right',
      cell: (i) => (
        <Button size="sm" variant="outline" onClick={() => quitar(i)}>
          <Trash2 />
          <span className="sr-only md:not-sr-only md:whitespace-nowrap">Quitar</span>
        </Button>
      ),
    })
  }

  return (
    <div className="space-y-4">
      {error && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <p className="text-sm text-muted-foreground">Inspecciones técnicas del vehículo {vehiculo.matricula}.</p>
      {!soloLectura && (
        <form onSubmit={agregar} className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <div className="space-y-1.5">
            <Label htmlFor="fechaInspeccion">Fecha de inspección</Label>
            <Input
              id="fechaInspeccion"
              type="date"
              required
              value={datos.fechaInspeccion}
              onChange={(e) => setDatos({ ...datos, fechaInspeccion: e.target.value })}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="fechaVencimiento">Fecha de vencimiento</Label>
            <Input
              id="fechaVencimiento"
              type="date"
              required
              min={datos.fechaInspeccion}
              value={datos.fechaVencimiento}
              onChange={(e) => setDatos({ ...datos, fechaVencimiento: e.target.value })}
            />
          </div>
          <Button type="submit" disabled={guardando}>
            <Plus />
            Agregar
          </Button>
        </form>
      )}
      <Grid columns={columnas} items={itvs} rowKey={(i) => i.id} empty="El vehículo no tiene ITV registradas." />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cerrar
        </Button>
      </DialogFooter>
    </div>
  )
}

export default ItvDialog

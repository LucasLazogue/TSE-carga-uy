import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertCircle } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { getErrorMessage } from '@/api/errors'
import { TAMANIO_MAXIMO } from '@/api/page'
import { VehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { VehiculoNuevo } from '@/api/vehiculos/vehiculos.types'
import { VEHICULO_VACIO } from '../vehiculos.constants'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  idEmpresa: number
  close: () => void
  onCreated: (idEmpresa: number) => void
}

function NuevoVehiculoDialog({ idEmpresa, close, onCreated }: Props) {
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [nuevo, setNuevo] = useState<VehiculoNuevo>({ ...VEHICULO_VACIO, idEmpresa })
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    EmpresasApi.findAll({}, { tamanio: TAMANIO_MAXIMO }).then((page) => setEmpresas(page.items))
  }, [])

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    setGuardando(true)
    try {
      await VehiculosApi.create(nuevo)
      onCreated(nuevo.idEmpresa)
      close()
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form onSubmit={guardar} className="space-y-4">
      {error && (
        <Alert variant="destructive">
          <AlertCircle />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="empresa">Empresa</Label>
          <select
            id="empresa"
            className={selectClass}
            value={nuevo.idEmpresa}
            onChange={(e) => setNuevo({ ...nuevo, idEmpresa: Number(e.target.value) })}
          >
            {empresas.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nombrePublico}
              </option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5 sm:col-span-2">
          <Label htmlFor="matricula">Matrícula</Label>
          <Input
            id="matricula"
            value={nuevo.matricula}
            onChange={(e) => setNuevo({ ...nuevo, matricula: e.target.value })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="marca">Marca</Label>
          <Input id="marca" value={nuevo.marca} onChange={(e) => setNuevo({ ...nuevo, marca: e.target.value })} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="modelo">Modelo</Label>
          <Input id="modelo" value={nuevo.modelo} onChange={(e) => setNuevo({ ...nuevo, modelo: e.target.value })} />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="peso">Peso (kg)</Label>
          <Input
            id="peso"
            type="number"
            value={nuevo.pesoVehiculo || ''}
            onChange={(e) => setNuevo({ ...nuevo, pesoVehiculo: Number(e.target.value) })}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="capacidad">Capacidad de carga (kg)</Label>
          <Input
            id="capacidad"
            type="number"
            value={nuevo.capacidadCarga || ''}
            onChange={(e) => setNuevo({ ...nuevo, capacidadCarga: Number(e.target.value) })}
          />
        </div>
      </div>
      <DialogFooter>
        <Button type="button" variant="outline" onClick={close}>
          Cancelar
        </Button>
        <Button type="submit" disabled={guardando}>
          Guardar
        </Button>
      </DialogFooter>
    </form>
  )
}

export default NuevoVehiculoDialog

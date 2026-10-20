import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { DialogFooter } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { GuiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import { TAMANIO_MAXIMO } from '@/api/page'
import { fecha } from '@/lib/fechas'
import AsignarViajeDialog from '@/views/guias/dialogs/AsignarViajeDialog'

const selectClass =
  'h-8 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50'

type Props = {
  idEmpresa: number
  close: () => void
  onAssigned: () => void
}

function NuevoViajeDialog({ idEmpresa, close, onAssigned }: Props) {
  const [guias, setGuias] = useState<Guia[]>([])
  const [idGuia, setIdGuia] = useState(0)
  const guia = guias.find((g) => g.id === idGuia)

  useEffect(() => {
    GuiasApi.findAll(idEmpresa, { conViaje: false }, { tamanio: TAMANIO_MAXIMO }).then((page) => setGuias(page.items))
  }, [idEmpresa])

  return (
    <div className="space-y-4">
      <div className="space-y-1.5">
        <Label htmlFor="guia">Guía</Label>
        <select id="guia" className={selectClass} value={idGuia} onChange={(e) => setIdGuia(Number(e.target.value))}>
          <option value={0}>Seleccione una guía</option>
          {guias.map((g) => (
            <option key={g.id} value={g.id}>
              {g.nroGuia} - {fecha(g.fecha)}
            </option>
          ))}
        </select>
      </div>
      {guia ? (
        <AsignarViajeDialog key={guia.id} guia={guia} close={close} onAssigned={onAssigned} />
      ) : (
        <DialogFooter>
          <Button type="button" variant="outline" onClick={close}>
            Cancelar
          </Button>
        </DialogFooter>
      )}
    </div>
  )
}

export default NuevoViajeDialog

import { useEffect, useState } from 'react'
import { Bell } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { cn } from 'cn'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from '@/components/ui/popover'
import { NotificacionesApi } from '@/api/notificaciones/notificaciones.api'
import type { Notificacion } from '@/api/notificaciones/notificaciones.types'
import { useSession } from '@/auth/useSession'
import { momento } from '@/lib/fechas'

const RECARGA_MS = 60_000

function Notificaciones() {
  const { profile } = useSession()
  const navigate = useNavigate()
  const [notificaciones, setNotificaciones] = useState<Notificacion[]>([])
  const [abierto, setAbierto] = useState(false)
  const [version, setVersion] = useState(0)
  const base = profile === 'FUNCIONARIO' ? '/casos' : '/infracciones'

  useEffect(() => {
    let vigente = true
    function cargar() {
      NotificacionesApi.findAll(base).then((page) => {
        if (vigente) {
          setNotificaciones(page.items)
        }
      })
    }
    cargar()
    const intervalo = window.setInterval(cargar, RECARGA_MS)
    return () => {
      vigente = false
      window.clearInterval(intervalo)
    }
  }, [base, version])

  const sinLeer = notificaciones.filter((n) => !n.leida).length

  // se recarga al cerrar, asi mientras esta abierto se ven cuales eran nuevas
  function cambiar(abrir: boolean) {
    setAbierto(abrir)
    if (abrir && sinLeer > 0) {
      NotificacionesApi.marcarLeidas(base)
    } else if (!abrir) {
      setVersion((v) => v + 1)
    }
  }

  function ir() {
    cambiar(false)
    navigate(base)
  }

  return (
    <Popover open={abierto} onOpenChange={cambiar}>
      <PopoverTrigger asChild>
        <Button variant="ghost" size="icon" className="relative" aria-label="Notificaciones">
          <Bell />
          {sinLeer > 0 && <Badge className="absolute -top-1 -right-1 h-4 min-w-4 px-1 text-[10px]">{sinLeer}</Badge>}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 gap-0 p-1">
        <PopoverHeader className="px-2 py-1.5">
          <PopoverTitle>Notificaciones</PopoverTitle>
        </PopoverHeader>
        {notificaciones.length === 0 ? (
          <p className="px-2 py-6 text-center text-muted-foreground">No hay notificaciones.</p>
        ) : (
          <ul className="max-h-96 overflow-y-auto">
            {notificaciones.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  className="flex w-full cursor-pointer gap-2 rounded-md px-2 py-2 text-left hover:bg-accent hover:text-accent-foreground"
                  onClick={ir}
                >
                  <span className={cn('mt-1.5 size-2 shrink-0 rounded-full', !n.leida && 'bg-primary')} />
                  <span className="flex flex-col gap-0.5">
                    <span>{n.mensaje}</span>
                    <span className="text-xs text-muted-foreground">{momento(n.fecha)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </PopoverContent>
    </Popover>
  )
}

export default Notificaciones

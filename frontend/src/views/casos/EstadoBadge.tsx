import type { ComponentProps } from 'react'
import { Badge } from '@/components/ui/badge'
import type { EstadoCaso } from '@/api/casos/casos.types'
import { ESTADOS_CASO } from './casos.constants'

const variantes: Record<EstadoCaso, ComponentProps<typeof Badge>['variant']> = {
  DETECTADA: 'outline',
  NOTIFICADA: 'outline',
  EN_DESCARGO: 'secondary',
  CONFIRMADA: 'destructive',
  DESESTIMADA: 'secondary',
}

function EstadoBadge({ estado }: { estado: EstadoCaso }) {
  return <Badge variant={variantes[estado]}>{ESTADOS_CASO[estado]}</Badge>
}

export default EstadoBadge

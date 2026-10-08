import { Building2, CalendarClock, FileText, Home, Route, ShieldCheck, Truck } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Profile } from '@/auth/profile'

type NavItem = {
  title: string
  description?: string
  path: string
  icon: LucideIcon
  profiles?: Profile[]
}

type NavGroup = {
  title?: string
  items: NavItem[]
}

const groups: NavGroup[] = [
  {
    items: [
      {
        title: 'Inicio',
        path: '/',
        icon: Home,
      },
      {
        title: 'Validar permisos',
        description: 'Verificar si una empresa transportista tiene sus permisos vigentes.',
        path: '/validar-permisos',
        icon: ShieldCheck,
      },
    ],
  },
  {
    title: 'Operación',
    items: [
      {
        title: 'Guías',
        description: 'Registrar guías de carga y consultar las existentes.',
        path: '/guias',
        icon: FileText,
        profiles: ['RESPONSABLE', 'FUNCIONARIO'],
      },
      {
        title: 'Viajes',
        description: 'Asignar guías a un vehículo y un chofer, y seguir su estado.',
        path: '/viajes',
        icon: Route,
        profiles: ['RESPONSABLE', 'FUNCIONARIO'],
      },
    ],
  },
  {
    title: 'Flota',
    items: [
      {
        title: 'Vehículos',
        description: 'Vehículos de carga con sus datos técnicos.',
        path: '/vehiculos',
        icon: Truck,
        profiles: ['RESPONSABLE', 'FUNCIONARIO'],
      },
      {
        title: 'Permisos',
        description: 'Permisos nacionales de circulación y su vigencia.',
        path: '/permisos',
        icon: CalendarClock,
        profiles: ['RESPONSABLE', 'FUNCIONARIO'],
      },
    ],
  },
  {
    title: 'Fiscalización',
    items: [
      {
        title: 'Empresas',
        description: 'Alta y consulta de empresas transportistas.',
        path: '/empresas',
        icon: Building2,
        profiles: ['FUNCIONARIO'],
      },
    ],
  },
]

export function navigationFor(profile: Profile | null) {
  return groups
    .map((group) => ({
      ...group,
      items: group.items.filter((i) => !i.profiles || (profile !== null && i.profiles.includes(profile))),
    }))
    .filter((group) => group.items.length > 0)
}

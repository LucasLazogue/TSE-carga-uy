import { Building2, CalendarClock, FileText, Home, Route, Truck } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import type { Profile } from '@/auth/profile'

type NavItem = {
  title: string
  description?: string
  path: string
  icon: LucideIcon
  profiles: Profile[]
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
        profiles: ['CIUDADANO', 'CHOFER', 'FUNCIONARIO'],
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
        profiles: ['CIUDADANO', 'FUNCIONARIO'],
      },
      {
        title: 'Viajes',
        description: 'Asignar guías a un vehículo y un chofer, y seguir su estado.',
        path: '/viajes',
        icon: Route,
        profiles: ['CIUDADANO', 'FUNCIONARIO'],
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
        profiles: ['CIUDADANO', 'FUNCIONARIO'],
      },
      {
        title: 'Permisos',
        description: 'Permisos nacionales de circulación y su vigencia.',
        path: '/permisos',
        icon: CalendarClock,
        profiles: ['CIUDADANO', 'FUNCIONARIO'],
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

export function navigationFor(profile: Profile) {
  return groups
    .map((group) => ({ ...group, items: group.items.filter((i) => i.profiles.includes(profile)) }))
    .filter((group) => group.items.length > 0)
}

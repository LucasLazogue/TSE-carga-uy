import type { Empresa } from '@/api/empresas/empresas.types'

export type Session = {
  id: number
  cedula: string
  correo: string
  roles: string[]
  empresas: Empresa[]
}

export type AuthConfig = {
  mock: boolean
}

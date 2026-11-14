import type { Empresa } from '@/api/empresas/empresas.types'

export type Session = {
  id: number
  cedula: string
  correo: string
  // datos de la PDI: faltan mientras el perfil esta pendiente, y en los funcionarios
  nombres?: string
  apellidos?: string
  fechaNacimiento?: string
  roles: string[]
  empresas: Empresa[]
}

export type AuthConfig = {
  mock: boolean
}

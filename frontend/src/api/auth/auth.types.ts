export type Session = {
  id: number
  cedula: string
  correo: string
  roles: string[]
}

export type AuthConfig = {
  mock: boolean
}

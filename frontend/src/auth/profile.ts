export type Profile = 'FUNCIONARIO' | 'CHOFER' | 'CIUDADANO'

export function getProfile(roles: string[]): Profile {
  if (roles.includes('FUNCIONARIO')) {
    return 'FUNCIONARIO'
  }
  if (roles.includes('CHOFER')) {
    return 'CHOFER'
  }
  return 'CIUDADANO'
}

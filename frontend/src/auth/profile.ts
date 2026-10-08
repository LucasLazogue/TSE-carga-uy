export type Profile = 'FUNCIONARIO' | 'RESPONSABLE' | 'CHOFER' | 'CIUDADANO'

export function getProfile(roles: string[]): Profile {
  if (roles.includes('FUNCIONARIO')) {
    return 'FUNCIONARIO'
  }
  if (roles.includes('RESPONSABLE')) {
    return 'RESPONSABLE'
  }
  if (roles.includes('CHOFER')) {
    return 'CHOFER'
  }
  return 'CIUDADANO'
}

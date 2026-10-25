export type Empresa = {
  id: number
  nroEmpresa: number
  nombrePublico: string
  razonSocial: string
  direccionPrincipal: string
  fechaAlta: string
}

export type EmpresaNueva = Omit<Empresa, 'id' | 'fechaAlta'> & {
  cedulaResponsable: string
}

export type FiltroEmpresas = {
  nombre?: string
}

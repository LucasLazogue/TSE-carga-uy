export type Empresa = {
  id: number
  nroEmpresa: number
  nombrePublico: string
  razonSocial: string
  direccionPrincipal: string
  fechaAlta: string
}

export type DatosEmpresa = Omit<Empresa, 'id' | 'fechaAlta'>

export type EmpresaNueva = DatosEmpresa & {
  cedulaResponsable: string
}

export type FiltroEmpresas = {
  nombre?: string
}

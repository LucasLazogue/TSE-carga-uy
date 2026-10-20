import { EmpresasApi } from '@/api/empresas/empresas.api'
import type { LookupOption } from './Lookup'

export function fuente<T extends { id: number }>(
  buscar: (texto: string) => Promise<T[]>,
  find: (id: number) => Promise<T>,
  label: (item: T) => string,
) {
  const opcion = (item: T): LookupOption => ({ value: String(item.id), label: label(item) })
  return {
    search: async (texto: string) => (await buscar(texto)).map(opcion),
    resolve: (value: string) => find(Number(value)).then(opcion, () => null),
  }
}

export const fuenteEmpresas = fuente(
  async (nombre) => (await EmpresasApi.findAll({ nombre })).items,
  EmpresasApi.find,
  (e) => e.nombrePublico,
)

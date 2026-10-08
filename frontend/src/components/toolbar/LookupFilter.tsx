import Lookup from '@/components/lookup/Lookup'
import type { LookupOption } from '@/components/lookup/Lookup'
import FilterChip from './FilterChip'

type Props = {
  label: string
  value?: string
  onChange: (value: string | undefined) => void
  search: (texto: string) => Promise<LookupOption[]>
  resolve: (value: string) => Promise<LookupOption | null>
  required?: boolean
}

function LookupFilter({ label, value, onChange, search, resolve, required }: Props) {
  return (
    <Lookup
      value={value}
      onChange={onChange}
      search={search}
      resolve={resolve}
      required={required}
      placeholder="Quitar filtro"
      searchPlaceholder={`Buscar ${label.toLowerCase()}...`}
      trigger={(seleccion) => <FilterChip label={label} valueLabel={seleccion?.label} />}
    />
  )
}

export default LookupFilter

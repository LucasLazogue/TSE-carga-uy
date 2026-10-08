import FilterChip from './FilterChip'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export type FilterOption = {
  value: string
  label: string
}

type Props = {
  label: string
  value?: string
  options: FilterOption[]
  onChange: (value: string | undefined) => void
}

function FilterSelect({ label, value, options, onChange }: Props) {
  const elegida = options.find((o) => o.value === value)

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <FilterChip label={label} valueLabel={elegida?.label} />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        <DropdownMenuRadioGroup value={value ?? ''} onValueChange={(v) => onChange(v || undefined)}>
          {options.map((o) => (
            <DropdownMenuRadioItem key={o.value} value={o.value}>
              {o.label}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
        {elegida && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => onChange(undefined)}>Quitar filtro</DropdownMenuItem>
          </>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export default FilterSelect

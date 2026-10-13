import { Input } from '@/components/ui/input'

type Props = {
  label: string
  value?: string
  onChange: (value: string | undefined) => void
}

function DateFilter({ label, value, onChange }: Props) {
  return (
    <label className="flex items-center gap-2 text-sm text-muted-foreground">
      {label}
      <Input type="date" className="w-36" value={value ?? ''} onChange={(e) => onChange(e.target.value || undefined)} />
    </label>
  )
}

export default DateFilter

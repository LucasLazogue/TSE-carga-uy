import type { ComponentProps } from 'react'
import { PlusCircle } from 'lucide-react'
import { cn } from 'cn'
import { Button } from '@/components/ui/button'

type Props = ComponentProps<typeof Button> & {
  label: string
  valueLabel?: string
}

function FilterChip({ label, valueLabel, className, ...props }: Props) {
  return (
    <Button variant="outline" className={cn(!valueLabel && 'border-dashed', className)} {...props}>
      {valueLabel ? (
        <>
          <span className="text-muted-foreground">{label}:</span>
          <span className="max-w-40 truncate">{valueLabel}</span>
        </>
      ) : (
        <>
          <PlusCircle />
          {label}
        </>
      )}
    </Button>
  )
}

export default FilterChip

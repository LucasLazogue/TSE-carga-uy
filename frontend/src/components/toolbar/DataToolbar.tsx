import type { ReactNode } from 'react'

type Props = {
  children: ReactNode
  actions?: ReactNode
}

function DataToolbar({ children, actions }: Props) {
  return (
    <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
      <div className="flex flex-1 flex-wrap items-center gap-2">{children}</div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  )
}

export default DataToolbar

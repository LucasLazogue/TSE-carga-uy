import type { Key, MouseEvent, ReactNode } from 'react'
import type { Page } from '@/api/page'
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from '@/components/ui/pagination'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

export type GridColumn<T> = {
  header: string
  cell: (item: T) => ReactNode
  className?: string
}

type Props<T> = {
  columns: GridColumn<T>[]
  items: T[]
  rowKey: (item: T) => Key
  empty?: string
  page?: Omit<Page<T>, 'items'>
  onPageChange?: (pagina: number) => void
}

const deshabilitado = 'pointer-events-none opacity-50'

function Grid<T>({ columns, items, rowKey, empty = 'No hay resultados.', page, onPageChange }: Props<T>) {
  const totalPaginas = page ? Math.max(1, Math.ceil(page.total / page.tamanio)) : 1

  function irA(pagina: number) {
    return (e: MouseEvent) => {
      e.preventDefault()
      onPageChange?.(pagina)
    }
  }

  return (
    <div className="space-y-3">
      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((c) => (
                <TableHead key={c.header} className={c.className}>
                  {c.header}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} className="h-24 text-center text-muted-foreground">
                  {empty}
                </TableCell>
              </TableRow>
            ) : (
              items.map((item) => (
                <TableRow key={rowKey(item)}>
                  {columns.map((c) => (
                    <TableCell key={c.header} className={c.className}>
                      {c.cell(item)}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
      {page && (
        <div className="flex items-center justify-between gap-4 text-sm text-muted-foreground">
          <span>
            Página {page.pagina + 1} de {totalPaginas} ({page.total} resultados)
          </span>
          <Pagination className="mx-0 w-auto">
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious
                  href="#"
                  text="Anterior"
                  aria-disabled={page.pagina === 0}
                  className={page.pagina === 0 ? deshabilitado : undefined}
                  onClick={irA(page.pagina - 1)}
                />
              </PaginationItem>
              <PaginationItem>
                <PaginationNext
                  href="#"
                  text="Siguiente"
                  aria-disabled={page.pagina + 1 >= totalPaginas}
                  className={page.pagina + 1 >= totalPaginas ? deshabilitado : undefined}
                  onClick={irA(page.pagina + 1)}
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </div>
      )}
    </div>
  )
}

export default Grid

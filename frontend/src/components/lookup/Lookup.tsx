import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import { Check, ChevronsUpDown, LoaderCircle, Search } from 'lucide-react'
import { cn } from 'cn'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'

export type LookupOption = {
  value: string
  label: string
}

type Props = {
  value?: string
  onChange: (value: string | undefined) => void
  search: (texto: string) => Promise<LookupOption[]>
  resolve: (value: string) => Promise<LookupOption | null>
  placeholder: string
  searchPlaceholder?: string
  trigger?: (seleccion: LookupOption | null) => ReactNode
  required?: boolean
}

const MAXIMO_OPCIONES = 10
const ESPERA_BUSQUEDA_MS = 250

function normalizar(texto: string) {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim()
}

function Lookup({
  value,
  onChange,
  search,
  resolve,
  placeholder,
  searchPlaceholder = 'Buscar...',
  trigger,
  required = false,
}: Props) {
  const [abierto, setAbierto] = useState(false)
  const [texto, setTexto] = useState('')
  const [iniciales, setIniciales] = useState<LookupOption[]>([])
  const [opciones, setOpciones] = useState<LookupOption[]>([])
  const [cargando, setCargando] = useState(false)
  const [activo, setActivo] = useState(0)
  const [conocida, setConocida] = useState<LookupOption | null>(null)
  const espera = useRef<number>(undefined)
  const ultimaBusqueda = useRef(0)
  const idLista = useId()

  const seleccion = value && conocida?.value === value ? conocida : null

  useEffect(() => {
    let vigente = true
    search('').then((resultado) => {
      if (vigente) {
        setIniciales(resultado.slice(0, MAXIMO_OPCIONES))
        setOpciones(resultado.slice(0, MAXIMO_OPCIONES))
      }
    })
    return () => {
      vigente = false
      window.clearTimeout(espera.current)
    }
  }, [search])

  useEffect(() => {
    if (!value || conocida?.value === value) {
      return
    }
    let vigente = true
    resolve(value).then((opcion) => {
      if (vigente) {
        setConocida(opcion)
      }
    })
    return () => {
      vigente = false
    }
  }, [value, conocida, resolve])

  const filtro = normalizar(texto)
  const visibles = cargando ? opciones.filter((o) => normalizar(o.label).includes(filtro)) : opciones
  const items: (LookupOption | null)[] = seleccion && !required ? [null, ...visibles] : visibles

  async function buscar(valor: string) {
    const busqueda = ++ultimaBusqueda.current
    try {
      const resultado = await search(valor.trim())
      if (busqueda === ultimaBusqueda.current) {
        setOpciones(resultado.slice(0, MAXIMO_OPCIONES))
      }
    } finally {
      if (busqueda === ultimaBusqueda.current) {
        setCargando(false)
      }
    }
  }

  function abrir(abrir: boolean) {
    setAbierto(abrir)
    if (abrir) {
      window.clearTimeout(espera.current)
      ultimaBusqueda.current++
      setTexto('')
      setOpciones(iniciales)
      setCargando(false)
      setActivo(0)
    }
  }

  function escribir(valor: string) {
    setTexto(valor)
    setActivo(0)
    setCargando(true)
    window.clearTimeout(espera.current)
    espera.current = window.setTimeout(() => buscar(valor), ESPERA_BUSQUEDA_MS)
  }

  function elegir(opcion: LookupOption | null) {
    setConocida(opcion)
    onChange(opcion?.value)
    setAbierto(false)
  }

  function teclear(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActivo((i) => Math.min(i + 1, items.length - 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActivo((i) => Math.max(i - 1, 0))
    } else if (e.key === 'Enter' && items[activo] !== undefined) {
      e.preventDefault()
      elegir(items[activo])
    }
  }

  return (
    <Popover open={abierto} onOpenChange={abrir}>
      <PopoverTrigger asChild>
        {trigger ? (
          trigger(seleccion)
        ) : (
          <Button variant="outline" role="combobox" aria-expanded={abierto} className="w-56 justify-between font-normal">
            <span className={cn('truncate', !seleccion && 'text-muted-foreground')}>{seleccion?.label ?? placeholder}</span>
            <ChevronsUpDown className="opacity-50" />
          </Button>
        )}
      </PopoverTrigger>
      <PopoverContent align="start" className="w-64 gap-0 p-1">
        <div className="relative mb-1">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            autoFocus
            role="combobox"
            aria-controls={idLista}
            aria-activedescendant={items.length ? `${idLista}-${activo}` : undefined}
            className="px-8"
            placeholder={searchPlaceholder}
            value={texto}
            onChange={(e) => escribir(e.target.value)}
            onKeyDown={teclear}
          />
          {cargando && (
            <LoaderCircle className="absolute top-1/2 right-2.5 size-4 -translate-y-1/2 animate-spin text-muted-foreground" />
          )}
        </div>
        <ul id={idLista} role="listbox" className="max-h-64 overflow-y-auto">
          {items.length === 0 && (
            <li className="px-2 py-6 text-center text-sm text-muted-foreground">
              {cargando ? 'Buscando...' : 'Sin resultados.'}
            </li>
          )}
          {items.map((o, i) => (
            <li
              key={o?.value ?? ''}
              id={`${idLista}-${i}`}
              role="option"
              aria-selected={i === activo}
              className={cn(
                'flex cursor-pointer items-center rounded-md px-2 py-1.5 text-sm',
                i === activo && 'bg-accent text-accent-foreground',
                !o && 'text-muted-foreground',
              )}
              onMouseEnter={() => setActivo(i)}
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => elegir(o)}
            >
              <span className="truncate">{o?.label ?? placeholder}</span>
              {o && seleccion?.value === o.value && <Check className="ml-auto size-4" />}
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  )
}

export default Lookup

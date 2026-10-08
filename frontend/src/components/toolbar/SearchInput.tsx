import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Search } from 'lucide-react'
import { Input } from '@/components/ui/input'

type Props = {
  value?: string
  onChange: (value: string | undefined) => void
  placeholder?: string
  delay?: number
}

function SearchInput({ value, onChange, placeholder = 'Buscar...', delay = 300 }: Props) {
  const [texto, setTexto] = useState(value ?? '')
  const [ultimoValor, setUltimoValor] = useState(value)
  const espera = useRef<number>(undefined)

  if (value !== ultimoValor) {
    setUltimoValor(value)
    setTexto(value ?? '')
  }

  useEffect(() => () => window.clearTimeout(espera.current), [])

  function emitir(valor: string) {
    window.clearTimeout(espera.current)
    onChange(valor.trim() || undefined)
  }

  function escribir(valor: string) {
    setTexto(valor)
    window.clearTimeout(espera.current)
    espera.current = window.setTimeout(() => emitir(valor), delay)
  }

  function enviar(e: FormEvent) {
    e.preventDefault()
    emitir(texto)
  }

  return (
    <form onSubmit={enviar} role="search" className="relative w-full max-w-sm">
      <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        aria-label={placeholder}
        className="pl-8"
        placeholder={placeholder}
        value={texto}
        onChange={(e) => escribir(e.target.value)}
      />
    </form>
  )
}

export default SearchInput

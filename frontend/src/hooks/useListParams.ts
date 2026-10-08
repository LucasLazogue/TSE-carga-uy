import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'

const PAGINA = 'pagina'

export function useListParams<K extends string>(keys: readonly K[]) {
  const [params, setParams] = useSearchParams()

  const values: Partial<Record<K, string>> = {}
  for (const key of keys) {
    const value = params.get(key)
    if (value) {
      values[key] = value
    }
  }
  const pagina = Math.max(0, Number(params.get(PAGINA)) || 0)
  const activos = keys.filter((key) => values[key]).length

  const set = useCallback(
    (name: K, value: string | undefined) => {
      setParams(
        (prev) => {
          const next = new URLSearchParams(prev)
          if (value?.trim()) {
            next.set(name, value)
          } else {
            next.delete(name)
          }
          next.delete(PAGINA)
          return next
        },
        { replace: true },
      )
    },
    [setParams],
  )

  const setPagina = useCallback(
    (nueva: number) => {
      setParams((prev) => {
        const next = new URLSearchParams(prev)
        if (nueva > 0) {
          next.set(PAGINA, String(nueva))
        } else {
          next.delete(PAGINA)
        }
        return next
      })
    },
    [setParams],
  )

  const limpiar = useCallback(() => {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        for (const key of keys) {
          next.delete(key)
        }
        next.delete(PAGINA)
        return next
      },
      { replace: true },
    )
  }, [keys, setParams])

  return { values, pagina, activos, set, setPagina, limpiar }
}

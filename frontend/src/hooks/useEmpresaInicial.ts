import { useEffect } from 'react'
import { EmpresasApi } from '@/api/empresas/empresas.api'

export function useEmpresaInicial(idEmpresa: string | undefined, set: (name: 'idEmpresa', value: string) => void) {
  useEffect(() => {
    if (idEmpresa) {
      return
    }
    let vigente = true
    EmpresasApi.findAll().then(({ items: empresas }) => {
      if (vigente && empresas.length) {
        set('idEmpresa', String(empresas[0].id))
      }
    })
    return () => {
      vigente = false
    }
  }, [idEmpresa, set])
}

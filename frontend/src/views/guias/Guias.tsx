import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { empresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { guiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import type { Page } from '@/api/page'
import NuevaGuiaDialog from './dialogs/NuevaGuiaDialog'

function Guias() {
  const [pagina, setPagina] = useState<Page<Guia> | null>(null)
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [numeroPagina, setNumeroPagina] = useState(0)
  const { open } = useDialogs()

  const totalPaginas = pagina ? Math.max(1, Math.ceil(pagina.total / pagina.tamanio)) : 1

  useEffect(() => {
    empresasApi.getAll().then((lista) => {
      setEmpresas(lista)
      setIdEmpresa(lista[0]?.id ?? 0)
    })
  }, [])

  useEffect(() => {
    if (idEmpresa) {
      guiasApi.getAll(idEmpresa, { pagina: numeroPagina }).then(setPagina)
    }
  }, [idEmpresa, numeroPagina])

  function elegirEmpresa(id: number) {
    setIdEmpresa(id)
    setNumeroPagina(0)
  }

  async function recargar() {
    setPagina(await guiasApi.getAll(idEmpresa, { pagina: numeroPagina }))
  }

  function nuevaGuia() {
    open({
      title: 'Nueva guía',
      size: 'lg',
      content: (close) => <NuevaGuiaDialog empresas={empresas} close={close} onCreated={recargar} />,
    })
  }

  return (
    <div>
      <h1>Guias</h1>
      <select value={idEmpresa} onChange={(e) => elegirEmpresa(Number(e.target.value))}>
        {empresas.map((e) => (
          <option key={e.id} value={e.id}>
            {e.nombrePublico}
          </option>
        ))}
      </select>
      <button onClick={nuevaGuia}>Nueva guía</button>

      <table border={1}>
        <thead>
          <tr>
            <th>Nro. guia</th>
            <th>Fecha</th>
            <th>Empresa</th>
            <th>Rubro</th>
            <th>Tipo de carga</th>
            <th>Volumen</th>
            <th>Origen</th>
            <th>Destino</th>
            <th>Viaje</th>
          </tr>
        </thead>
        <tbody>
          {pagina?.items.map((g) => (
            <tr key={g.id}>
              <td>{g.nroGuia}</td>
              <td>{g.fecha}</td>
              <td>{g.nombreEmpresa}</td>
              <td>{g.nombreRubro}</td>
              <td>{g.nombreTipoCarga}</td>
              <td>{g.volumen}</td>
              <td>
                {g.origenLat}, {g.origenLon}
              </td>
              <td>
                {g.destinoLat}, {g.destinoLon}
              </td>
              <td>{g.idViaje ?? 'Sin asignar'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div>
        <button disabled={numeroPagina === 0} onClick={() => setNumeroPagina(numeroPagina - 1)}>
          Anterior
        </button>
        <span>
          {' '}
          Página {numeroPagina + 1} de {totalPaginas} ({pagina?.total ?? 0} guías){' '}
        </span>
        <button disabled={numeroPagina + 1 >= totalPaginas} onClick={() => setNumeroPagina(numeroPagina + 1)}>
          Siguiente
        </button>
      </div>
      <p>
        <Link to="/">Volver al inicio</Link>
      </p>
    </div>
  )
}

export default Guias

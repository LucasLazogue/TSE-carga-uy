import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { useDialogs } from '@/components/dialogs/useDialogs'
import { empresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { guiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import NuevaGuiaDialog from './dialogs/NuevaGuiaDialog'

function Guias() {
  const [lista, setLista] = useState<Guia[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const { open } = useDialogs()

  useEffect(() => {
    empresasApi.getAll().then(setEmpresas)
    guiasApi.getAll().then(setLista)
  }, [])

  async function cargar(filtro: number) {
    setLista(await guiasApi.getAll(filtro || undefined))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(idEmpresa)
  }

  function nuevaGuia() {
    open({
      title: 'Nueva guía',
      size: 'lg',
      content: (close) => <NuevaGuiaDialog empresas={empresas} close={close} onCreated={() => cargar(idEmpresa)} />,
    })
  }

  return (
    <div>
      <h1>Guias</h1>
      <form onSubmit={buscar}>
        <select value={idEmpresa} onChange={(e) => setIdEmpresa(Number(e.target.value))}>
          <option value={0}>Todas las empresas</option>
          {empresas.map((e) => (
            <option key={e.id} value={e.id}>
              {e.nombrePublico}
            </option>
          ))}
        </select>
        <button type="submit">Buscar</button>
      </form>
      <button
        onClick={() => {
          setIdEmpresa(0)
          cargar(0)
        }}
      >
        Ver todas
      </button>
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
          {lista.map((g) => (
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
      <p>
        <Link to="/">Volver al inicio</Link>
      </p>
    </div>
  )
}

export default Guias

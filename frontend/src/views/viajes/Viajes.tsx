import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { getErrorMessage } from '@/api/errors'
import { empresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { guiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import { TAMANIO_MAXIMO } from '@/api/page'
import type { Page } from '@/api/page'
import { vehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { viajesApi } from '@/api/viajes/viajes.api'
import type { EstadoViaje, FiltroViajes, Viaje, ViajeNuevo } from '@/api/viajes/viajes.types'
import { ESTADOS_VIAJE, VIAJE_VACIO } from './viajes.constants'

function Viajes() {
  const [pagina, setPagina] = useState<Page<Viaje> | null>(null)
  const [filtro, setFiltro] = useState<FiltroViajes>({ pagina: 0 })
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [guias, setGuias] = useState<Guia[]>([])
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<ViajeNuevo>(VIAJE_VACIO)
  const [error, setError] = useState('')

  const totalPaginas = pagina ? Math.max(1, Math.ceil(pagina.total / pagina.tamanio)) : 1

  useEffect(() => {
    empresasApi.getAll(undefined, { tamanio: TAMANIO_MAXIMO }).then(({ items: lista }) => {
      setEmpresas(lista)
      setIdEmpresa(lista[0]?.id ?? 0)
    })
  }, [])

  useEffect(() => {
    if (idEmpresa) {
      cargarGuiasSinViaje(idEmpresa)
      vehiculosApi.getAll(idEmpresa, { tamanio: TAMANIO_MAXIMO }).then((page) => setVehiculos(page.items))
    }
  }, [idEmpresa])

  useEffect(() => {
    if (idEmpresa) {
      viajesApi.getAll(idEmpresa, filtro).then(setPagina)
    }
  }, [idEmpresa, filtro])

  async function cargarGuiasSinViaje(id: number) {
    setGuias((await guiasApi.getAll(id, { conViaje: false, tamanio: 100 })).items)
  }

  function elegirEmpresa(id: number) {
    setIdEmpresa(id)
    setFiltro({ ...filtro, pagina: 0 })
    setNuevo(VIAJE_VACIO)
  }

  function filtrar(cambio: FiltroViajes) {
    setFiltro({ ...filtro, ...cambio, pagina: 0 })
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await viajesApi.create(idEmpresa, nuevo)
      setNuevo(VIAJE_VACIO)
      setCreando(false)
      await cargarGuiasSinViaje(idEmpresa)
      setFiltro({ ...filtro })
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <div>
      <h1>Viajes</h1>
      <div>
        <select value={idEmpresa} onChange={(e) => elegirEmpresa(Number(e.target.value))}>
          {empresas.map((e) => (
            <option key={e.id} value={e.id}>
              {e.nombrePublico}
            </option>
          ))}
        </select>
        <select
          value={filtro.estado ?? ''}
          onChange={(e) => filtrar({ estado: (e.target.value || undefined) as EstadoViaje | undefined })}
        >
          <option value="">Todos los estados</option>
          {ESTADOS_VIAJE.map((estado) => (
            <option key={estado} value={estado}>
              {estado}
            </option>
          ))}
        </select>
      </div>
      <button onClick={() => setCreando(true)}>Asignar viaje</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Asignar viaje</h2>
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <div>
            <label>Guia </label>
            <select value={nuevo.idGuia} onChange={(e) => setNuevo({ ...nuevo, idGuia: Number(e.target.value) })}>
              <option value={0}>Seleccione una guia</option>
              {guias.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.nroGuia} - {g.nombreEmpresa} - {g.fecha}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label>Vehiculo </label>
            <select
              value={nuevo.idVehiculo}
              onChange={(e) => setNuevo({ ...nuevo, idVehiculo: Number(e.target.value) })}
            >
              <option value={0}>Seleccione un vehiculo</option>
              {vehiculos.map((v) => (
                <option key={v.id} value={v.id}>
                  {v.matricula}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label>Id chofer </label>
            <input
              type="number"
              value={nuevo.idChofer || ''}
              onChange={(e) => setNuevo({ ...nuevo, idChofer: Number(e.target.value) })}
            />
          </div>
          <button type="submit">Guardar</button>
          <button type="button" onClick={() => setCreando(false)}>
            Cancelar
          </button>
        </form>
      )}

      <table border={1}>
        <thead>
          <tr>
            <th>Id</th>
            <th>Guia</th>
            <th>Vehiculo</th>
            <th>Chofer</th>
            <th>Estado</th>
            <th>Inicio</th>
            <th>Fin</th>
          </tr>
        </thead>
        <tbody>
          {pagina?.items.map((v) => (
            <tr key={v.id}>
              <td>{v.id}</td>
              <td>{v.nroGuia}</td>
              <td>{v.matricula}</td>
              <td>{v.cedulaChofer}</td>
              <td>{v.estado}</td>
              <td>{v.fechaInicio}</td>
              <td>{v.fechaFin}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div>
        <button
          disabled={!filtro.pagina}
          onClick={() => setFiltro({ ...filtro, pagina: (filtro.pagina ?? 0) - 1 })}
        >
          Anterior
        </button>
        <span>
          {' '}
          Página {(filtro.pagina ?? 0) + 1} de {totalPaginas} ({pagina?.total ?? 0} viajes){' '}
        </span>
        <button
          disabled={(filtro.pagina ?? 0) + 1 >= totalPaginas}
          onClick={() => setFiltro({ ...filtro, pagina: (filtro.pagina ?? 0) + 1 })}
        >
          Siguiente
        </button>
      </div>
    </div>
  )
}

export default Viajes

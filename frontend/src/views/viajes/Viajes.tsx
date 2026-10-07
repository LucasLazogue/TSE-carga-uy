import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { getErrorMessage } from '@/api/errors'
import { empresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { guiasApi } from '@/api/guias/guias.api'
import type { Guia } from '@/api/guias/guias.types'
import { vehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { VIAJE_VACIO } from './viajes.constants'
import { viajesApi } from '@/api/viajes/viajes.api'
import type { Viaje, ViajeNuevo } from '@/api/viajes/viajes.types'

function Viajes() {
  const [lista, setLista] = useState<Viaje[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [guias, setGuias] = useState<Guia[]>([])
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<ViajeNuevo>(VIAJE_VACIO)
  const [error, setError] = useState('')

  const guiasSinViaje = guias.filter((g) => g.idViaje === null)
  const guiaElegida = guias.find((g) => g.id === nuevo.idGuia)
  const vehiculosDeLaEmpresa = vehiculos.filter((v) => v.idEmpresa === guiaElegida?.idEmpresa)

  useEffect(() => {
    empresasApi.getAll().then(setEmpresas)
    guiasApi.getAll().then(setGuias)
    vehiculosApi.getAll().then(setVehiculos)
    viajesApi.getAll().then(setLista)
  }, [])

  async function cargar(filtro: number) {
    setLista(await viajesApi.getAll(filtro || undefined))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(idEmpresa)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await viajesApi.create(nuevo)
      setNuevo(VIAJE_VACIO)
      setCreando(false)
      setGuias(await guiasApi.getAll())
      await cargar(idEmpresa)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <div>
      <h1>Viajes</h1>
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
        Ver todos
      </button>
      <button onClick={() => setCreando(true)}>Asignar viaje</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Asignar viaje</h2>
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <div>
            <label>Guia </label>
            <select
              value={nuevo.idGuia}
              onChange={(e) => setNuevo({ ...nuevo, idGuia: Number(e.target.value), idVehiculo: 0 })}
            >
              <option value={0}>Seleccione una guia</option>
              {guiasSinViaje.map((g) => (
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
              {vehiculosDeLaEmpresa.map((v) => (
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
          {lista.map((v) => (
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
      <p>
        <Link to="/">Volver al inicio</Link>
      </p>
    </div>
  )
}

export default Viajes

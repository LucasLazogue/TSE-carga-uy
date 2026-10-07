import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { getErrorMessage } from '@/api/errors'
import { vehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { PERMISO_VACIO } from './permisos.constants'
import { permisosApi } from '@/api/permisos/permisos.api'
import type { Permiso, PermisoNuevo } from '@/api/permisos/permisos.types'

function Permisos() {
  const [lista, setLista] = useState<Permiso[]>([])
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [idVehiculo, setIdVehiculo] = useState(0)
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<PermisoNuevo>(PERMISO_VACIO)
  const [error, setError] = useState('')

  useEffect(() => {
    vehiculosApi.getAll().then(setVehiculos)
    permisosApi.getAll().then(setLista)
  }, [])

  async function cargar(filtro: number) {
    setLista(await permisosApi.getAll(filtro || undefined))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(idVehiculo)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await permisosApi.create(nuevo)
      setNuevo(PERMISO_VACIO)
      setCreando(false)
      await cargar(idVehiculo)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <div>
      <h1>Permisos de circulacion</h1>
      <form onSubmit={buscar}>
        <select value={idVehiculo} onChange={(e) => setIdVehiculo(Number(e.target.value))}>
          <option value={0}>Todos los vehiculos</option>
          {vehiculos.map((v) => (
            <option key={v.id} value={v.id}>
              {v.matricula}
            </option>
          ))}
        </select>
        <button type="submit">Buscar</button>
      </form>
      <button
        onClick={() => {
          setIdVehiculo(0)
          cargar(0)
        }}
      >
        Ver todos
      </button>
      <button onClick={() => setCreando(true)}>Crear</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Nuevo permiso</h2>
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <div>
            <label>Nro. permiso </label>
            <input
              value={nuevo.nroPermiso}
              onChange={(e) => setNuevo({ ...nuevo, nroPermiso: e.target.value })}
            />
          </div>
          <div>
            <label>Valido desde </label>
            <input
              type="date"
              value={nuevo.validoDesde}
              onChange={(e) => setNuevo({ ...nuevo, validoDesde: e.target.value })}
            />
          </div>
          <div>
            <label>Valido hasta </label>
            <input
              type="date"
              value={nuevo.validoHasta}
              onChange={(e) => setNuevo({ ...nuevo, validoHasta: e.target.value })}
            />
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
            <th>Nro. permiso</th>
            <th>Valido desde</th>
            <th>Valido hasta</th>
            <th>Vehiculo</th>
          </tr>
        </thead>
        <tbody>
          {lista.map((p) => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.nroPermiso}</td>
              <td>{p.validoDesde}</td>
              <td>{p.validoHasta}</td>
              <td>{p.matricula}</td>
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

export default Permisos

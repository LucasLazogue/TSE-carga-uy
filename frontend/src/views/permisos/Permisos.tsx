import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { getErrorMessage } from '@/api/errors'
import { empresasApi } from '@/api/empresas/empresas.api'
import { TAMANIO_MAXIMO } from '@/api/page'
import type { Empresa } from '@/api/empresas/empresas.types'
import { vehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo } from '@/api/vehiculos/vehiculos.types'
import { PERMISO_VACIO } from './permisos.constants'
import { permisosApi } from '@/api/permisos/permisos.api'
import type { Permiso, PermisoNuevo } from '@/api/permisos/permisos.types'

function Permisos() {
  const [lista, setLista] = useState<Permiso[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [vehiculos, setVehiculos] = useState<Vehiculo[]>([])
  const [idVehiculo, setIdVehiculo] = useState(0)
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<PermisoNuevo>(PERMISO_VACIO)
  const [error, setError] = useState('')

  useEffect(() => {
    empresasApi.getAll(undefined, { tamanio: TAMANIO_MAXIMO }).then(({ items: lista }) => {
      setEmpresas(lista)
      if (lista.length) {
        elegirEmpresa(lista[0].id)
      }
    })
  }, [])

  async function cargar(id: number) {
    setLista(id ? (await permisosApi.getAll(id, { tamanio: TAMANIO_MAXIMO })).items : [])
  }

  async function elegirEmpresa(id: number) {
    setIdEmpresa(id)
    const { items: delaEmpresa } = await vehiculosApi.getAll(id, { tamanio: TAMANIO_MAXIMO })
    setVehiculos(delaEmpresa)
    elegirVehiculo(delaEmpresa[0]?.id ?? 0)
  }

  function elegirVehiculo(id: number) {
    setIdVehiculo(id)
    cargar(id)
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
      <select value={idEmpresa} onChange={(e) => elegirEmpresa(Number(e.target.value))}>
        {empresas.map((e) => (
          <option key={e.id} value={e.id}>
            {e.nombrePublico}
          </option>
        ))}
      </select>
      <select value={idVehiculo} onChange={(e) => elegirVehiculo(Number(e.target.value))}>
        {vehiculos.map((v) => (
          <option key={v.id} value={v.id}>
            {v.matricula}
          </option>
        ))}
      </select>
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

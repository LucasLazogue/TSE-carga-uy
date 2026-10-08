import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { getErrorMessage } from '@/api/errors'
import { empresasApi } from '@/api/empresas/empresas.api'
import { TAMANIO_MAXIMO } from '@/api/page'
import type { Empresa } from '@/api/empresas/empresas.types'
import { VEHICULO_VACIO } from './vehiculos.constants'
import { vehiculosApi } from '@/api/vehiculos/vehiculos.api'
import type { Vehiculo, VehiculoNuevo } from '@/api/vehiculos/vehiculos.types'

function Vehiculos() {
  const [lista, setLista] = useState<Vehiculo[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<VehiculoNuevo>(VEHICULO_VACIO)
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
    setLista((await vehiculosApi.getAll(id, { tamanio: TAMANIO_MAXIMO })).items)
  }

  function elegirEmpresa(id: number) {
    setIdEmpresa(id)
    cargar(id)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await vehiculosApi.create(nuevo)
      setNuevo(VEHICULO_VACIO)
      setCreando(false)
      await cargar(idEmpresa)
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  return (
    <div>
      <h1>Vehiculos</h1>
      <select value={idEmpresa} onChange={(e) => elegirEmpresa(Number(e.target.value))}>
        {empresas.map((e) => (
          <option key={e.id} value={e.id}>
            {e.nombrePublico}
          </option>
        ))}
      </select>
      <button onClick={() => setCreando(true)}>Crear</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Nuevo vehiculo</h2>
          {error && <p style={{ color: 'red' }}>{error}</p>}
          <div>
            <label>Matricula </label>
            <input
              value={nuevo.matricula}
              onChange={(e) => setNuevo({ ...nuevo, matricula: e.target.value })}
            />
          </div>
          <div>
            <label>Marca </label>
            <input value={nuevo.marca} onChange={(e) => setNuevo({ ...nuevo, marca: e.target.value })} />
          </div>
          <div>
            <label>Modelo </label>
            <input value={nuevo.modelo} onChange={(e) => setNuevo({ ...nuevo, modelo: e.target.value })} />
          </div>
          <div>
            <label>Peso (kg) </label>
            <input
              type="number"
              value={nuevo.pesoVehiculo}
              onChange={(e) => setNuevo({ ...nuevo, pesoVehiculo: Number(e.target.value) })}
            />
          </div>
          <div>
            <label>Capacidad de carga (kg) </label>
            <input
              type="number"
              value={nuevo.capacidadCarga}
              onChange={(e) => setNuevo({ ...nuevo, capacidadCarga: Number(e.target.value) })}
            />
          </div>
          <div>
            <label>Empresa </label>
            <select
              value={nuevo.idEmpresa}
              onChange={(e) => setNuevo({ ...nuevo, idEmpresa: Number(e.target.value) })}
            >
              <option value={0}>Seleccione una empresa</option>
              {empresas.map((e) => (
                <option key={e.id} value={e.id}>
                  {e.nombrePublico}
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
            <th>Matricula</th>
            <th>Marca</th>
            <th>Modelo</th>
            <th>Peso (kg)</th>
            <th>Capacidad de carga (kg)</th>
            <th>Empresa</th>
          </tr>
        </thead>
        <tbody>
          {lista.map((v) => (
            <tr key={v.id}>
              <td>{v.id}</td>
              <td>{v.matricula}</td>
              <td>{v.marca}</td>
              <td>{v.modelo}</td>
              <td>{v.pesoVehiculo}</td>
              <td>{v.capacidadCarga}</td>
              <td>{v.nombreEmpresa}</td>
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

export default Vehiculos

import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import empresasService from '../empresas/empresas.service.ts'
import type { Empresa } from '../empresas/empresas.types.ts'
import { VEHICULO_VACIO } from './vehiculos.constants.ts'
import vehiculos from './vehiculos.service.ts'
import type { Vehiculo, VehiculoNuevo } from './vehiculos.types.ts'

function Vehiculos() {
  const [lista, setLista] = useState<Vehiculo[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<VehiculoNuevo>(VEHICULO_VACIO)

  useEffect(() => {
    empresasService.getAll().then(setEmpresas)
    vehiculos.getAll().then(setLista)
  }, [])

  async function cargar(filtro: number) {
    setLista(await vehiculos.getAll(filtro || undefined))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(idEmpresa)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    await vehiculos.create(nuevo)
    setNuevo(VEHICULO_VACIO)
    setCreando(false)
    await cargar(idEmpresa)
  }

  return (
    <div>
      <h1>Vehiculos</h1>
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
      <button onClick={() => setCreando(true)}>Crear</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Nuevo vehiculo</h2>
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

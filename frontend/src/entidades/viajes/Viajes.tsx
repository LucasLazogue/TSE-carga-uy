import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { mensajeDeError } from '../../common/errores.ts'
import empresasService from '../empresas/empresas.service.ts'
import type { Empresa } from '../empresas/empresas.types.ts'
import guiasService from '../guias/guias.service.ts'
import type { Guia } from '../guias/guias.types.ts'
import vehiculosService from '../vehiculos/vehiculos.service.ts'
import type { Vehiculo } from '../vehiculos/vehiculos.types.ts'
import { VIAJE_VACIO } from './viajes.constants.ts'
import viajes from './viajes.service.ts'
import type { Viaje, ViajeNuevo } from './viajes.types.ts'

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
    empresasService.getAll().then(setEmpresas)
    guiasService.getAll().then(setGuias)
    vehiculosService.getAll().then(setVehiculos)
    viajes.getAll().then(setLista)
  }, [])

  async function cargar(filtro: number) {
    setLista(await viajes.getAll(filtro || undefined))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(idEmpresa)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await viajes.create(nuevo)
      setNuevo(VIAJE_VACIO)
      setCreando(false)
      setGuias(await guiasService.getAll())
      await cargar(idEmpresa)
    } catch (err) {
      setError(mensajeDeError(err))
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

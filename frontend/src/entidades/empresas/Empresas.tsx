import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { EMPRESA_VACIA } from './empresas.constants.ts'
import empresas from './empresas.service.ts'
import type { Empresa, EmpresaNueva } from './empresas.types.ts'

function Empresas() {
  const [lista, setLista] = useState<Empresa[]>([])
  const [nombre, setNombre] = useState('')
  const [creando, setCreando] = useState(false)
  const [nueva, setNueva] = useState<EmpresaNueva>(EMPRESA_VACIA)

  async function cargar(filtro?: string) {
    setLista(await empresas.getAll(filtro))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(nombre)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    await empresas.create(nueva)
    setNueva(EMPRESA_VACIA)
    setCreando(false)
    await cargar(nombre)
  }

  return (
    <div>
      <h1>Empresas</h1>
      <form onSubmit={buscar}>
        <input
          placeholder="Buscar por nombre o razon social"
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
        />
        <button type="submit">Buscar</button>
      </form>
      <button
        onClick={() => {
          setNombre('')
          cargar()
        }}
      >
        Ver todas
      </button>
      <button onClick={() => setCreando(true)}>Crear</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Nueva empresa</h2>
          <div>
            <label>Nro. Empresa </label>
            <input
              type="number"
              value={nueva.nroEmpresa}
              onChange={(e) => setNueva({ ...nueva, nroEmpresa: Number(e.target.value) })}
            />
          </div>
          <div>
            <label>Nombre publico </label>
            <input
              value={nueva.nombrePublico}
              onChange={(e) => setNueva({ ...nueva, nombrePublico: e.target.value })}
            />
          </div>
          <div>
            <label>Razon social </label>
            <input
              value={nueva.razonSocial}
              onChange={(e) => setNueva({ ...nueva, razonSocial: e.target.value })}
            />
          </div>
          <div>
            <label>Direccion principal </label>
            <input
              value={nueva.direccionPrincipal}
              onChange={(e) => setNueva({ ...nueva, direccionPrincipal: e.target.value })}
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
            <th>Nro. Empresa</th>
            <th>Nombre publico</th>
            <th>Razon social</th>
            <th>Direccion principal</th>
            <th>Fecha de alta</th>
          </tr>
        </thead>
        <tbody>
          {lista.map((e) => (
            <tr key={e.id}>
              <td>{e.id}</td>
              <td>{e.nroEmpresa}</td>
              <td>{e.nombrePublico}</td>
              <td>{e.razonSocial}</td>
              <td>{e.direccionPrincipal}</td>
              <td>{e.fechaAlta}</td>
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

export default Empresas

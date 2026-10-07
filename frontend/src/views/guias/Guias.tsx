import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { FormEvent } from 'react'
import { authApi } from '@/api/auth/auth.api'
import { getErrorMessage } from '@/api/errors'
import { empresasApi } from '@/api/empresas/empresas.api'
import type { Empresa } from '@/api/empresas/empresas.types'
import { GUIA_VACIA } from './guias.constants'
import { guiasApi } from '@/api/guias/guias.api'
import type { Guia, GuiaNueva, Rubro, TipoCarga } from '@/api/guias/guias.types'

function Guias() {
  const [lista, setLista] = useState<Guia[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [rubros, setRubros] = useState<Rubro[]>([])
  const [tiposCarga, setTiposCarga] = useState<TipoCarga[]>([])
  const [idUsuario, setIdUsuario] = useState(0)
  const [idEmpresa, setIdEmpresa] = useState(0)
  const [creando, setCreando] = useState(false)
  const [nuevo, setNuevo] = useState<GuiaNueva>(GUIA_VACIA)
  const [error, setError] = useState('')

  useEffect(() => {
    empresasApi.getAll().then(setEmpresas)
    guiasApi.getRubros().then(setRubros)
    guiasApi.getTiposCarga().then(setTiposCarga)
    authApi.me().then((s) => setIdUsuario(s.id)).catch(() => setIdUsuario(0))
    guiasApi.getAll().then(setLista)
  }, [])

  async function cargar(filtro: number) {
    setLista(await guiasApi.getAll(filtro || undefined))
  }

  async function buscar(e: FormEvent) {
    e.preventDefault()
    await cargar(idEmpresa)
  }

  async function guardar(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await guiasApi.create({ ...nuevo, idRegistradaPor: idUsuario })
      setNuevo(GUIA_VACIA)
      setCreando(false)
      await cargar(idEmpresa)
    } catch (err) {
      setError(getErrorMessage(err))
    }
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
      <button onClick={() => setCreando(true)}>Crear</button>

      {creando && (
        <form onSubmit={guardar}>
          <h2>Nueva guia</h2>
          {error && <p style={{ color: 'red' }}>{error}</p>}
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
          <div>
            <label>Rubro del cliente </label>
            <select
              value={nuevo.idRubro}
              onChange={(e) => setNuevo({ ...nuevo, idRubro: Number(e.target.value) })}
            >
              <option value={0}>Seleccione un rubro</option>
              {rubros.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.nombre}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label>Tipo de carga </label>
            <select
              value={nuevo.idTipoCarga}
              onChange={(e) => setNuevo({ ...nuevo, idTipoCarga: Number(e.target.value) })}
            >
              <option value={0}>Seleccione un tipo de carga</option>
              {tiposCarga.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.nombre}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label>Volumen (kg) </label>
            <input
              type="number"
              value={nuevo.volumen || ''}
              onChange={(e) => setNuevo({ ...nuevo, volumen: Number(e.target.value) })}
            />
          </div>
          <div>
            <label>Fecha </label>
            <input
              type="date"
              value={nuevo.fecha}
              onChange={(e) => setNuevo({ ...nuevo, fecha: e.target.value })}
            />
          </div>
          <div>
            <label>Origen (lat, lon) </label>
            <input
              type="number"
              step="any"
              value={nuevo.origenLat || ''}
              onChange={(e) => setNuevo({ ...nuevo, origenLat: Number(e.target.value) })}
            />
            <input
              type="number"
              step="any"
              value={nuevo.origenLon || ''}
              onChange={(e) => setNuevo({ ...nuevo, origenLon: Number(e.target.value) })}
            />
          </div>
          <div>
            <label>Destino (lat, lon) </label>
            <input
              type="number"
              step="any"
              value={nuevo.destinoLat || ''}
              onChange={(e) => setNuevo({ ...nuevo, destinoLat: Number(e.target.value) })}
            />
            <input
              type="number"
              step="any"
              value={nuevo.destinoLon || ''}
              onChange={(e) => setNuevo({ ...nuevo, destinoLon: Number(e.target.value) })}
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

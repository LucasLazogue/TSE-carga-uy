import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import auth from './auth/auth.service.ts'
import type { Sesion } from './auth/auth.service.ts'

function Inicio() {
  const [sesion, setSesion] = useState<Sesion | null>(null)
  const [params] = useSearchParams()
  const error = params.get('error')

  useEffect(() => {
    auth.me().then(setSesion).catch(() => setSesion(null))
  }, [])

  const salir = () => {
    auth.salir().then(() => setSesion(null))
  }

  return (
    <div>
      <h2>Carga UY</h2>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {sesion ? (
        <p>
          {sesion.cedula} ({sesion.roles.join(', ')}) <button onClick={salir}>Salir</button>
        </p>
      ) : (
        <p>
          <a href={auth.urlLogin}>Ingresar con gub.uy</a>
        </p>
      )}
      <p>
        <Link to="/empresas">Empresas</Link>
      </p>
      <p>
        <Link to="/vehiculos">Vehiculos</Link>
      </p>
      <p>
        <Link to="/permisos">Permisos</Link>
      </p>
    </div>
  )
}

export default Inicio

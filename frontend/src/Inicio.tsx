import { Link } from 'react-router-dom'

function Inicio() {
  return (
    <div>
      <h2>Carga UY</h2>
      <p>
        <Link to="/empresas">Empresas</Link>
      </p>
      <p>
        <Link to="/vehiculos">Vehiculos</Link>
      </p>
    </div>
  )
}

export default Inicio

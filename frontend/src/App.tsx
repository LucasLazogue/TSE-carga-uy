import { Route, Routes } from 'react-router-dom'
import Inicio from './Inicio.tsx'
import Empresas from './entidades/empresas/Empresas.tsx'
import Vehiculos from './entidades/vehiculos/Vehiculos.tsx'
import Permisos from './entidades/permisos/Permisos.tsx'
import Guias from './entidades/guias/Guias.tsx'
import Viajes from './entidades/viajes/Viajes.tsx'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/empresas" element={<Empresas />} />
      <Route path="/vehiculos" element={<Vehiculos />} />
      <Route path="/permisos" element={<Permisos />} />
      <Route path="/guias" element={<Guias />} />
      <Route path="/viajes" element={<Viajes />} />
    </Routes>
  )
}

export default App

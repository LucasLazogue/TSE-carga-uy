import { Route, Routes } from 'react-router-dom'
import Inicio from './Inicio.tsx'
import Empresas from './entidades/empresas/Empresas.tsx'
import Vehiculos from './entidades/vehiculos/Vehiculos.tsx'
import Permisos from './entidades/permisos/Permisos.tsx'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/empresas" element={<Empresas />} />
      <Route path="/vehiculos" element={<Vehiculos />} />
      <Route path="/permisos" element={<Permisos />} />
    </Routes>
  )
}

export default App

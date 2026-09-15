import { Route, Routes } from 'react-router-dom'
import Inicio from './Inicio.tsx'
import Empresas from './entidades/empresas/Empresas.tsx'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="/empresas" element={<Empresas />} />
    </Routes>
  )
}

export default App

import { Navigate, Route, Routes } from 'react-router-dom'
import AppShell from '@/components/layout/AppShell'
import { useSession } from '@/auth/useSession'
import Empresas from '@/views/empresas/Empresas'
import Guias from '@/views/guias/Guias'
import Home from '@/views/home/Home'
import PublicHome from '@/views/home/PublicHome'
import Login from '@/views/login/Login'
import Permisos from '@/views/permisos/Permisos'
import ValidarPermisos from '@/views/validar-permisos/ValidarPermisos'
import Vehiculos from '@/views/vehiculos/Vehiculos'
import Viajes from '@/views/viajes/Viajes'

function App() {
  const { session, loading } = useSession()

  if (loading) {
    return null
  }

  return (
    <Routes>
      <Route path="/ingresar" element={session ? <Navigate to="/" replace /> : <Login />} />
      <Route element={<AppShell />}>
        <Route path="/" element={session ? <Home /> : <PublicHome />} />
        <Route path="/validar-permisos" element={<ValidarPermisos />} />
        {session && (
          <>
            <Route path="/empresas" element={<Empresas />} />
            <Route path="/vehiculos" element={<Vehiculos />} />
            <Route path="/permisos" element={<Permisos />} />
            <Route path="/guias" element={<Guias />} />
            <Route path="/viajes" element={<Viajes />} />
          </>
        )}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}

export default App

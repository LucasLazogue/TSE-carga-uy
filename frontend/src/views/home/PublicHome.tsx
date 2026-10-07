import { Navigate, useSearchParams } from 'react-router-dom'
import PageHeader from '@/components/PageHeader'

function PublicHome() {
  const [params] = useSearchParams()
  const error = params.get('error')

  if (error) {
    return <Navigate to={`/ingresar?error=${encodeURIComponent(error)}`} replace />
  }

  return (
    <PageHeader
      title="Carga UY"
      description="Plataforma del MTOP para la gestión y fiscalización del transporte de carga."
    />
  )
}

export default PublicHome

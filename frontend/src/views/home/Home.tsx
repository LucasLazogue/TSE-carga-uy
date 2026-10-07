import { Link } from 'react-router-dom'
import { Smartphone } from 'lucide-react'
import PageHeader from '@/components/PageHeader'
import { navigationFor } from '@/components/layout/navigation'
import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { Profile } from '@/auth/profile'
import { useSession } from '@/auth/useSession'

const descriptions: Record<Exclude<Profile, 'CHOFER'>, string> = {
  CIUDADANO: 'Gestioná la flota, las guías y los viajes de tu empresa.',
  FUNCIONARIO: 'Fiscalizá empresas, vehículos y viajes.',
}

function Home() {
  const { profile } = useSession()

  if (!profile) {
    return null
  }

  if (profile === 'CHOFER') {
    return (
      <Card className="mx-auto mt-12 max-w-md text-center">
        <CardHeader>
          <Smartphone className="mx-auto size-8 text-muted-foreground" />
          <CardTitle>Usá la aplicación móvil</CardTitle>
          <CardDescription>Los viajes asignados a choferes se gestionan desde la app de Carga UY.</CardDescription>
        </CardHeader>
      </Card>
    )
  }

  const shortcuts = navigationFor(profile)
    .flatMap((group) => group.items)
    .filter((item) => item.path !== '/')

  return (
    <>
      <PageHeader title="Inicio" description={descriptions[profile]} />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {shortcuts.map((item) => (
          <Link key={item.path} to={item.path} className="rounded-xl transition-colors hover:bg-muted/50">
            <Card className="h-full bg-transparent">
              <CardHeader>
                <item.icon className="mb-2 size-5 text-muted-foreground" />
                <CardTitle>{item.title}</CardTitle>
                <CardDescription>{item.description}</CardDescription>
              </CardHeader>
            </Card>
          </Link>
        ))}
      </div>
    </>
  )
}

export default Home

import { ChevronDown, LogOut } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import type { Session } from '@/api/auth/auth.types'
import { useSession } from '@/auth/useSession'

// DNIC puede informar la fecha parcial: 1975-11-03, 1975-11 o 1975
const formatFechaNacimiento = (fecha: string) => fecha.split('-').reverse().join('/')

function UserMenu({ session }: { session: Session }) {
  const { logout } = useSession()
  const navigate = useNavigate()
  const nombre = [session.nombres, session.apellidos].filter(Boolean).join(' ')

  const handleLogout = async () => {
    await logout()
    navigate('/')
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" className="h-9 gap-2 px-2">
          <Avatar className="size-7">
            <AvatarFallback className="text-xs">{session.cedula.slice(-2)}</AvatarFallback>
          </Avatar>
          <span className="hidden text-sm sm:inline">{nombre || session.cedula}</span>
          <ChevronDown className="size-4 text-muted-foreground" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-60">
        <DropdownMenuLabel className="flex flex-col gap-1">
          {nombre && <span className="font-medium">{nombre}</span>}
          <span className={nombre ? 'text-xs font-normal text-muted-foreground' : 'font-medium'}>{session.cedula}</span>
          <span className="truncate text-xs font-normal text-muted-foreground">{session.correo}</span>
          {session.fechaNacimiento && (
            <span className="text-xs font-normal text-muted-foreground">
              Nacimiento: {formatFechaNacimiento(session.fechaNacimiento)}
            </span>
          )}
          <span className="flex flex-wrap gap-1 pt-1">
            {session.roles.map((role) => (
              <Badge key={role} variant="secondary">
                {role.toLowerCase()}
              </Badge>
            ))}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={handleLogout}>
          <LogOut />
          Cerrar sesión
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export default UserMenu

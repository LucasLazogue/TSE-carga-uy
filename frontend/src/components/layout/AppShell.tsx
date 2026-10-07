import { Outlet } from 'react-router-dom'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { useSession } from '@/auth/useSession'
import AppSidebar from './AppSidebar'
import Logo from './Logo'
import UserMenu from './UserMenu'

function AppShell() {
  const { session, profile } = useSession()

  if (!session || !profile) {
    return null
  }

  return (
    <SidebarProvider className="flex-col">
      <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center gap-2 border-b bg-background px-3">
        <SidebarTrigger />
        <Logo />
        <div className="ml-auto">
          <UserMenu session={session} />
        </div>
      </header>
      <div className="flex flex-1">
        <AppSidebar profile={profile} />
        <SidebarInset>
          <main className="mx-auto w-full max-w-6xl flex-1 p-4 md:p-6">
            <Outlet />
          </main>
        </SidebarInset>
      </div>
    </SidebarProvider>
  )
}

export default AppShell

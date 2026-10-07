import { NavLink, useLocation } from 'react-router-dom'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  useSidebar,
} from '@/components/ui/sidebar'
import type { Profile } from '@/auth/profile'
import { navigationFor } from './navigation'

function isActive(pathname: string, path: string) {
  return path === '/' ? pathname === '/' : pathname.startsWith(path)
}

function AppSidebar({ profile }: { profile: Profile | null }) {
  const { pathname } = useLocation()
  const { setOpenMobile } = useSidebar()

  return (
    <Sidebar collapsible="icon" className="top-14 h-[calc(100svh-3.5rem)]">
      <SidebarContent>
        {navigationFor(profile).map((group, i) => (
          <SidebarGroup key={group.title ?? i}>
            {group.title && <SidebarGroupLabel>{group.title}</SidebarGroupLabel>}
            <SidebarGroupContent>
              <SidebarMenu>
                {group.items.map((item) => (
                  <SidebarMenuItem key={item.path}>
                    <SidebarMenuButton asChild isActive={isActive(pathname, item.path)} tooltip={item.title}>
                      <NavLink to={item.path} onClick={() => setOpenMobile(false)}>
                        <item.icon />
                        <span>{item.title}</span>
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        ))}
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  )
}

export default AppSidebar

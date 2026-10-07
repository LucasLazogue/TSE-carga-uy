import { createContext } from 'react'
import type { Session } from '@/api/auth/auth.types'
import type { Profile } from './profile'

export type SessionState = {
  session: Session | null
  profile: Profile | null
  loading: boolean
  logout: () => Promise<void>
}

export const SessionContext = createContext<SessionState | null>(null)

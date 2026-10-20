import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthApi } from '@/api/auth/auth.api'
import type { Session } from '@/api/auth/auth.types'
import { getProfile } from './profile'
import { SessionContext } from './session.context'

function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    AuthApi
      .me()
      .then(setSession)
      .catch(() => setSession(null))
      .finally(() => setLoading(false))
  }, [])

  const logout = useCallback(async () => {
    try {
      await AuthApi.logout()
    } finally {
      setSession(null)
    }
  }, [])

  const value = useMemo(
    () => ({ session, profile: session ? getProfile(session.roles) : null, loading, logout }),
    [session, loading, logout],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export default SessionProvider

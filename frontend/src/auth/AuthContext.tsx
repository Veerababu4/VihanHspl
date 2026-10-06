import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'

import { apiSend, setAuthHooks } from '../api/client'
import { AuthContext, type Me, type SessionDto, type Status } from './session'

/** A second browser tab may have just rotated the cookie; wait briefly and try once more. */
const RETRY_DELAY_MS = 600

/**
 * Staff session (DESIGN.md §12.2). The access token is kept only in memory; on page load the
 * session is restored from the httpOnly refresh cookie. Wraps the staff portal only, so the
 * public site never calls the auth API.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<Status>('checking')
  const [user, setUser] = useState<Me | null>(null)
  const [loggedOut, setLoggedOut] = useState(false)
  const tokenRef = useRef<string | null>(null)
  const refreshing = useRef<Promise<string | null> | null>(null)

  const applySession = useCallback((session: SessionDto | null) => {
    tokenRef.current = session?.accessToken ?? null
    setUser(session?.user ?? null)
    setStatus(session ? 'authenticated' : 'anonymous')
  }, [])

  /**
   * One refresh at a time, shared by every request that hit a 401.
   * @param retry try a second time after a short wait (expired token mid-session, maybe a two-tab race)
   */
  const refresh = useCallback((retry = true): Promise<string | null> => {
    if (!refreshing.current) {
      refreshing.current = (async () => {
        // 204 = no refresh cookie at all (nobody logged in on this browser)
        const attempt = async () => {
          const session = await apiSend<SessionDto | undefined>('POST', '/auth/refresh')
          if (!session) throw new Error('no session')
          return session
        }
        try {
          let session: SessionDto
          try {
            session = await attempt()
          } catch (e) {
            if (!retry) throw e
            await new Promise((r) => setTimeout(r, RETRY_DELAY_MS))
            session = await attempt()
          }
          applySession(session)
          return session.accessToken
        } catch {
          applySession(null)
          return null
        } finally {
          refreshing.current = null
        }
      })()
    }
    return refreshing.current
  }, [applySession])

  useEffect(() => {
    setAuthHooks({ getAccessToken: () => tokenRef.current, refresh: () => refresh(true) })
    void refresh(false)   // restore the session after a page reload (no cookie = just show the login page)
    return () => setAuthHooks(null)
  }, [refresh])

  const login = useCallback(
    async (username: string, password: string) => {
      const session = await apiSend<SessionDto>('POST', '/auth/login', { username, password })
      setLoggedOut(false)
      applySession(session)
      return session.user
    },
    [applySession],
  )

  const logout = useCallback(async () => {
    try {
      await apiSend('POST', '/auth/logout')
    } finally {
      setLoggedOut(true)
      applySession(null)
    }
  }, [applySession])

  const changePassword = useCallback(
    async (currentPassword: string, newPassword: string) => {
      applySession(await apiSend<SessionDto>('POST', '/auth/change-password', { currentPassword, newPassword }))
    },
    [applySession],
  )

  const value = useMemo(
    () => ({ status, user, loggedOut, login, logout, changePassword }),
    [status, user, loggedOut, login, logout, changePassword],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

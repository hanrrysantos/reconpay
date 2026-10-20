'use client'

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { ApiError, authenticate, createDemo, getMe, type Me } from './api'

/** Only the demo token is kept across reloads (AD-014); a regular login stays in memory. */
const DEMO_TOKEN_KEY = 'reconpay.demoToken'

type Session = { token: string; user: Me }
type AuthContextValue = {
  token: string | null
  user: Me | null
  /** True until the stored demo session, if any, has been checked on mount. */
  restoring: boolean
  /** The stored demo token was refused with 401 while restoring. */
  expired: boolean
  signIn: (email: string, password: string) => Promise<void>
  startDemo: () => Promise<void>
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

// Storage can be missing or throw (private mode, blocked site data); the session then simply lives in memory.
function readDemoToken(): string | null {
  try { return sessionStorage.getItem(DEMO_TOKEN_KEY) } catch { return null }
}
function writeDemoToken(token: string | null) {
  try {
    if (token) sessionStorage.setItem(DEMO_TOKEN_KEY, token)
    else sessionStorage.removeItem(DEMO_TOKEN_KEY)
  } catch { /* ignore */ }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  // Starts true so route guards wait for the effect below instead of redirecting on the first render.
  const [restoring, setRestoring] = useState(true)
  const [expired, setExpired] = useState(false)
  const signOut = useCallback(() => {
    writeDemoToken(null)
    setSession(null)
    setExpired(false)
  }, [])

  useEffect(() => {
    const token = readDemoToken()
    if (!token) {
      setRestoring(false)
      return
    }
    let active = true
    getMe(token)
      .then(user => { if (active) setSession({ token, user }) })
      .catch((error: unknown) => {
        if (!active || !(error instanceof ApiError && error.status === 401)) return
        writeDemoToken(null)
        setExpired(true)
      })
      .finally(() => { if (active) setRestoring(false) })
    return () => { active = false }
  }, [])

  async function signIn(email: string, password: string) {
    try {
      const nextSession = await authenticate(email, password)
      setSession(nextSession)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) setSession(null)
      throw error
    }
  }

  async function startDemo() {
    const nextSession = await createDemo()
    writeDemoToken(nextSession.token)
    setSession(nextSession)
  }

  return (
    <AuthContext.Provider value={{
      token: session?.token ?? null,
      user: session?.user ?? null,
      restoring,
      expired,
      signIn,
      startDemo,
      signOut,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return context
}

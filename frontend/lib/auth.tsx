'use client'

import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'
import { ApiError, authenticate, type Me } from './api'

type Session = { token: string; user: Me }
type AuthContextValue = {
  token: string | null
  user: Me | null
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const signOut = useCallback(() => setSession(null), [])

  async function signIn(email: string, password: string) {
    try {
      const nextSession = await authenticate(email, password)
      setSession(nextSession)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) setSession(null)
      throw error
    }
  }

  return (
    <AuthContext.Provider value={{
      token: session?.token ?? null,
      user: session?.user ?? null,
      signIn,
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

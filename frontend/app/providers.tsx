'use client'

import { useState, type ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider, useAuth } from '@/lib/auth'
import { createQueryClient } from '@/lib/queries'

function QueryProvider({ children }: { children: ReactNode }) {
  const { signOut } = useAuth()
  // Any 401 ends the in-memory session; the authenticated routes then redirect to /login.
  const [queryClient] = useState(() => createQueryClient(signOut))
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

export function Providers({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <QueryProvider>{children}</QueryProvider>
    </AuthProvider>
  )
}

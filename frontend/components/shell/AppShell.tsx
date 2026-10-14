'use client'

import { useRouter } from 'next/navigation'
import { useEffect, type ReactNode } from 'react'
import { useAuth } from '@/lib/auth'
import { TopNav } from './TopNav'

/** Session guard and shared chrome of the authenticated routes. */
export function AppShell({ children }: { children: ReactNode }) {
  const router = useRouter()
  const { token } = useAuth()

  useEffect(() => { if (!token) router.replace('/login') }, [router, token])

  if (!token) return <p className="grid min-h-screen place-items-center text-muted" role="status">Abrindo login...</p>

  return (
    <div className="mx-auto flex min-h-screen max-w-[1440px] flex-col gap-6 px-4 pt-6 pb-12 lg:px-10">
      <TopNav />
      <main className="flex flex-col gap-6">{children}</main>
    </div>
  )
}

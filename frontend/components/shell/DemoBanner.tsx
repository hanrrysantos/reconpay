'use client'

import { useAuth } from '@/lib/auth'

const timeFormat = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

/** Tells a demo visitor the account is temporary and when it expires, in the browser's local time. */
export function DemoBanner() {
  const { user } = useAuth()
  if (!user?.demoExpiresAt) return null

  return (
    <p className="rounded-full bg-primary-soft px-4 py-2 text-center text-[13px] font-medium text-foreground">
      Conta de demonstração · expira às {timeFormat.format(new Date(user.demoExpiresAt))}
    </p>
  )
}

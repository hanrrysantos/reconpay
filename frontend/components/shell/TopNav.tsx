'use client'

import { LogOut } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { ReactNode } from 'react'
import { Logo } from '@/components/brand/Logo'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/utils'
import { AccountMenu } from './AccountMenu'

export const NAV_ITEMS = [
  { label: 'Visão geral', href: '/painel' },
  { label: 'Conciliações', href: '/conciliacoes' },
  { label: 'Divergências', href: '/divergencias' },
  { label: 'Transações', href: '/transacoes' },
  { label: 'Importações', href: '/importacoes' },
  { label: 'Fechamento', href: '/fechamento' },
] as const

const isActive = (pathname: string, href: string) => pathname === href || pathname.startsWith(`${href}/`)

/** Product navigation; the notification bell is out of scope (no data source). */
export function TopNav({ merchantSwitcher }: { merchantSwitcher?: ReactNode }) {
  const pathname = usePathname()
  const { signOut } = useAuth()

  return (
    <header className="flex min-h-10 items-center gap-6">
      <Link href="/painel" className="shrink-0">
        <Logo tone="light" />
      </Link>

      <nav aria-label="Principal" className="min-w-0 overflow-x-auto">
        <ul className="flex w-max gap-1 rounded-full bg-surface p-1">
          {NAV_ITEMS.map(item => {
            const active = isActive(pathname, item.href)
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  aria-current={active ? 'page' : undefined}
                  className={cn(
                    'block whitespace-nowrap rounded-full px-4 py-2 text-[14px] font-medium transition-colors',
                    active ? 'bg-foreground text-white' : 'text-muted hover:text-foreground',
                  )}
                >
                  {item.label}
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      <div className="ml-auto flex shrink-0 items-center gap-2">
        {merchantSwitcher}
        <AccountMenu />
        {/* Ending the session clears the token; AppShell then redirects to /login. */}
        <button type="button" aria-label="Sair" onClick={signOut} className="grid size-10 place-items-center rounded-full bg-surface">
          <LogOut aria-hidden="true" className="size-4" />
        </button>
      </div>
    </header>
  )
}

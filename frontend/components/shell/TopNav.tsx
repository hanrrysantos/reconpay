'use client'

import { Check } from 'lucide-react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { ReactNode } from 'react'
import { useAuth } from '@/lib/auth'
import { cn } from '@/lib/utils'

export const NAV_ITEMS = [
  { label: 'Visão geral', href: '/' },
  { label: 'Conciliações', href: '/conciliacoes' },
  { label: 'Divergências', href: '/divergencias' },
  { label: 'Transações', href: '/transacoes' },
  { label: 'Importações', href: '/importacoes' },
  { label: 'Fechamento', href: '/fechamento' },
] as const

const isActive = (pathname: string, href: string) =>
  href === '/' ? pathname === '/' : pathname === href || pathname.startsWith(`${href}/`)

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase()
}

/** Product navigation; the notification bell is out of scope (no data source). */
export function TopNav({ merchantSwitcher }: { merchantSwitcher?: ReactNode }) {
  const pathname = usePathname()
  const { user } = useAuth()

  return (
    <header className="flex min-h-10 items-center gap-6">
      <Link href="/" className="flex shrink-0 items-center gap-2.5 font-display text-[20px] font-extrabold tracking-[-0.04em]">
        <span className="grid size-[30px] place-items-center rounded-sm bg-primary text-foreground">
          <Check aria-hidden="true" className="size-4" strokeWidth={3} />
        </span>
        reconpay
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
        {user && (
          <span
            role="img"
            aria-label={user.name}
            className="grid size-10 place-items-center rounded-full bg-foreground font-display text-[14px] font-extrabold text-primary"
          >
            {initials(user.name)}
          </span>
        )}
      </div>
    </header>
  )
}

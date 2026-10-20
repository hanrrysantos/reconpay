'use client'

import { LogOut, Users } from 'lucide-react'
import Link from 'next/link'
import { useEffect, useRef, useState } from 'react'
import { useAuth } from '@/lib/auth'

export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase()
}

const item = 'flex items-center gap-2 rounded-[14px] p-3 text-left text-[14px] font-semibold hover:bg-subtle'

/** The avatar opens who is signed in, with which role, "Usuários" for an ADMIN and "Sair". */
export function AccountMenu() {
  const { user, signOut } = useAuth()
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  if (!user) return null
  const admin = user.role === 'ADMIN'

  return (
    <div ref={root} className="relative" onKeyDown={event => { if (event.key === 'Escape') setOpen(false) }}>
      <button
        type="button"
        aria-label={user.name}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen(current => !current)}
        className="grid size-10 place-items-center rounded-full bg-foreground font-display text-[14px] font-extrabold text-primary"
      >
        {initials(user.name)}
      </button>

      {open && (
        <div
          role="menu"
          aria-label="Conta"
          className="absolute top-12 right-0 z-30 flex w-[280px] flex-col gap-1 rounded-[20px] bg-white p-2 shadow-[0_0_0_1px_var(--color-border),0_20px_48px_rgba(11,13,10,0.14)]"
        >
          <div className="flex flex-col gap-1 p-3">
            <span className="truncate text-[14px] font-semibold">{user.name}</span>
            <span className="truncate text-[13px] text-muted">{user.email}</span>
            <span className="mt-1 w-fit rounded-full bg-primary-soft px-2.5 py-0.5 text-[12px] font-semibold">{admin ? 'Admin' : 'Operador'}</span>
          </div>
          <div className="flex flex-col border-t border-border pt-1">
            {admin && (
              <Link href="/usuarios" role="menuitem" onClick={() => setOpen(false)} className={item}>
                <Users aria-hidden="true" className="size-4" />
                Usuários
              </Link>
            )}
            <button type="button" role="menuitem" onClick={signOut} className={item}>
              <LogOut aria-hidden="true" className="size-4" />
              Sair
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

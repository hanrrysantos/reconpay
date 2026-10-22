'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useAuth } from '@/lib/auth'

/** The secondary CTA: signing in, or with a session open, leaving it to sign in with another account. */
export function AccountLink({ className }: { className: string }) {
  const router = useRouter()
  const { user, signOut } = useAuth()

  if (!user) return <Link href="/login" className={className}>Entrar na minha conta</Link>

  function switchAccount() {
    signOut()
    router.push('/login')
  }

  return <button type="button" onClick={switchAccount} className={className}>Sair e entrar com outra conta</button>
}

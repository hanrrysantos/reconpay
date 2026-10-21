'use client'

import { ArrowRight } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useRef, useState } from 'react'
import { ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'

const GENERIC_ERROR = 'Não foi possível abrir a demonstração. Tente novamente.'

/**
 * Opens a demo account and goes to the dashboard; styled by the caller like the landing's primary CTA.
 * With a session already open it only leads back, since another demo would spend the visitor's IP quota.
 */
export function DemoButton({ className }: { className: string }) {
  const router = useRouter()
  const { user, startDemo } = useAuth()
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // Guards repeated clicks before React re-renders the button as disabled: each call creates an account.
  const started = useRef(false)

  async function open() {
    if (started.current) return
    started.current = true
    setPending(true)
    setError(null)
    try {
      await startDemo()
      router.push('/painel')
    } catch (caught) {
      // Only the limit refusals carry a message meant for the visitor.
      const limited = caught instanceof ApiError && (caught.status === 429 || caught.status === 503)
      setError(limited ? caught.message : GENERIC_ERROR)
      started.current = false
      setPending(false)
    }
  }

  if (user) {
    return (
      <button type="button" onClick={() => router.push('/painel')} className={className}>
        Voltar ao painel
        <ArrowRight aria-hidden="true" className="size-[18px]" />
      </button>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <button type="button" onClick={open} disabled={pending} className={`${className} disabled:cursor-wait disabled:opacity-80`}>
        {pending ? 'Preparando demonstração...' : 'Ver demonstração'}
        {!pending && <ArrowRight aria-hidden="true" className="size-[18px]" />}
      </button>
      {error && <p role="alert" className="max-w-[320px] rounded-md bg-danger-soft px-3 py-2 text-[13px] font-medium text-danger-strong">{error}</p>}
    </div>
  )
}

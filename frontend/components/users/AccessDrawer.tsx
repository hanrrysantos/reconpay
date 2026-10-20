'use client'

import { useQuery } from '@tanstack/react-query'
import { useRef, useState, type FormEvent } from 'react'
import { SideDrawer } from '@/components/ui/SideDrawer'
import { getMerchants, type User } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { formatCnpj } from '@/lib/cnpj'
import { queryKeys, useReplaceUserAccess, useUserAccess } from '@/lib/queries'

const messageOf = (error: unknown, fallback: string) => error instanceof Error ? error.message : fallback

/**
 * The merchants an OPERATOR reaches: one checkbox per active merchant, by name, the current grants checked.
 * Saving sends exactly the checked ids, and an empty list leaves the user without merchants.
 */
export function AccessDrawer({ user, onClose, onDone }: { user: User; onClose: () => void; onDone: (message: string) => void }) {
  const { token, user: me } = useAuth()
  // Same key as the top menu: for an ADMIN, `/api/me/merchants` is every active merchant.
  const merchants = useQuery({ queryKey: [...queryKeys.merchants, me?.id ?? ''], queryFn: () => getMerchants(token!), enabled: Boolean(token) })
  const access = useUserAccess(user.id)
  const replace = useReplaceUserAccess()
  const sending = useRef(false)
  /** null until the user changes a box: the current grants are the starting point. */
  const [chosen, setChosen] = useState<Set<string> | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)
  const loaded = merchants.data !== undefined && access.data !== undefined
  const checked = chosen ?? new Set(access.data ?? [])

  function toggle(merchantId: string) {
    const next = new Set(checked)
    if (next.has(merchantId)) next.delete(merchantId)
    else next.add(merchantId)
    setChosen(next)
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current || !loaded) return
    sending.current = true
    setPending(true)
    setError(null)
    try {
      await replace.mutateAsync({ userId: user.id, merchantIds: merchants.data.filter(merchant => checked.has(merchant.id)).map(merchant => merchant.id) })
      onDone('Acessos atualizados.')
    } catch (failure) {
      setError(messageOf(failure, 'Não foi possível salvar.'))
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  const loadError = merchants.error ?? access.error

  return (
    <SideDrawer
      title={`Acessos de ${user.name}`}
      description="Marque as empresas que este operador acessa."
      formId="access-form"
      submitLabel="Salvar"
      pending={pending || !loaded}
      error={error ?? (loadError ? messageOf(loadError, 'Não foi possível carregar as empresas.') : null)}
      onSubmit={event => void submit(event)}
      onClose={onClose}
    >
      {!loaded && !loadError && <p role="status" className="text-[13px] text-muted">Carregando empresas…</p>}
      {loaded && merchants.data.length === 0 && <p className="text-[14px] text-muted">Nenhuma empresa cadastrada.</p>}
      {loaded && merchants.data.length > 0 && (
        <fieldset className="flex flex-col gap-1">
          <legend className="sr-only">Empresas</legend>
          {merchants.data.map(merchant => (
            <label key={merchant.id} className="flex cursor-pointer items-center gap-3 rounded-[14px] p-2 hover:bg-subtle">
              <input type="checkbox" checked={checked.has(merchant.id)} onChange={() => toggle(merchant.id)} className="size-4 shrink-0 accent-foreground" />
              <span className="flex min-w-0 flex-col">
                <span className="truncate text-[14px] font-semibold">{merchant.name}</span>
                <span className="font-mono text-[12px] text-muted">{formatCnpj(merchant.document)}</span>
              </span>
            </label>
          ))}
        </fieldset>
      )}
    </SideDrawer>
  )
}

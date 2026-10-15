'use client'

import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import type { Merchant } from '@/lib/api'
import { formatCnpj } from '@/lib/cnpj'
import { useDeactivateMerchant, useInvalidateMerchants } from '@/lib/queries'
import { isGone } from './MerchantDrawer'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível desativar.'

/** Confirmation before `DELETE /api/merchants/{id}`; the CNPJ of a deactivated merchant stays reserved. */
export function DeactivateDialog({ merchant, onClose }: { merchant: Merchant; onClose: () => void }) {
  const deactivate = useDeactivateMerchant()
  const reloadLists = useInvalidateMerchants()
  const cancel = useRef<HTMLButtonElement>(null)
  const sending = useRef(false)
  const titleId = useId()
  const warningId = useId()
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => { cancel.current?.focus() }, [])

  async function confirm() {
    if (sending.current) return
    sending.current = true
    setPending(true)
    setError(null)
    try {
      await deactivate.mutateAsync(merchant.id)
      onClose()
    } catch (failure) {
      setError(messageOf(failure))
      if (isGone(failure)) await reloadLists()
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') { event.preventDefault(); onClose() }
  }

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-[rgba(11,13,10,0.42)] p-4" onClick={onClose}>
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={warningId}
        onKeyDown={onKeyDown}
        onClick={event => event.stopPropagation()}
        className="flex w-[420px] max-w-full flex-col gap-3 rounded-[20px] bg-white p-5 shadow-[0_0_0_1px_var(--color-border),0_20px_48px_rgba(11,13,10,0.14)]"
      >
        <h2 id={titleId} className="font-display text-[22px] font-extrabold tracking-[-0.03em]">Desativar {merchant.name}?</h2>
        <p id={warningId} className="text-[13px] text-muted">
          A empresa sai da lista e do menu. O CNPJ <span className="font-mono">{formatCnpj(merchant.document)}</span> não poderá ser cadastrado de novo.
        </p>
        {error && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] text-danger-strong">{error}</p>}
        <div className="flex justify-end gap-2">
          <button ref={cancel} type="button" onClick={onClose} className="h-9 rounded-full bg-surface px-4 text-[13px] font-semibold">Cancelar</button>
          <button type="button" disabled={pending} onClick={() => void confirm()} className="h-9 rounded-full bg-foreground px-4 text-[13px] font-semibold text-white disabled:bg-surface disabled:text-muted">
            Desativar
          </button>
        </div>
      </div>
    </div>
  )
}

'use client'

import { useRef, useState } from 'react'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import type { Transaction, TransactionStatus } from '@/lib/api'
import { useInvalidateTransactions, useUpdateTransactionStatus } from '@/lib/queries'
import { FINAL_STATUSES, TRANSACTION_STATUS_LABELS } from '@/lib/transactions'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível alterar o status.'

/** Confirmation before `PATCH .../transactions/{id}/status`; the final statuses have no way back. */
export function StatusDialog({ merchantId, transaction, onClose }: { merchantId: string; transaction: Transaction; onClose: () => void }) {
  const update = useUpdateTransactionStatus(merchantId)
  const reloadList = useInvalidateTransactions(merchantId)
  const sending = useRef(false)
  const [status, setStatus] = useState<TransactionStatus | null>(null)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirm() {
    if (sending.current || status === null) return
    sending.current = true
    setPending(true)
    setError(null)
    try {
      await update.mutateAsync({ transactionId: transaction.id, status })
      onClose()
    } catch (failure) {
      setError(messageOf(failure))
      // 400 or 409: the status or the period changed elsewhere, so the list shows what the API has now.
      await reloadList()
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <ConfirmDialog
      title="Alterar status"
      warning={<>Transação <strong className="text-foreground">{transaction.externalReference}</strong>. A mudança não pode ser desfeita.</>}
      confirmLabel={pending ? 'Confirmando…' : 'Confirmar'}
      pending={pending}
      confirmDisabled={status === null}
      error={error}
      onConfirm={() => void confirm()}
      onClose={onClose}
    >
      <div role="radiogroup" aria-label="Novo status" className="flex flex-col gap-1">
        {FINAL_STATUSES.map(option => (
          <label key={option} className="flex h-10 items-center gap-3 rounded-md bg-surface px-3 text-[14px] font-semibold">
            <input type="radio" name="transaction-status" value={option} checked={status === option} onChange={() => setStatus(option)} />
            {TRANSACTION_STATUS_LABELS[option]}
          </label>
        ))}
      </div>
    </ConfirmDialog>
  )
}

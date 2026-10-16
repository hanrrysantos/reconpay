'use client'

import { useRef, useState } from 'react'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import type { Merchant } from '@/lib/api'
import { formatCnpj } from '@/lib/cnpj'
import { useDeactivateMerchant, useInvalidateMerchants } from '@/lib/queries'
import { isGone } from './MerchantDrawer'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível desativar.'

/** Confirmation before `DELETE /api/merchants/{id}`; the CNPJ of a deactivated merchant stays reserved. */
export function DeactivateDialog({ merchant, onClose }: { merchant: Merchant; onClose: () => void }) {
  const deactivate = useDeactivateMerchant()
  const reloadLists = useInvalidateMerchants()
  const sending = useRef(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

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

  return (
    <ConfirmDialog
      title={`Desativar ${merchant.name}?`}
      warning={<>A empresa sai da lista e do menu. O CNPJ <span className="font-mono">{formatCnpj(merchant.document)}</span> não poderá ser cadastrado de novo.</>}
      confirmLabel="Desativar"
      pending={pending}
      error={error}
      onConfirm={() => void confirm()}
      onClose={onClose}
    />
  )
}

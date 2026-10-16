'use client'

import { useRef, useState } from 'react'
import { METHOD_LABELS } from '@/components/dashboard/format'
import { isGone } from '@/components/merchants/MerchantDrawer'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import type { FeeRule } from '@/lib/api'
import { useDeactivateFeeRule, useInvalidateFeeRules } from '@/lib/queries'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível desativar.'

/** Confirmation before `DELETE /api/merchants/{merchantId}/fee-rules/{id}`; the same combination can be registered again. */
export function DeactivateRuleDialog({ merchantId, rule, onClose }: { merchantId: string; rule: FeeRule; onClose: () => void }) {
  const deactivate = useDeactivateFeeRule(merchantId)
  const reloadList = useInvalidateFeeRules(merchantId)
  const sending = useRef(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirm() {
    if (sending.current) return
    sending.current = true
    setPending(true)
    setError(null)
    try {
      await deactivate.mutateAsync(rule.id)
      onClose()
    } catch (failure) {
      setError(messageOf(failure))
      // 404: the rule was deactivated elsewhere (403: the merchant left the user's access), so the list is stale.
      if (isGone(failure)) await reloadList()
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <ConfirmDialog
      title={`Desativar a regra ${METHOD_LABELS[rule.paymentMethod]?.label ?? rule.paymentMethod} ${rule.installments}x?`}
      warning="Novas transações dessa combinação serão recusadas até existir outra regra."
      confirmLabel="Desativar"
      pending={pending}
      error={error}
      onConfirm={() => void confirm()}
      onClose={onClose}
    />
  )
}

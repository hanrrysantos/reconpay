'use client'

import { useRef, useState } from 'react'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import type { User } from '@/lib/api'
import { useDeactivateUser } from '@/lib/queries'

const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível desativar.'

/** Confirmation before `DELETE /api/users/{id}`; the account's open session stops working on its next request. */
export function DeactivateUserDialog({ user, onClose, onDone }: { user: User; onClose: () => void; onDone: (message: string) => void }) {
  const deactivate = useDeactivateUser()
  const sending = useRef(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function confirm() {
    if (sending.current) return
    sending.current = true
    setPending(true)
    setError(null)
    try {
      await deactivate.mutateAsync(user.id)
      onDone('Usuário desativado.')
    } catch (failure) {
      setError(messageOf(failure))
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <ConfirmDialog
      title={`Desativar ${user.name}?`}
      warning={`${user.name} perde o acesso na hora, inclusive a sessão aberta.`}
      confirmLabel="Desativar"
      pending={pending}
      error={error}
      onConfirm={() => void confirm()}
      onClose={onClose}
    />
  )
}

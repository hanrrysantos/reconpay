'use client'

import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'

type ConfirmDialogProps = {
  title: string
  warning: ReactNode
  confirmLabel: string
  pending: boolean
  error: string | null
  onConfirm: () => void
  onClose: () => void
}

/** Confirmation shell (`alertdialog`): "Cancelar" gets the focus, `Esc` and an outside click close, and the error shows inside. */
export function ConfirmDialog({ title, warning, confirmLabel, pending, error, onConfirm, onClose }: ConfirmDialogProps) {
  const cancel = useRef<HTMLButtonElement>(null)
  const titleId = useId()
  const warningId = useId()

  useEffect(() => { cancel.current?.focus() }, [])

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
        <h2 id={titleId} className="font-display text-[22px] font-extrabold tracking-[-0.03em]">{title}</h2>
        <p id={warningId} className="text-[13px] text-muted">{warning}</p>
        {error && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] text-danger-strong">{error}</p>}
        <div className="flex justify-end gap-2">
          <button ref={cancel} type="button" onClick={onClose} className="h-9 rounded-full bg-surface px-4 text-[13px] font-semibold">Cancelar</button>
          <button type="button" disabled={pending} onClick={onConfirm} className="h-9 rounded-full bg-foreground px-4 text-[13px] font-semibold text-white disabled:bg-surface disabled:text-muted">
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

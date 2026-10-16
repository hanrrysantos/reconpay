'use client'

import { Check, X } from 'lucide-react'
import { useRef, type FormEvent, type KeyboardEvent, type ReactNode } from 'react'

type SideDrawerProps = {
  title: string
  description: string
  formId: string
  submitLabel: string
  pending: boolean
  error: string | null
  onSubmit: (event: FormEvent) => void
  onClose: () => void
  children: ReactNode
}

/**
 * Side drawer shell: the overlay closes on an outside click, `Esc` closes, `Tab` stays inside the panel, and the
 * footer has "Cancelar" and the submit button, disabled while `pending`. The fields go in `children`.
 */
export function SideDrawer({ title, description, formId, submitLabel, pending, error, onSubmit, onClose, children }: SideDrawerProps) {
  const panel = useRef<HTMLDivElement>(null)

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') { event.preventDefault(); onClose(); return }
    if (event.key !== 'Tab') return
    const focusable = Array.from(panel.current?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([readonly]):not([disabled]), select:not([disabled])') ?? [])
    if (focusable.length === 0) return
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
  }

  return (
    <div className="fixed inset-0 z-40 bg-[rgba(11,13,10,0.42)]" onClick={onClose}>
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        onKeyDown={onKeyDown}
        onClick={event => event.stopPropagation()}
        className="absolute top-4 right-4 bottom-4 flex w-[480px] max-w-[calc(100%-32px)] flex-col rounded-[28px] bg-white shadow-[0_24px_64px_rgba(0,0,0,0.24)] outline-none"
      >
        <header className="flex items-start gap-3 p-6 pb-4">
          <div className="flex flex-col gap-1">
            <h2 className="font-display text-[22px] font-extrabold tracking-[-0.03em]">{title}</h2>
            <p className="text-[13px] text-muted">{description}</p>
          </div>
          <button type="button" aria-label="Fechar" onClick={onClose} className="ml-auto grid size-9 shrink-0 place-items-center rounded-full bg-surface">
            <X aria-hidden="true" className="size-4" />
          </button>
        </header>

        <form id={formId} onSubmit={onSubmit} noValidate className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 pb-6">
          {children}
          {error && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] text-danger-strong">{error}</p>}
        </form>

        <footer className="flex items-center justify-end gap-2 border-t border-border px-6 py-4">
          <button type="button" onClick={onClose} className="h-10 rounded-full bg-surface px-4 text-[14px] font-semibold">Cancelar</button>
          <button type="submit" form={formId} disabled={pending} className="flex h-10 items-center gap-2 rounded-full bg-primary px-4 text-[14px] font-semibold disabled:bg-surface disabled:text-muted">
            {submitLabel} <Check aria-hidden="true" className="size-4" />
          </button>
        </footer>
      </div>
    </div>
  )
}

'use client'

import { CalendarDays, ChevronDown, Lock } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import type { Run } from '@/lib/api'
import { formatWindow, monthsBack, type Window } from '@/lib/period'
import { cn } from '@/lib/utils'

export const windowKey = ({ fromDate, toDate }: Window) => `${fromDate}/${toDate}`

const brDate = (iso: string) => new Date(iso).toLocaleDateString('pt-BR')

export type PeriodMenuProps = {
  window: Window
  /** Months to list, newest first; when absent, the menu derives them from `runs`. */
  months?: Window[]
  /** Runs of the merchant: the oldest sets how far back the menu goes; a run with the exact window marks it "Aberto".
   *  Without runs, the months carry no run or lock marks and the settlement note is hidden. */
  runs?: Run[]
  /** Lock state of known windows, keyed by `windowKey`. */
  locks?: Record<string, boolean>
  /** Rejects with the API error when the window is invalid; the menu then shows its message under the fields. */
  onApply: (window: Window) => void | Promise<void>
  settlementImportedAt?: string | null
  today?: Date
  /** Edge of the trigger the panel lines up with; `start` when the trigger sits on the left of the page. */
  align?: 'start' | 'end'
}

export function PeriodMenu({ window, months: givenMonths, runs, locks = {}, onApply, settlementImportedAt = null, today = new Date(), align = 'end' }: PeriodMenuProps) {
  const [open, setOpen] = useState(false)
  const [from, setFrom] = useState(window.fromDate)
  const [to, setTo] = useState(window.toDate)
  const [error, setError] = useState<string | null>(null)
  const root = useRef<HTMLDivElement>(null)
  const fromInput = useRef<HTMLInputElement>(null)

  const oldest = (runs ?? []).reduce<string | null>((min, run) => (min === null || run.fromDate < min ? run.fromDate : min), null)
  const months = givenMonths ?? monthsBack(today, oldest)
  const withRun = new Set((runs ?? []).map(windowKey))

  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  function toggle() {
    setFrom(window.fromDate)
    setTo(window.toDate)
    setError(null)
    setOpen(current => !current)
  }

  async function apply(next: Window) {
    setError(null)
    try {
      await onApply(next)
      setOpen(false)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível aplicar o período.')
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    void apply({ fromDate: from, toDate: to })
  }

  return (
    <div ref={root} className="relative" onKeyDown={event => { if (event.key === 'Escape') setOpen(false) }}>
      <button
        type="button"
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={toggle}
        className="flex h-11 items-center gap-2 rounded-full bg-surface pr-2 pl-4 text-[14px] font-semibold"
      >
        <CalendarDays aria-hidden="true" className="size-4" />
        {formatWindow(window)}
        <span className="grid size-7 place-items-center rounded-full bg-white"><ChevronDown aria-hidden="true" className="size-4" /></span>
      </button>

      {open && (
        <div role="dialog" aria-label="Período" className={cn('absolute top-13 z-30 flex', align === 'start' ? 'left-0' : 'right-0', 'w-[520px] max-w-[calc(100vw-32px)] gap-2 rounded-[20px] bg-white p-2 shadow-[0_0_0_1px_var(--color-border),0_20px_48px_rgba(11,13,10,0.14)]')}>
          <div className="flex w-[200px] shrink-0 flex-col gap-0.5">
            <ul aria-label="Meses" className="flex max-h-72 flex-col gap-0.5 overflow-y-auto">
              {months.map(month => {
                const key = windowKey(month)
                const current = key === windowKey(window)
                return (
                  <li key={key}>
                    <button
                      type="button"
                      onClick={() => void apply(month)}
                      className={cn('flex w-full items-center justify-between gap-2 rounded-[12px] px-3 py-2 text-left text-[14px] hover:bg-subtle', current && 'bg-surface font-semibold hover:bg-surface')}
                    >
                      {formatWindow(month)}
                      {runs && (locks[key]
                        ? <Lock aria-label="Travado" className="size-3.5 text-success" />
                        : withRun.has(key)
                          ? <span className="rounded-full bg-primary px-2 py-0.5 text-[11px] font-bold">Aberto</span>
                          : <span className="text-[12px] text-muted">Sem run</span>)}
                    </button>
                  </li>
                )
              })}
            </ul>
            <button type="button" onClick={() => fromInput.current?.focus()} className="rounded-[12px] px-3 py-2 text-left text-[14px] text-muted hover:bg-subtle">
              Personalizado…
            </button>
          </div>

          <form onSubmit={submit} className="flex min-w-0 flex-1 flex-col gap-3 border-l border-border p-2 pl-3">
            <div className="grid grid-cols-2 gap-2">
              <label className="flex flex-col gap-1 text-[12px] text-muted">
                De
                <input ref={fromInput} type="date" required value={from} onChange={event => setFrom(event.target.value)} className="h-10 rounded-[12px] bg-surface px-3 font-mono text-[13px] text-foreground" />
              </label>
              <label className="flex flex-col gap-1 text-[12px] text-muted">
                Até
                <input type="date" required value={to} onChange={event => setTo(event.target.value)} className="h-10 rounded-[12px] bg-surface px-3 font-mono text-[13px] text-foreground" />
              </label>
            </div>
            {error && <p role="alert" className="text-[12px] text-danger-strong">{error}</p>}
            {runs && (
              <p className="rounded-[12px] bg-surface p-3 text-[12px] text-muted">
                {settlementImportedAt && <><strong className="font-semibold text-foreground">Liquidações lidas até {brDate(settlementImportedAt)}</strong> — </>}
                A janela usa a data da venda. O lado da liquidação estende alguns dias além do fim da janela para pegar repasses atrasados.
              </p>
            )}
            <div className="mt-auto flex justify-end gap-2">
              <button type="button" onClick={() => setOpen(false)} className="h-9 rounded-full bg-surface px-4 text-[13px] font-semibold">Cancelar</button>
              <button type="submit" className="h-9 rounded-full bg-foreground px-4 text-[13px] font-semibold text-white">Aplicar</button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}

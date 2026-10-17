'use client'

import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import { SideDrawer } from '@/components/ui/SideDrawer'
import type { Run } from '@/lib/api'
import { formatWindow, monthWindow } from '@/lib/period'
import { useStartReconciliation } from '@/lib/queries'
import { newRunWindowError } from '@/lib/reconciliations'
import { recentMonths } from '@/lib/transactions'
import { cn } from '@/lib/utils'

const CUSTOM = 'custom'
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível conciliar.'

const field = 'h-12 rounded-md bg-surface px-3 text-[14px] outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'

function Field({ id, label, children }: { id: string; label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-[13px] font-semibold">{label}</label>
      {children}
    </div>
  )
}

type Props = { merchantId: string; onClose: () => void; onStarted: (run: Run) => void }

/**
 * Side drawer that starts a reconciliation of any window of up to 366 days, checked here with the API's rules
 * before it is called. Opens on the current month; editing a date switches "Mês" to "Personalizado".
 */
export function NewRunDrawer({ merchantId, onClose, onStarted }: Props) {
  const start = useStartReconciliation(merchantId)
  const sending = useRef(false)
  const [months] = useState(() => recentMonths(new Date()))
  const [initial] = useState(() => monthWindow(new Date()))
  const [month, setMonth] = useState(initial.fromDate)
  const [fromDate, setFromDate] = useState(initial.fromDate)
  const [toDate, setToDate] = useState(initial.toDate)
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  function chooseMonth(value: string) {
    setMonth(value)
    if (value === CUSTOM) return
    const window = monthWindow(value)
    setFromDate(window.fromDate)
    setToDate(window.toDate)
  }

  function editDate(set: (value: string) => void, value: string) {
    set(value)
    setMonth(CUSTOM)
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (sending.current) return
    const invalid = newRunWindowError(fromDate, toDate)
    setError(invalid)
    if (invalid) return

    sending.current = true
    setPending(true)
    try {
      onStarted(await start.mutateAsync({ fromDate, toDate }))
    } catch (failure) {
      setError(messageOf(failure))
    } finally {
      sending.current = false
      setPending(false)
    }
  }

  return (
    <SideDrawer
      title="Nova conciliação"
      description="Escolha a janela, de até 366 dias, que será conciliada."
      formId="new-run-form"
      submitLabel={pending ? 'Conciliando…' : 'Conciliar'}
      pending={pending}
      error={error}
      onSubmit={event => void submit(event)}
      onClose={onClose}
    >
      <Field id="new-run-month" label="Mês">
        <select id="new-run-month" value={month} onChange={event => chooseMonth(event.target.value)} className={field}>
          {months.map(window => <option key={window.fromDate} value={window.fromDate}>{formatWindow(window)}</option>)}
          <option value={CUSTOM}>Personalizado</option>
        </select>
      </Field>

      <div className="grid grid-cols-2 gap-3">
        <Field id="new-run-from" label="De">
          <input id="new-run-from" type="date" value={fromDate} onChange={event => editDate(setFromDate, event.target.value)} className={cn(field, 'font-mono')} />
        </Field>
        <Field id="new-run-to" label="Até">
          <input id="new-run-to" type="date" value={toDate} onChange={event => editDate(setToDate, event.target.value)} className={cn(field, 'font-mono')} />
        </Field>
      </div>
    </SideDrawer>
  )
}

'use client'

import { Check, X } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react'
import { ApiError, type DiscrepancyStatus, type DiscrepancyType, type ReconciliationItem } from '@/lib/api'
import { formatMoney, toCents } from '@/lib/money'
import type { Window } from '@/lib/period'
import { useDiscrepancy, useResolveDiscrepancy } from '@/lib/queries'
import { cn } from '@/lib/utils'
import { METHOD_LABELS, STATUS_LABELS, TYPE_LABELS, shortDate } from './format'

export const NOTE_LIMIT = 500
export const CONFLICT_MESSAGE = 'Esta divergência mudou. Recarregamos o estado atual.'
export const ZERO_AMOUNT_MESSAGE = 'Informe um valor diferente de zero'

export type DrawerEntry = { item: ReconciliationItem; discrepancyId: string }

export type ResolveDrawerProps = {
  merchantId: string
  window: Window
  runId: string
  /** The loaded list of open discrepancies; J/K move inside it. */
  list: DrawerEntry[]
  index: number
  /** Open discrepancies of the run, for the footer ("3 de 12 abertas"). */
  total: number | null
  locked: boolean
  onClose: () => void
  onNavigate: (index: number) => void
}

type Outcome = Extract<DiscrepancyStatus, 'ACCEPTED' | 'ADJUSTED' | 'WRITTEN_OFF'>
type Field = 'gross' | 'net' | 'method' | 'installments' | 'status' | 'date'

const OUTCOMES: Array<{ status: Outcome; label: string; description: string; action: string }> = [
  { status: 'ACCEPTED', label: 'Aceitar', description: 'A diferença está correta e fica registrada.', action: 'Aceitar' },
  { status: 'ADJUSTED', label: 'Ajustar', description: 'Lança uma correção com sinal.', action: 'Salvar ajuste' },
  { status: 'WRITTEN_OFF', label: 'Baixar', description: 'Encerra sem correção.', action: 'Baixar' },
]

const DIVERGENT_FIELD: Partial<Record<DiscrepancyType, Field>> = {
  INCORRECT_AMOUNT: 'gross',
  FEE_DIVERGENCE: 'net',
  MISSING_SETTLEMENT: 'net',
  ORPHAN_SETTLEMENT: 'net',
  PAYMENT_METHOD_MISMATCH: 'method',
  INSTALLMENTS_MISMATCH: 'installments',
  STATUS_MISMATCH: 'status',
}

const method = (value: string | null) => value === null ? '—' : METHOD_LABELS[value]?.label ?? value
const money = (value: string | null) => value === null ? '—' : formatMoney(value)
const text = (value: string | number | null) => value === null ? '—' : String(value)
const dateTime = (iso: string) => new Date(iso).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })

/** "−1.000,00", "-1000.00" or "1000,5" → "-1000.00"-style decimal text; null when it is not a number with up to 2 decimals. */
export function parseAmount(input: string): string | null {
  let value = input.replace(/\s|R\$/g, '').replace(/−/g, '-')
  if (value.includes(',')) value = value.replace(/\./g, '').replace(',', '.')
  return /^-?\d+(\.\d{1,2})?$/.test(value) ? value : null
}

function comparison(item: ReconciliationItem, type: DiscrepancyType, actualValue: string | null) {
  const settled = item.externalSettlementId !== null
  const settlementSide = <T,>(field: Field, value: T) => DIVERGENT_FIELD[type] === field ? actualValue : settled ? value : null
  return [
    { field: 'gross' as const, label: 'Valor bruto', internal: money(item.transactionAmount), acquirer: money(item.settlementAmount) },
    { field: 'net' as const, label: 'Líquido', internal: money(item.expectedNetAmount), acquirer: money(item.settlementNetAmount) },
    { field: 'method' as const, label: 'Método', internal: method(item.paymentMethod), acquirer: method(settlementSide('method', item.paymentMethod)) },
    { field: 'installments' as const, label: 'Parcelas', internal: text(item.installments), acquirer: text(settlementSide('installments', item.installments)) },
    { field: 'status' as const, label: 'Status', internal: text(item.transactionStatus), acquirer: text(item.settlementStatus) },
    { field: 'date' as const, label: 'Data', internal: item.transactionDate ? shortDate(item.transactionDate) : '—', acquirer: item.settlementDate ? shortDate(item.settlementDate) : '—' },
  ]
}

export function ResolveDrawer({ merchantId, window, runId, list, index, total, locked, onClose, onNavigate }: ResolveDrawerProps) {
  const entry = list[index]
  const { item, discrepancyId } = entry
  const snapshot = item.discrepancies.find(discrepancy => discrepancy.id === discrepancyId)!
  const detail = useDiscrepancy(merchantId, runId, discrepancyId)
  const resolve = useResolveDiscrepancy(merchantId, window)
  const status = detail.data?.status ?? snapshot.status
  const panel = useRef<HTMLDivElement>(null)

  const [outcome, setOutcome] = useState<Outcome | null>(null)
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [amountError, setAmountError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // A different discrepancy starts with a clean form.
  useEffect(() => {
    setOutcome(null); setAmount(''); setNote(''); setAmountError(null); setError(null)
  }, [discrepancyId])

  useEffect(() => { panel.current?.focus() }, [])

  const noteTooLong = note.length > NOTE_LIMIT

  async function save(update: { status: DiscrepancyStatus; correctionAmount?: string }) {
    setError(null)
    try {
      await resolve.mutateAsync({ runId, discrepancyId, update: { ...update, ...(note.trim() ? { note } : {}) } })
      setOutcome(null); setAmount(''); setNote('')
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 409) {
        setError(CONFLICT_MESSAGE)
        await detail.refetch()
      } else {
        setError(failure instanceof Error ? failure.message : 'Não foi possível salvar.')
      }
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    if (!outcome || noteTooLong) return
    if (outcome !== 'ADJUSTED') return void save({ status: outcome })
    const parsed = parseAmount(amount)
    if (parsed === null || toCents(parsed) === BigInt(0)) {
      setAmountError(ZERO_AMOUNT_MESSAGE)
      return
    }
    setAmountError(null)
    void save({ status: 'ADJUSTED', correctionAmount: parsed })
  }

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') { event.preventDefault(); onClose(); return }
    if (event.key === 'Tab') {
      const focusable = Array.from(panel.current?.querySelectorAll<HTMLElement>('button:not([disabled]), input, textarea, a[href]') ?? [])
      if (focusable.length === 0) return
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && (document.activeElement === first || document.activeElement === panel.current)) { event.preventDefault(); last.focus() }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
      return
    }
    const target = event.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return
    if ((event.key === 'j' || event.key === 'J') && index < list.length - 1) onNavigate(index + 1)
    if ((event.key === 'k' || event.key === 'K') && index > 0) onNavigate(index - 1)
  }

  const transitions = [...(detail.data?.transitions ?? [])].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  const adjustments = detail.data?.adjustments ?? []
  const divergent = DIVERGENT_FIELD[snapshot.type]
  const selected = OUTCOMES.find(option => option.status === outcome)

  return (
    <div className="fixed inset-0 z-40 bg-[rgba(11,13,10,0.42)]" onClick={onClose}>
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-label="Resolver divergência"
        tabIndex={-1}
        onKeyDown={onKeyDown}
        onClick={event => event.stopPropagation()}
        className="absolute top-4 right-4 bottom-4 flex w-[560px] max-w-[calc(100%-32px)] flex-col rounded-[28px] bg-white shadow-[0_24px_64px_rgba(0,0,0,0.24)] outline-none"
      >
        <header className="flex flex-col gap-3 p-6 pb-4">
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-primary-soft px-2.5 py-1 text-[12px] font-semibold">{TYPE_LABELS[snapshot.type]}</span>
            <span data-testid="drawer-status" className={cn('rounded-full px-2.5 py-1 text-[12px] font-semibold', status === 'OPEN' ? 'bg-danger-soft text-danger-strong' : 'bg-primary-soft text-success')}>
              {STATUS_LABELS[status]}
            </span>
            <button type="button" aria-label="Fechar" onClick={onClose} className="ml-auto grid size-9 place-items-center rounded-full bg-surface">
              <X aria-hidden="true" className="size-4" />
            </button>
          </div>
          <h2 className="font-mono text-[22px] font-semibold">{item.externalReference}</h2>
          <p className="text-[13px] text-muted">
            {method(item.paymentMethod)}{item.transactionDate ? ` · venda de ${shortDate(item.transactionDate)}` : ''} · snapshot do run
          </p>
        </header>

        <div className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 pb-6">
          <table aria-label="Comparação" className="w-full rounded-[18px] bg-surface p-2 text-[13px]">
            <thead className="text-[12px] text-muted">
              <tr>
                <th scope="col" className="w-32 px-3 py-2 text-left font-normal">Campo</th>
                <th scope="col" className="px-3 py-2 text-left font-normal">Venda interna</th>
                <th scope="col" className="px-3 py-2 text-left font-normal">Adquirente</th>
              </tr>
            </thead>
            <tbody>
              {comparison(item, snapshot.type, snapshot.actualValue).map(row => (
                <tr key={row.field} data-divergent={row.field === divergent || undefined} className={cn(row.field === divergent && 'bg-white outline-[1.5px] outline-danger outline-solid')}>
                  <th scope="row" className="px-3 py-2 text-left font-normal text-muted">{row.label}</th>
                  <td className="px-3 py-2 font-mono">{row.internal}</td>
                  <td className={cn('px-3 py-2 font-mono', row.field === divergent && 'text-danger')}>{row.acquirer}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {locked && <p className="rounded-md bg-surface px-4 py-3 text-[13px] text-muted">Período travado · somente leitura</p>}

          {!locked && status === 'OPEN' && (
            <form id="resolve-form" onSubmit={submit} className="flex flex-col gap-4" noValidate>
              <fieldset className="flex flex-col gap-2">
                <legend className="mb-2 text-[14px] font-semibold">Desfecho</legend>
                <div className="grid grid-cols-3 gap-2">
                  {OUTCOMES.map(option => (
                    <label key={option.status} className={cn('flex cursor-pointer flex-col gap-1 rounded-md p-3 shadow-[inset_0_0_0_1px_var(--color-border)]', outcome === option.status && 'bg-foreground text-white')}>
                      <input type="radio" name="outcome" value={option.status} checked={outcome === option.status} onChange={() => setOutcome(option.status)} className="sr-only" />
                      <span className="text-[14px] font-semibold">{option.label}</span>
                      <span className={cn('text-[12px]', outcome === option.status ? 'text-white/70' : 'text-muted')}>{option.description}</span>
                    </label>
                  ))}
                </div>
              </fieldset>

              {outcome === 'ADJUSTED' && (
                <div className="flex flex-col gap-2">
                  <label htmlFor="correction" className="text-[13px] font-semibold">Valor da correção</label>
                  <div className="flex h-12 items-center gap-2 rounded-md px-3 shadow-[inset_0_0_0_2px_var(--color-foreground)]">
                    <span className="text-muted">R$</span>
                    <input
                      id="correction"
                      inputMode="decimal"
                      value={amount}
                      onChange={event => setAmount(event.target.value)}
                      aria-invalid={amountError !== null}
                      aria-describedby="correction-help"
                      className="min-w-0 flex-1 bg-transparent font-mono outline-none"
                    />
                    <button
                      type="button"
                      onClick={() => setAmount(item.netDifference.replace('.', ','))}
                      className="h-8 shrink-0 rounded-full bg-surface px-3 text-[12px] font-semibold"
                    >
                      Usar diferença
                    </button>
                  </div>
                  {amountError
                    ? <p role="alert" className="text-[12px] text-danger">{amountError}</p>
                    : <p id="correction-help" className="text-[12px] text-muted">Com sinal e diferente de zero. Não altera a venda, o snapshot nem o próximo run.</p>}
                </div>
              )}

              <div className="flex flex-col gap-2">
                <div className="flex items-center justify-between">
                  <label htmlFor="note" className="text-[13px] font-semibold">Nota (opcional)</label>
                  <span data-testid="note-counter" className={cn('font-mono text-[12px]', noteTooLong ? 'text-danger' : 'text-muted')}>{note.length}/{NOTE_LIMIT}</span>
                </div>
                <textarea id="note" value={note} onChange={event => setNote(event.target.value)} rows={3} className="rounded-[14px] bg-surface p-3 text-[13px] outline-none" />
              </div>
            </form>
          )}

          {error && <p role="alert" className="rounded-md bg-danger-soft px-4 py-3 text-[13px] text-danger-strong">{error}</p>}

          <section className="flex flex-col gap-3">
            <h3 className="text-[14px] font-semibold">Histórico</h3>
            <ol aria-label="Histórico" className="flex flex-col gap-3 border-l-2 border-border pl-4">
              {transitions.map(transition => (
                <li key={`${transition.createdAt}-${transition.toStatus}`} className="relative flex flex-col text-[13px]">
                  <span aria-hidden="true" className="absolute top-1.5 -left-[22px] size-2.5 rounded-full bg-foreground" />
                  <span className="font-semibold">{STATUS_LABELS[transition.fromStatus]} → {STATUS_LABELS[transition.toStatus]}</span>
                  {transition.note && <span>{transition.note}</span>}
                  <span className="font-mono text-[12px] text-muted">{dateTime(transition.createdAt)}</span>
                </li>
              ))}
              {adjustments.map((adjustment, position) => (
                <li key={`adjustment-${position}`} className="text-[13px] text-muted">
                  lançamento de {formatMoney(adjustment.amount, { currency: false })}{adjustment.voided ? ' anulado' : ''}
                </li>
              ))}
              {detail.data && transitions.length === 0 && adjustments.length === 0 && <li className="text-[13px] text-muted">Sem alterações desde o run</li>}
            </ol>
          </section>
        </div>

        <footer className="flex items-center gap-2 border-t border-border px-6 py-4">
          <span className="mr-auto text-[13px] text-muted">
            {index + 1} de {total ?? list.length} abertas · J / K navega
          </span>
          <button type="button" onClick={onClose} className="h-10 rounded-full bg-surface px-4 text-[14px] font-semibold">Cancelar</button>
          {!locked && status === 'OPEN' && (
            <button type="submit" form="resolve-form" disabled={!selected || noteTooLong || resolve.isPending} className="flex h-10 items-center gap-2 rounded-full bg-primary px-4 text-[14px] font-semibold disabled:bg-surface disabled:text-muted">
              {selected?.action ?? 'Salvar'} <Check aria-hidden="true" className="size-4" />
            </button>
          )}
          {!locked && status !== 'OPEN' && (
            <button type="button" disabled={resolve.isPending} onClick={() => void save({ status: 'OPEN' })} className="h-10 rounded-full bg-foreground px-4 text-[14px] font-semibold text-white">
              Reabrir
            </button>
          )}
        </footer>
      </div>
    </div>
  )
}

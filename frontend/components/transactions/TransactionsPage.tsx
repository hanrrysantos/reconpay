'use client'

import { Plus } from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { METHOD_LABELS } from '@/components/dashboard/format'
import { PeriodMenu } from '@/components/dashboard/PeriodMenu'
import { NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import type { PaymentMethod, Transaction, TransactionFilters, TransactionStatus } from '@/lib/api'
import { monthWindow, type Window } from '@/lib/period'
import { TRANSACTION_STATUS_LABELS, invalidWindowError, recentMonths } from '@/lib/transactions'
import { StatusDialog } from './StatusDialog'
import { TransactionDrawer } from './TransactionDrawer'
import { TransactionList } from './TransactionList'

const STATUSES: TransactionStatus[] = ['APPROVED', 'CANCELLED', 'REFUNDED', 'CHARGEBACK']
const METHODS: PaymentMethod[] = ['CREDIT_CARD', 'DEBIT_CARD', 'PIX', 'BOLETO']
const select = 'h-11 rounded-full bg-surface px-4 text-[14px] font-semibold outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'

function Filter({ id, label, value, onChange, children }: { id: string; label: string; value: string; onChange: (value: string) => void; children: ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      <label htmlFor={id} className="text-[13px] text-muted">{label}</label>
      <select id={id} value={value} onChange={event => onChange(event.target.value)} className={select}>
        <option value="">Todos</option>
        {children}
      </select>
    </div>
  )
}

/**
 * `/transacoes`: the transactions of the merchant selected in the top menu, filtered by period, status and method
 * (kept in the page state, not in the URL). New sales come from a drawer; approved ones change status in a dialog.
 */
export function TransactionsPage() {
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const [months] = useState(() => recentMonths(new Date()))
  const [period, setPeriod] = useState<Window>(() => monthWindow(new Date()))
  const [status, setStatus] = useState<TransactionStatus | ''>('')
  const [method, setMethod] = useState<PaymentMethod | ''>('')
  const [page, setPage] = useState(0)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [changing, setChanging] = useState<Transaction | null>(null)
  /** The transaction just registered, named in the notice. */
  const [created, setCreated] = useState<Transaction | null>(null)
  // Switching merchants closes the drawer and the dialog, drops the notice and goes back to page 1; filters stay.
  const [shownMerchantId, setShownMerchantId] = useState(merchantId)
  if (shownMerchantId !== merchantId) {
    setShownMerchantId(merchantId)
    setDrawerOpen(false)
    setChanging(null)
    setCreated(null)
    setPage(0)
  }
  // While the top menu has not loaded (or failed), the page waits, as the other merchant pages do.
  const noMerchant = !merchantsLoading && !merchantsError && merchantId === null
  const filters: TransactionFilters = { window: period, ...(status && { status }), ...(method && { paymentMethod: method }) }

  function applyPeriod(next: Window) {
    const error = invalidWindowError(next)
    if (error) throw new Error(error)
    setPeriod(next)
    setPage(0)
  }

  function onCreated(transaction: Transaction) {
    setDrawerOpen(false)
    setCreated(transaction)
  }

  return (
    <>
      <section className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="truncate text-[14px] text-muted">{merchant?.name}</p>
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Transações</h1>
        </div>
        {merchantId !== null && (
          <button type="button" onClick={() => setDrawerOpen(true)} className="flex h-11 items-center gap-2 rounded-full bg-primary px-5 text-[14px] font-semibold text-foreground">
            <Plus aria-hidden="true" className="size-4" />
            Nova transação
          </button>
        )}
      </section>

      {noMerchant && <NoMerchant />}

      {merchantId !== null && (
        <>
          {created && (
            <section role="status" className="rounded-lg bg-primary-soft px-5 py-4 text-[14px]">
              Transação <strong className="font-semibold">{created.externalReference}</strong> cadastrada.
            </section>
          )}

          <div className="flex flex-wrap items-center gap-3">
            <PeriodMenu window={period} months={months} onApply={applyPeriod} align="start" />
            <Filter id="transaction-status-filter" label="Status" value={status} onChange={value => { setStatus(value as TransactionStatus | ''); setPage(0) }}>
              {STATUSES.map(item => <option key={item} value={item}>{TRANSACTION_STATUS_LABELS[item]}</option>)}
            </Filter>
            <Filter id="transaction-method-filter" label="Método" value={method} onChange={value => { setMethod(value as PaymentMethod | ''); setPage(0) }}>
              {METHODS.map(item => <option key={item} value={item}>{METHOD_LABELS[item].label}</option>)}
            </Filter>
          </div>

          <TransactionList key={merchantId} merchantId={merchantId} filters={filters} page={page} onPageChange={setPage} onChangeStatus={setChanging} />

          {drawerOpen && <TransactionDrawer merchantId={merchantId} onClose={() => setDrawerOpen(false)} onCreated={onCreated} />}
          {changing !== null && <StatusDialog merchantId={merchantId} transaction={changing} onClose={() => setChanging(null)} />}
        </>
      )}
    </>
  )
}

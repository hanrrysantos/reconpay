'use client'

import { useQueryClient } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { TYPE_LABELS } from '@/components/dashboard/format'
import { PeriodMenu, windowKey } from '@/components/dashboard/PeriodMenu'
import { ResolveDrawer, STALE_RUN_NOTICE, type DrawerEntry } from '@/components/dashboard/ResolveDrawer'
import { BlockError, NoMerchant } from '@/components/dashboard/states'
import { useSelectedMerchant } from '@/components/shell/MerchantMenu'
import type { DiscrepancyType, Period, Run } from '@/lib/api'
import { STATUS_FILTERS, type StatusFilter } from '@/lib/discrepancies'
import { monthWindow, type Window } from '@/lib/period'
import { useLatestRun, usePeriod, useRuns } from '@/lib/queries'
import { anyInFlight } from '@/lib/reconciliations'
import { invalidWindowError } from '@/lib/transactions'
import { DiscrepancyList } from './DiscrepancyList'
import { WindowNotice } from './WindowNotice'

const TYPES = Object.keys(TYPE_LABELS) as DiscrepancyType[]
const select = 'h-11 rounded-full bg-surface px-4 text-[14px] font-semibold outline-none focus:shadow-[inset_0_0_0_2px_var(--color-foreground)]'
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível carregar as conciliações.'

function Filter({ id, label, value, onChange, children }: { id: string; label: string; value: string; onChange: (value: string) => void; children: ReactNode }) {
  return (
    <div className="flex items-center gap-2">
      <label htmlFor={id} className="text-[13px] text-muted">{label}</label>
      <select id={id} value={value} onChange={event => onChange(event.target.value)} className={select}>{children}</select>
    </div>
  )
}

type Filters = { status: StatusFilter; type: DiscrepancyType | null }

/** Lock state of every window whose period is already cached, for the marks of the period menu. */
function useLocks(merchantId: string, window: Window, period: Period | undefined): Record<string, boolean> {
  const queryClient = useQueryClient()
  const locks: Record<string, boolean> = {}
  for (const [queryKey, data] of queryClient.getQueriesData<Period>({ queryKey: ['period', merchantId] })) {
    if (data) locks[`${queryKey[2]}/${queryKey[3]}`] = data.locked
  }
  if (period) locks[windowKey(window)] = period.locked
  return locks
}

/** One merchant: its window, the current run of that window, the list and the drawer. Remounted per merchant. */
function DiscrepanciesWindow({ merchantId, merchantName, runs, initial, filters, onFilters }: {
  merchantId: string
  merchantName?: string
  runs: Run[]
  initial: Window
  filters: Filters
  onFilters: (filters: Filters) => void
}) {
  const [window, setWindow] = useState(initial)
  const [page, setPage] = useState(0)
  /**
   * The rows of the page and the run when the drawer opened; they stay fixed until it closes, so J/K keep their
   * order and a newer run of the window never receives an action meant for these rows.
   */
  const [drawer, setDrawer] = useState<{ list: DrawerEntry[]; index: number; runId: string } | null>(null)
  const run = useLatestRun(merchantId, window)
  const period = usePeriod(merchantId, window)
  const locks = useLocks(merchantId, window, period.data)
  const current = run.data
  const locked = (period.data?.locked ?? false) || anyInFlight(current ? [current] : [])

  function applyWindow(next: Window) {
    const error = invalidWindowError(next)
    if (error) throw new Error(error)
    setWindow(next)
    setPage(0)
    setDrawer(null)
  }

  function changeFilters(next: Filters) {
    onFilters(next)
    setPage(0)
  }

  return (
    <>
      <section className="flex flex-wrap items-end justify-between gap-6">
        <div className="flex min-w-0 flex-col gap-2">
          <p className="truncate text-[14px] text-muted">{merchantName}</p>
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Divergências</h1>
        </div>
        <PeriodMenu window={window} runs={runs} locks={locks} onApply={applyWindow} />
      </section>

      <div className="flex flex-wrap items-center gap-4">
        <Filter id="discrepancy-status-filter" label="Status" value={filters.status} onChange={value => changeFilters({ ...filters, status: value as StatusFilter })}>
          {STATUS_FILTERS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </Filter>
        <Filter id="discrepancy-type-filter" label="Tipo" value={filters.type ?? ''} onChange={value => changeFilters({ ...filters, type: value === '' ? null : value as DiscrepancyType })}>
          <option value="">Todos</option>
          {TYPES.map(type => <option key={type} value={type}>{TYPE_LABELS[type]}</option>)}
        </Filter>
      </div>

      {run.isPending && <div role="status" aria-label="Carregando conciliação" aria-busy="true" className="h-40 animate-pulse rounded-lg bg-surface" />}
      {run.isError && <BlockError message={messageOf(run.error)} action="Tentar novamente" onRetry={() => void run.refetch()} />}
      {current !== undefined && <WindowNotice run={current} locked={period.data?.locked ?? false} />}
      {current?.status === 'COMPLETED' && (
        <DiscrepancyList
          merchantId={merchantId}
          runId={current.id}
          status={filters.status}
          type={filters.type}
          page={page}
          onPageChange={setPage}
          onOpen={(list, index) => setDrawer({ list, index, runId: current.id })}
        />
      )}

      {drawer && (
        <ResolveDrawer
          merchantId={merchantId}
          window={window}
          runId={drawer.runId}
          list={drawer.list}
          index={drawer.index}
          total={drawer.list.length}
          footerNoun={null}
          locked={locked || drawer.runId !== current?.id}
          lockedNotice={drawer.runId !== current?.id ? STALE_RUN_NOTICE : undefined}
          onClose={() => setDrawer(null)}
          onNavigate={index => setDrawer({ ...drawer, index })}
        />
      )}
    </>
  )
}

/**
 * `/divergencias`: the discrepancies of the current run of a window of the merchant selected in the top menu.
 * The window starts at the merchant's latest run (or the current month); status and type survive a merchant switch.
 */
export function DiscrepanciesPage() {
  const { merchant, merchantId, isLoading: merchantsLoading, error: merchantsError } = useSelectedMerchant()
  const runs = useRuns(merchantId)
  const [filters, setFilters] = useState<Filters>({ status: 'OPEN', type: null })
  const noMerchant = !merchantsLoading && !merchantsError && merchantId === null

  if (noMerchant) {
    return (
      <>
        <section className="flex min-w-0 flex-col gap-2">
          <h1 className="font-display text-[44px] leading-none font-extrabold tracking-[-0.04em]">Divergências</h1>
        </section>
        <NoMerchant />
      </>
    )
  }
  if (merchantId === null || runs.isPending) {
    return <div role="status" aria-label="Carregando divergências" aria-busy="true" className="h-40 animate-pulse rounded-lg bg-surface" />
  }
  if (runs.isError) return <BlockError message={messageOf(runs.error)} action="Tentar novamente" onRetry={() => void runs.refetch()} />

  const latest = runs.data.content[0]
  const initial = latest ? { fromDate: latest.fromDate, toDate: latest.toDate } : monthWindow(new Date())
  return (
    <DiscrepanciesWindow
      key={merchantId}
      merchantId={merchantId}
      merchantName={merchant?.name}
      runs={runs.data.content}
      initial={initial}
      filters={filters}
      onFilters={setFilters}
    />
  )
}

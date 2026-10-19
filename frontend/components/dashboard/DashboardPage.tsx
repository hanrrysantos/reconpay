'use client'

import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { ApiError, getPeriod, type DiscrepancyType, type Period, type PeriodSummary, type Run } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { monthWindow, type Window } from '@/lib/period'
import {
  queryKeys, useExportRun, useLatestImports, useLatestRun, useLockPeriod, useOpenDiscrepancies, usePeriod, usePeriodSummary,
  useRunReconciliation, useRuns, useUnlockPeriod,
} from '@/lib/queries'
import { cn } from '@/lib/utils'
import { MerchantSwitcher, useSelectedMerchant } from '@/components/shell/MerchantMenu'
import { CashflowChart } from './CashflowChart'
import { CloseStepper } from './CloseStepper'
import { DailyHeatmap } from './DailyHeatmap'
import { openCount } from './format'
import { KpiCards } from './KpiCards'
import { MatchRingCard } from './MatchRingCard'
import { OpenDiscrepancyTable, rowDiscrepancy } from './OpenDiscrepancyTable'
import { PageHeader } from './PageHeader'
import { PeriodMenu, windowKey } from './PeriodMenu'
import { ResolveDrawer, type DrawerEntry } from './ResolveDrawer'
import { TypeDonut } from './TypeDonut'
import { BlockError, DashboardSkeleton, EmptyOnboarding, Forbidden, NetworkBanner, NoMerchant, RunFailedCard } from './states'
import { deriveDashboardView, type Loaded } from './view'

const statusOf = (error: unknown) => error instanceof ApiError ? error.status : undefined
/** A rejected fetch becomes an ApiError without status (lib/api.ts). */
const isNetworkError = (error: unknown) => error instanceof ApiError && error.status === undefined
const messageOf = (error: unknown) => error instanceof Error ? error.message : 'Não foi possível concluir a ação.'
const inFlight = (run: Run | null | undefined) => run?.status === 'PENDING' || run?.status === 'RUNNING'

/** Dashboard at `/`: picks the merchant's initial window (latest run, or the current month), then shows it. */
export function DashboardPage({ today = new Date() }: { today?: Date }) {
  const queryClient = useQueryClient()
  const { merchantId, isLoading, error: merchantsError } = useSelectedMerchant()
  const runs = useRuns(merchantId)
  const [chosen, setChosen] = useState<{ merchantId: string; window: Window } | null>(null)
  const retryAll = () => void queryClient.refetchQueries({ type: 'active' })

  const banner = [merchantsError, runs.error].some(isNetworkError) ? <NetworkBanner onRetry={retryAll} /> : null
  const fallbackWindow = monthWindow(today)

  if (isLoading || (merchantId !== null && runs.data === undefined && !runs.error)) {
    return <>{banner}<DashboardSkeleton /></>
  }
  if (merchantId === null) {
    if (merchantsError) return <>{banner}<DashboardSkeleton /></>
    return <>
      <PageHeader view={{ kind: 'noMerchant' }} window={fallbackWindow} onRun={() => {}} onExport={() => {}} />
      <NoMerchant />
    </>
  }
  if (statusOf(runs.error) === 403) {
    return <>
      <PageHeader view={{ kind: 'forbidden' }} window={fallbackWindow} onRun={() => {}} onExport={() => {}} />
      <Forbidden merchantMenu={<MerchantSwitcher />} />
    </>
  }
  if (!runs.data) {
    return <>{banner ?? <BlockError message="Os runs não carregaram" onRetry={() => void runs.refetch()} />}<DashboardSkeleton /></>
  }

  const latest = runs.data.content[0]
  const initial = latest ? { fromDate: latest.fromDate, toDate: latest.toDate } : fallbackWindow
  const window = chosen?.merchantId === merchantId ? chosen.window : initial

  return (
    <DashboardWindow
      key={merchantId}
      merchantId={merchantId}
      window={window}
      runs={runs.data.content}
      today={today}
      onWindow={next => setChosen({ merchantId, window: next })}
    />
  )
}

function DashboardWindow({ merchantId, window, runs, today, onWindow }: {
  merchantId: string
  window: Window
  runs: Run[]
  today: Date
  onWindow: (window: Window) => void
}) {
  const queryClient = useQueryClient()
  const { token } = useAuth()

  const run = useLatestRun(merchantId, window)
  const period = usePeriod(merchantId, window)
  const summary = usePeriodSummary(merchantId, window)
  const imports = useLatestImports(merchantId)
  // While a run is in flight `/periods` and `/periods/summary` answer 409. TanStack Query v5 keeps the last real
  // data of the same key on error (only placeholder data from another window is dropped), so the previous numbers
  // of this window stay visible.
  const periodData = period.data
  const summaryData = summary.data

  const [typeFilter, setTypeFilter] = useState<DiscrepancyType | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  /** The run of the table when the drawer opened: a newer run of the window never receives its actions. */
  const [drawer, setDrawer] = useState<{ list: DrawerEntry[]; index: number; runId: string } | null>(null)

  const runMutation = useRunReconciliation(merchantId, window)
  const lock = useLockPeriod(merchantId, window)
  const unlock = useUnlockPeriod(merchantId, window)
  const exportRun = useExportRun(merchantId)

  const view = deriveDashboardView({
    merchant: { isLoading: false, merchantId },
    window,
    run: { data: run.data, error: run.error },
    period: { data: periodData, error: period.error },
    summary: { data: summaryData, error: summary.error },
  })
  const loaded: Loaded | undefined = view.kind === 'open' || view.kind === 'locked' ? view.data
    : view.kind === 'running' || view.kind === 'failed' ? view.previous : undefined
  const tableRunId = loaded?.period.runId ?? null
  const open = useOpenDiscrepancies(merchantId, tableRunId, typeFilter ?? undefined)

  // DASH-05 AC4: when the polled run reaches COMPLETED, reload the window's period, summary and open discrepancies.
  const previousStatus = useRef(run.data?.status)
  useEffect(() => {
    const status = run.data?.status
    const was = previousStatus.current
    previousStatus.current = status
    if ((was === 'PENDING' || was === 'RUNNING') && status === 'COMPLETED') {
      void queryClient.invalidateQueries({ queryKey: queryKeys.period(merchantId, window) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.summary(merchantId, window) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.openDiscrepancies(merchantId) })
      void queryClient.invalidateQueries({ queryKey: queryKeys.runs(merchantId), exact: true })
    }
  }, [run.data?.status, merchantId, window, queryClient])

  const networkDown = [run.error, period.error, summary.error, imports.error, open.error].some(isNetworkError)

  async function startRun() {
    setNotice(null)
    try {
      await runMutation.mutateAsync()
    } catch (error) {
      setNotice(messageOf(error))
      // DASH-05 AC6: a 409 means the window already has a run in flight; poll it.
      if (statusOf(error) === 409) await queryClient.invalidateQueries({ queryKey: queryKeys.latestRun(merchantId, window) })
    }
  }

  async function changeLock(action: 'lock' | 'unlock') {
    setNotice(null)
    try {
      const result = await (action === 'lock' ? lock : unlock).mutateAsync()
      queryClient.setQueryData(queryKeys.period(merchantId, window), result)
    } catch (error) {
      setNotice(messageOf(error))
      if (statusOf(error) === 409) await queryClient.invalidateQueries({ queryKey: queryKeys.period(merchantId, window) })
    }
  }

  async function exportCsv() {
    if (!loaded) return
    setNotice(null)
    try {
      const blob = await exportRun.mutateAsync(loaded.period.runId)
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `reconpay-${window.fromDate}-${window.toDate}.csv`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setNotice(messageOf(error))
    }
  }

  /** A 400 keeps the current window (the menu shows the API message); any other answer moves to the new window. */
  async function applyWindow(next: Window) {
    try {
      await queryClient.fetchQuery({ queryKey: queryKeys.period(merchantId, next), queryFn: () => getPeriod(token!, merchantId, next) })
    } catch (error) {
      if (statusOf(error) === 400 || isNetworkError(error)) throw error
    }
    setTypeFilter(null)
    setDrawer(null)
    onWindow(next)
  }

  const locks: Record<string, boolean> = {}
  for (const [queryKey, data] of queryClient.getQueriesData<Period>({ queryKey: ['period', merchantId] })) {
    if (data) locks[`${queryKey[2]}/${queryKey[3]}`] = data.locked
  }
  if (periodData) locks[windowKey(window)] = periodData.locked

  const header = (
    <PageHeader
      view={view}
      window={window}
      periodMenu={<PeriodMenu window={window} runs={runs} locks={locks} onApply={applyWindow} settlementImportedAt={imports.data?.settlementImportedAt} today={today} />}
      onRun={() => void startRun()}
      onExport={() => void exportCsv()}
    />
  )
  const banner = networkDown ? <NetworkBanner onRetry={() => void queryClient.refetchQueries({ type: 'active' })} /> : null
  const noticeLine = notice && <p role="status" className="rounded-md bg-surface px-4 py-3 text-[13px]">{notice}</p>

  if (view.kind === 'forbidden') return <>{banner}{header}<Forbidden merchantMenu={<MerchantSwitcher />} /></>
  if (view.kind === 'loading' || view.kind === 'noMerchant') return <>{banner}{header}<DashboardSkeleton /></>

  const stepper = (
    <CloseStepper
      imports={imports.data}
      run={run.data ?? null}
      openCount={summaryData ? openCount(summaryData) : null}
      locked={periodData?.locked ?? false}
      onLock={() => void changeLock('lock')}
      onUnlock={() => void changeLock('unlock')}
    />
  )

  if (view.kind === 'empty') {
    return <>{banner}{header}{noticeLine}{stepper}<EmptyOnboarding imports={imports.data} onRun={() => void startRun()} /></>
  }

  const locked = view.kind === 'locked' || inFlight(run.data)
  const summaryBlock = (content: (summary: PeriodSummary) => ReactNode, message: string, action: string, className?: string) =>
    summaryData ? content(summaryData)
      : summary.error ? <BlockError message={message} action={action} onRetry={() => void summary.refetch()} className={className} />
        : <div aria-hidden="true" className={cn('min-h-40 animate-pulse rounded-lg bg-surface', className)} />

  const ring = view.kind === 'failed'
    ? <RunFailedCard run={view.run} onRetry={() => void startRun()} />
    : <MatchRingCard period={loaded?.period} summary={summaryData} run={run.data} />
  const openItems = open.data?.content
  const openTypes = summaryData
    ? (Object.entries(summaryData.openByType).filter(([, count]) => (count ?? 0) > 0).map(([type]) => type as DiscrepancyType))
    : []

  return (
    <>
      {banner}
      {header}
      {noticeLine}
      {stepper}
      <div className="flex flex-col gap-4 lg:flex-row">
        {ring}
        {loaded && (
          <div className={cn('flex min-w-0 flex-1', view.kind === 'running' && 'opacity-45')}>
            {summaryBlock(data => <CashflowChart days={data.days} bankReceivedAmount={data.bankReceivedAmount} expectedNetAmount={data.expectedNetAmount} />, 'O gráfico não carregou', 'Recarregar gráfico', 'flex-1')}
          </div>
        )}
      </div>
      {loaded && (
        <div className={cn('flex flex-col gap-6', view.kind === 'running' && 'opacity-45')}>
          <div className="flex flex-col gap-4 lg:flex-row">
            <KpiCards period={loaded.period} summary={summaryData} />
            {summaryBlock(data => <TypeDonut openByType={data.openByType} />, 'Os tipos não carregaram', 'Recarregar', 'lg:w-[420px]')}
          </div>
          <div className="flex flex-col items-start gap-4 lg:flex-row">
            {open.error && !open.data
              ? <BlockError message="As divergências não carregaram" onRetry={() => void open.refetch()} className="w-full flex-1" />
              : <OpenDiscrepancyTable
                  items={openItems}
                  total={open.data?.totalElements ?? null}
                  types={openTypes}
                  typeFilter={typeFilter}
                  onFilter={setTypeFilter}
                  locked={locked}
                  onResolve={(item, discrepancy) => {
                    if (!tableRunId) return
                    const list = (openItems ?? []).slice(0, 4).map(row => ({ item: row, discrepancyId: rowDiscrepancy(row, typeFilter)?.id ?? '' }))
                    const index = list.findIndex(entry => entry.item.id === item.id && entry.discrepancyId === discrepancy.id)
                    setDrawer({ list, index: Math.max(index, 0), runId: tableRunId })
                  }}
                />}
            {summaryBlock(data => <DailyHeatmap days={data.days} window={window} />, 'O mapa diário não carregou', 'Recarregar', 'lg:w-[420px]')}
          </div>
        </div>
      )}
      {drawer && (
        <ResolveDrawer
          merchantId={merchantId}
          window={window}
          runId={drawer.runId}
          list={drawer.list}
          index={drawer.index}
          total={summaryData ? openCount(summaryData) : null}
          locked={locked || drawer.runId !== tableRunId}
          onClose={() => setDrawer(null)}
          onNavigate={index => setDrawer({ ...drawer, index })}
        />
      )}
    </>
  )
}


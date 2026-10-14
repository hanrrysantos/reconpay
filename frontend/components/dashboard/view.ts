import { ApiError, type Period, type PeriodSummary, type Run } from '@/lib/api'
import type { Window } from '@/lib/period'

/** The part of a query result the view needs; `data` may be the previous key's data while an error is shown. */
export type QuerySlice<T> = { data?: T; error?: unknown }

export type Loaded = { period: Period; summary: PeriodSummary | undefined }

export type DashboardView =
  | { kind: 'loading' }
  | { kind: 'noMerchant' }
  | { kind: 'forbidden' }
  | { kind: 'empty'; window: Window }
  | { kind: 'running'; run: Run; previous?: Loaded }
  | { kind: 'failed'; run: Run; previous?: Loaded }
  | { kind: 'open'; data: Loaded }
  | { kind: 'locked'; data: Loaded }

export type DashboardViewInput = {
  merchant: { isLoading: boolean; merchantId: string | null }
  window: Window
  /** Latest run of the window; `null` when the window has none. */
  run: QuerySlice<Run | null>
  period: QuerySlice<Period>
  summary: QuerySlice<PeriodSummary>
}

const statusOf = (error: unknown) => error instanceof ApiError ? error.status : undefined

/**
 * Decides which dashboard screen to show. The run query drives running/failed, so the 409 that `/periods`
 * answers during a run never empties the page; a 404 from `/periods` means the window has no current run.
 */
export function deriveDashboardView({ merchant, window, run, period, summary }: DashboardViewInput): DashboardView {
  if (merchant.isLoading) return { kind: 'loading' }
  if (merchant.merchantId === null) return { kind: 'noMerchant' }
  if ([run, period, summary].some(query => statusOf(query.error) === 403)) return { kind: 'forbidden' }
  if (run.data === undefined) return { kind: 'loading' }

  const loaded: Loaded | undefined = period.data ? { period: period.data, summary: summary.data } : undefined
  const previous = loaded ? { previous: loaded } : {}

  if (run.data?.status === 'PENDING' || run.data?.status === 'RUNNING') return { kind: 'running', run: run.data, ...previous }
  if (run.data?.status === 'FAILED') return { kind: 'failed', run: run.data, ...previous }
  if (run.data === null || statusOf(period.error) === 404) return { kind: 'empty', window }
  if (!loaded) return { kind: 'loading' }
  return loaded.period.locked ? { kind: 'locked', data: loaded } : { kind: 'open', data: loaded }
}

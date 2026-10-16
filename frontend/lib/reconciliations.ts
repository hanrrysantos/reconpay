import { STATUS_LABELS, TYPE_LABELS } from '@/components/dashboard/format'
import type { Discrepancy, ReconciliationItem, Run, RunStatus } from './api'
import type { Window } from './period'
import { invalidWindowError } from './transactions'

export const RUN_STATUS_LABELS: Record<RunStatus, string> = {
  PENDING: 'Na fila',
  RUNNING: 'Em andamento',
  COMPLETED: 'Concluída',
  FAILED: 'Falhou',
}

type ItemResult = ReconciliationItem['result']

/** The result of one item, in the Resultado column. */
export const RESULT_LABELS: Record<ItemResult, string> = { MATCHED: 'Bateu', DIVERGENT: 'Divergente' }
/** The options of the Resultado filter. */
export const RESULT_FILTER_LABELS: Record<ItemResult, string> = { MATCHED: 'Bateram', DIVERGENT: 'Divergentes' }

export const MAX_WINDOW_DAYS = 366
export const DATES_REQUIRED_MESSAGE = 'Informe as duas datas'
export const WINDOW_TOO_LONG_MESSAGE = `Janela de conciliação excede o máximo de ${MAX_WINDOW_DAYS} dias`

const DAY_MS = 24 * 60 * 60 * 1000
const utcDay = (iso: string) => { const [year, month, day] = iso.split('-').map(Number); return Date.UTC(year, month - 1, day) }

/** Days of the window counting both ends, from the ISO text so the time zone never shifts the day. */
export function windowDays({ fromDate, toDate }: Window): number {
  return Math.round((utcDay(toDate) - utcDay(fromDate)) / DAY_MS) + 1
}

/** The same rules as the API, checked before sending. */
export function newRunWindowError(fromDate: string, toDate: string): string | null {
  if (fromDate === '' || toDate === '') return DATES_REQUIRED_MESSAGE
  const window = { fromDate, toDate }
  return invalidWindowError(window) ?? (windowDays(window) > MAX_WINDOW_DAYS ? WINDOW_TOO_LONG_MESSAGE : null)
}

/** Itens column: progress while RUNNING, "—" while PENDING, the total otherwise. */
export function itemsProgress(run: Pick<Run, 'status' | 'processedItems' | 'totalItems'>): string {
  if (run.status === 'PENDING') return '—'
  if (run.status === 'RUNNING') return `${run.processedItems ?? 0} de ${run.totalItems ?? 0}`
  return run.totalItems === null ? '—' : String(run.totalItems)
}

export function discrepancyBadge({ type, status }: Pick<Discrepancy, 'type' | 'status'>): string {
  return `${TYPE_LABELS[type]} · ${STATUS_LABELS[status]}`
}

export function anyInFlight(runs: readonly Pick<Run, 'status'>[]): boolean {
  return runs.some(run => run.status === 'PENDING' || run.status === 'RUNNING')
}

import { ApiError, type Period, type Run } from './api'
import type { Window } from './period'

export const CLOSING_PAGE_SIZE = 10

export type PeriodStatus = 'locked' | 'open' | 'running' | 'none' | 'error' | 'loading'

export const PERIOD_STATUS_LABELS: Record<'locked' | 'open' | 'running' | 'none', string> = {
  locked: 'Travado',
  open: 'Aberto',
  running: 'Em andamento',
  none: 'Sem conciliação concluída',
}

/** One window per date pair of the runs, newest start date first; on a tie, newest end date first. */
export function periodWindows(runs: readonly Pick<Run, 'fromDate' | 'toDate'>[]): Window[] {
  const windows = new Map<string, Window>()
  for (const { fromDate, toDate } of runs) windows.set(`${fromDate}/${toDate}`, { fromDate, toDate })
  return [...windows.values()].sort((a, b) => b.fromDate.localeCompare(a.fromDate) || b.toDate.localeCompare(a.toDate))
}

/** Seconds from the run's end to the lock: "29 s", "3 min", "2 h 5 min", "1 d 3 h"; "—" when absent. */
export function formatDuration(seconds: number | null): string {
  if (seconds === null) return '—'
  if (seconds < 60) return `${seconds} s`
  if (seconds < 3600) return `${Math.floor(seconds / 60)} min`
  if (seconds < 86400) {
    const minutes = Math.floor(seconds / 60) % 60
    return `${Math.floor(seconds / 3600)} h${minutes ? ` ${minutes} min` : ''}`
  }
  const hours = Math.floor(seconds / 3600) % 24
  return `${Math.floor(seconds / 86400)} d${hours ? ` ${hours} h` : ''}`
}

const pad = (value: number) => String(value).padStart(2, '0')

/** `dd/mm/aaaa hh:mm` in local time; "—" when absent. */
export function formatLockedAt(iso: string | null): string {
  if (iso === null) return '—'
  const date = new Date(iso)
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** The row status from the period read: an error wins over data kept from an earlier read. */
export function periodStatus({ data, error }: { data?: Period; error: unknown }): PeriodStatus {
  if (error) {
    const status = error instanceof ApiError ? error.status : undefined
    return status === 409 ? 'running' : status === 404 ? 'none' : 'error'
  }
  if (data) return data.locked ? 'locked' : 'open'
  return 'loading'
}

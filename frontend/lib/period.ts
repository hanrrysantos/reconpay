export type Window = { fromDate: string; toDate: string }

export const MONTH_NAMES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
] as const

const pad = (value: number) => String(value).padStart(2, '0')

/** Year and zero-based month; ISO strings are read as text so the time zone never shifts the day. */
function yearMonth(date: Date | string): [number, number] {
  if (typeof date !== 'string') return [date.getFullYear(), date.getMonth()]
  const [year, month] = date.split('-').map(Number)
  return [year, month - 1]
}

function windowOf(year: number, month: number): Window {
  const lastDay = new Date(Date.UTC(year, month + 1, 0)).getUTCDate()
  return { fromDate: `${year}-${pad(month + 1)}-01`, toDate: `${year}-${pad(month + 1)}-${pad(lastDay)}` }
}

export function monthWindow(date: Date | string): Window {
  return windowOf(...yearMonth(date))
}

/** Calendar months from `current` back to the month of `oldest` (inclusive), newest first. */
export function monthsBack(current: Date | string, oldest: Date | string | null): Window[] {
  const monthIndex = (date: Date | string) => { const [year, month] = yearMonth(date); return year * 12 + month }
  const start = monthIndex(current)
  const end = oldest === null ? start : Math.min(start, monthIndex(oldest))
  const windows: Window[] = []
  for (let index = start; index >= end; index--) windows.push(windowOf(Math.floor(index / 12), index % 12))
  return windows
}

const brDate = (iso: string) => iso.split('-').reverse().join('/')

export function formatWindow(window: Window): string {
  const [year, month] = yearMonth(window.fromDate)
  const whole = windowOf(year, month)
  if (whole.fromDate === window.fromDate && whole.toDate === window.toDate) return `${MONTH_NAMES[month]} de ${year}`
  return `${brDate(window.fromDate)} – ${brDate(window.toDate)}`
}

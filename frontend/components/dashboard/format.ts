import type { PeriodSummary } from '@/lib/api'
import { MONTH_NAMES, formatWindow, monthWindow, type Window } from '@/lib/period'

const MINUS = '−'

/** A 0–1 decimal string (scale 4) in basis points, read as text so it never rounds through a float. */
function basisPoints(rate: string): number {
  const [whole, fraction = ''] = rate.replace('-', '').split('.')
  const value = Number(whole) * 10000 + Number(fraction.padEnd(4, '0').slice(0, 4))
  return rate.startsWith('-') ? -value : value
}

const oneDecimal = (tenths: number) => {
  const text = (Math.abs(tenths) / 10).toFixed(1).replace('.', ',')
  return text.endsWith(',0') ? text.slice(0, -2) : text
}

/** "0.9420" → "94,2%"; a null rate (no item in the window) reads as 0%. */
export function formatRate(rate: string | null): string {
  return `${oneDecimal(Math.round(basisPoints(rate ?? '0') / 10))}%`
}

/** Difference between two rates in percentage points: "+1,8 pp", "−0,4 pp". */
export function formatRateDelta(rate: string, previous: string): string {
  const tenths = Math.round((basisPoints(rate) - basisPoints(previous)) / 10)
  return `${tenths < 0 ? MINUS : '+'}${oneDecimal(tenths)} pp`
}

/** "setembro" for a whole calendar month, otherwise the date range. */
export function windowName(window: Window): string {
  const month = monthWindow(window.fromDate)
  if (month.fromDate === window.fromDate && month.toDate === window.toDate) {
    return MONTH_NAMES[Number(window.fromDate.slice(5, 7)) - 1].toLowerCase()
  }
  return formatWindow(window)
}

export const capitalize = (text: string) => text.charAt(0).toUpperCase() + text.slice(1)

export function openCount(summary: Pick<PeriodSummary, 'openByType'>): number {
  return Object.values(summary.openByType).reduce<number>((total, count) => total + (count ?? 0), 0)
}

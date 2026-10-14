import type { DiscrepancyStatus, DiscrepancyType, PeriodSummary } from '@/lib/api'
import { MONTH_NAMES, formatWindow, monthWindow, type Window } from '@/lib/period'

const MINUS = '−'

export const TYPE_LABELS: Record<DiscrepancyType, string> = {
  MISSING_SETTLEMENT: 'Liquidação ausente',
  ORPHAN_SETTLEMENT: 'Liquidação órfã',
  INCORRECT_AMOUNT: 'Valor bruto incorreto',
  FEE_DIVERGENCE: 'Taxa divergente',
  STATUS_MISMATCH: 'Status inconsistente',
  PAYMENT_METHOD_MISMATCH: 'Método divergente',
  INSTALLMENTS_MISMATCH: 'Parcelas divergentes',
  BANK_AMOUNT_MISMATCH: 'Valor no banco divergente',
  MISSING_BANK_CREDIT: 'Sem linha no extrato',
  ORPHAN_BANK_CREDIT: 'Crédito sem venda',
  AMBIGUOUS_BANK_MATCH: 'Extrato ambíguo',
}

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

/**
 * A percentage the API already computed and rounded ("6.2", "-3.8", "3.03"), shown as text without
 * re-rounding (AD-010): "6,2%", "−3,8%"; `signed` prefixes "+" on a positive value.
 */
export function formatPercent(value: string, { signed = false } = {}): string {
  const negative = value.startsWith('-')
  const digits = negative ? value.slice(1) : value
  const zero = /^0*(\.0*)?$/.test(digits)
  const sign = zero ? '' : negative ? MINUS : signed ? '+' : ''
  return `${sign}${digits.replace('.', ',')}%`
}

/** The only four statuses a discrepancy can have; there is no "Em análise". */
export const STATUS_LABELS: Record<DiscrepancyStatus, string> = {
  OPEN: 'Aberta',
  ACCEPTED: 'Aceita',
  ADJUSTED: 'Ajustada',
  WRITTEN_OFF: 'Baixada',
}

export const METHOD_LABELS: Record<string, { short: string; label: string }> = {
  CREDIT_CARD: { short: 'CR', label: 'Crédito' },
  DEBIT_CARD: { short: 'DB', label: 'Débito' },
  PIX: { short: 'PIX', label: 'Pix' },
  BOLETO: { short: 'BOL', label: 'Boleto' },
}

/** "2026-09-26" → "26 set". */
export const shortDate = (iso: string) => `${iso.slice(8, 10)} ${MONTH_NAMES[Number(iso.slice(5, 7)) - 1].slice(0, 3).toLowerCase()}`

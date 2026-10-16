import { ApiError, type TransactionStatus } from './api'
import { monthsBack, type Window } from './period'

export const REFERENCE_REQUIRED_MESSAGE = 'Informe a referência externa'
export const REFERENCE_TOO_LONG_MESSAGE = 'Use até 100 caracteres'
export const AMOUNT_MESSAGE = 'Informe um valor maior que zero com até 2 casas decimais'
export const DATE_REQUIRED_MESSAGE = 'Informe a data da transação'
export const FUTURE_DATE_MESSAGE = 'Data da transação não pode ser futura'
export const INVALID_WINDOW_MESSAGE = 'A data inicial deve ser igual ou anterior à final'
/** The API's text when no active fee rule matches; the only way to tell it from the other `409 CONFLICT`s. */
export const MISSING_FEE_RULE_MESSAGE = 'Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas'

export const TRANSACTION_STATUS_LABELS: Record<TransactionStatus, string> = {
  APPROVED: 'Aprovada',
  CANCELLED: 'Cancelada',
  REFUNDED: 'Estornada',
  CHARGEBACK: 'Chargeback',
}

/** Statuses an approved transaction can move to; none of them has a way out. */
export const FINAL_STATUSES: readonly TransactionStatus[] = ['CANCELLED', 'REFUNDED', 'CHARGEBACK']

const AMOUNT_INPUT = /^(\d+)(?:[.,](\d{1,2}))?$/
const MAX_AMOUNT_DIGITS = 17
const MAX_REFERENCE_LENGTH = 100

/** `"1234,5"` → `"1234.5"`; `null` when empty, not a number, zero, with more than 17 integer digits or 2 decimals. Text only (AD-010). */
export function parseAmount(input: string): string | null {
  const match = AMOUNT_INPUT.exec(input.trim())
  if (!match) return null
  const [, whole, fraction] = match
  const digits = whole.replace(/^0+(?=\d)/, '')
  if (digits.length > MAX_AMOUNT_DIGITS || !/[1-9]/.test(digits + (fraction ?? ''))) return null
  return fraction === undefined ? digits : `${digits}.${fraction}`
}

/** The reference without surrounding spaces, or the field error. */
export function parseReference(input: string): { value: string } | { error: string } {
  const value = input.trim()
  if (value === '') return { error: REFERENCE_REQUIRED_MESSAGE }
  if (value.length > MAX_REFERENCE_LENGTH) return { error: REFERENCE_TOO_LONG_MESSAGE }
  return { value }
}

/** Both dates are `yyyy-mm-dd`, compared as text so the time zone never shifts the day. */
export function dateError(date: string, today: string): string | null {
  if (date === '') return DATE_REQUIRED_MESSAGE
  return date > today ? FUTURE_DATE_MESSAGE : null
}

const pad = (value: number) => String(value).padStart(2, '0')

/** The browser's local date as `yyyy-mm-dd`, the same day the operator sees. */
export function todayIso(now = new Date()): string {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

/** The month of `today` and the 11 before it, newest first. */
export function recentMonths(today: Date): Window[] {
  return monthsBack(today, new Date(today.getFullYear(), today.getMonth() - 11, 1))
}

export function invalidWindowError({ fromDate, toDate }: Window): string | null {
  return fromDate > toDate ? INVALID_WINDOW_MESSAGE : null
}

export function isMissingFeeRule(error: unknown): boolean {
  return error instanceof ApiError && error.message === MISSING_FEE_RULE_MESSAGE
}

/** `"2026-10-03"` → `"03/10/2026"`, read as text. */
export function formatTransactionDate(iso: string): string {
  return iso.split('-').reverse().join('/')
}

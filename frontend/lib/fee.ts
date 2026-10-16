import type { PaymentMethod } from './api'

/** Methods that cannot be paid in installments: their fee rule always has 1 installment. */
export const SINGLE_INSTALLMENT_METHODS: readonly PaymentMethod[] = ['PIX', 'BOLETO', 'DEBIT_CARD']

const INPUT = (decimals: number) => new RegExp(`^(\\d+)(?:[.,](\\d{1,${decimals}}))?$`)
const PERCENT_INPUT = INPUT(4)
const FIXED_FEE_INPUT = INPUT(2)

/** Typed text as a JSON number literal: dot separator, no leading zeros. Text only, never a float (AD-010). */
function normalize(whole: string, fraction: string | undefined): string {
  const digits = whole.replace(/^0+(?=\d)/, '')
  return fraction === undefined ? digits : `${digits}.${fraction}`
}

/** `"2,99"` or `"2.99"` → `"2.99"`; `null` when empty, not a number, below 0, above 100 or with more than 4 decimals. */
export function parseFeePercent(input: string): string | null {
  const match = PERCENT_INPUT.exec(input.trim())
  if (!match) return null
  const [, whole, fraction] = match
  const value = normalize(whole, fraction)
  const [digits] = value.split('.')
  const aboveHundred = digits.length > 3 || (digits.length === 3 && digits > '100') || (digits === '100' && /[1-9]/.test(fraction ?? ''))
  return aboveHundred ? null : value
}

/** `"0,30"` → `"0.30"`; `null` when empty, not a number, negative or with more than 2 decimals. */
export function parseFixedFee(input: string): string | null {
  const match = FIXED_FEE_INPUT.exec(input.trim())
  return match ? normalize(match[1], match[2]) : null
}

/** `"2.9900"` → `"2,99%"`, `"0.0000"` → `"0%"`: trailing zeros dropped, comma separator. */
export function formatPercent(value: string): string {
  const [whole, fraction = ''] = value.split('.')
  const kept = fraction.replace(/0+$/, '')
  return `${whole}${kept ? `,${kept}` : ''}%`
}

/** API decimal as the text of an input: `"2.9900"` → `"2,99"`, `"0.30"` → `"0,30"` (zeros dropped past the 2nd decimal). */
export function toInputDecimal(value: string): string {
  const [whole, fraction] = value.split('.')
  if (fraction === undefined) return whole
  return `${whole},${fraction.slice(0, 2)}${fraction.slice(2).replace(/0+$/, '')}`
}

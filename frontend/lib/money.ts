const DECIMAL = /^(-?)(\d+)(?:\.(\d{1,2}))?$/
const MINUS = '−'

/** Formats a decimal string as pt-BR money without converting it to a binary float. */
export function formatMoney(value: string, { currency = true, signed = false }: { currency?: boolean; signed?: boolean } = {}): string {
  const match = DECIMAL.exec(value)
  if (!match) return currency ? `R$ ${value}` : value
  const [, minus, whole, cents = ''] = match
  const digits = whole.replace(/^0+(?=\d)/, '')
  const fraction = cents.padEnd(2, '0')
  const isZero = /^0+$/.test(digits + fraction)
  const sign = isZero ? '' : minus ? MINUS : signed ? '+' : ''
  return `${sign}${currency ? 'R$ ' : ''}${digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${fraction}`
}

function toCents(value: string | null): bigint {
  if (value === null) return BigInt(0)
  const match = DECIMAL.exec(value)
  if (!match) throw new Error(`Valor decimal inválido: ${value}`)
  const [, minus, whole, cents = ''] = match
  const absolute = BigInt(whole + cents.padEnd(2, '0'))
  return minus ? -absolute : absolute
}

/** `a − b` in integer cents; a null side counts as zero. Only for pre-filling inputs (AD-010). */
export function subtractDecimal(a: string | null, b: string | null): string {
  const result = toCents(a) - toCents(b)
  const negative = result < BigInt(0)
  const digits = (negative ? -result : result).toString().padStart(3, '0')
  return `${negative ? '-' : ''}${digits.slice(0, -2)}.${digits.slice(-2)}`
}

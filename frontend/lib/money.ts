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

export function toCents(value: string | null): bigint {
  if (value === null) return BigInt(0)
  const match = DECIMAL.exec(value)
  if (!match) throw new Error(`Valor decimal inválido: ${value}`)
  const [, minus, whole, cents = ''] = match
  const absolute = BigInt(whole + cents.padEnd(2, '0'))
  return minus ? -absolute : absolute
}

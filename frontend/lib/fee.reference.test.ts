import { describe, expect, it } from 'vitest'
import { formatPercent, parseFeePercent, parseFixedFee, toInputDecimal } from './fee'

/*
 * Reference check of lib/fee.ts. The app reads and writes decimals as text (AD-010), which leaves many paths through
 * the bound checks; instead of one example per path, every combination below is compared with an independent
 * reference that does exact integer arithmetic with BigInt. BigInt lives only in this test, never in the app.
 */

/** Every leading digit past the bound (100..999 by hundreds), plus zeros, small values and longer numbers. */
const WHOLES = ['0', '00', '1', '01', '9', '10', '99', '099', '100', '0100', '00100', '101', '0101', '109', '110', '199',
  '200', '300', '400', '500', '600', '700', '800', '899', '900', '999', '1000', '1001', '10000', '12345']
/** Every digit 0..9 alone and in each position up to 4 decimals, plus zero runs and a 5-decimal overflow. */
const FRACTIONS = [undefined, ...new Set([
  '0', '00', '000', '0000', '00000', '99999', '00001',
  ...'0123456789'.split('').flatMap(digit => [digit, `${digit}0`, `0${digit}`, `00${digit}`, `${digit}00`, `000${digit}`, `${digit}000`]),
])]
const SEPARATORS = [',', '.']

function* inputs(): Generator<{ text: string; whole: string; fraction?: string }> {
  for (const whole of WHOLES) {
    for (const fraction of FRACTIONS) {
      if (fraction === undefined) { yield { text: whole, whole }; continue }
      for (const separator of SEPARATORS) yield { text: `${whole}${separator}${fraction}`, whole, fraction }
    }
  }
}

/** 10^scale as a BigInt, built from text so the project's TypeScript target needs no bigint literals. */
const power = (scale: number) => BigInt(`1${'0'.repeat(scale)}`)
/** 100 with 4 decimals as a scaled integer. */
const HUNDRED = BigInt(100) * power(4)

/** Value scaled by 10^scale as an exact integer: ("2", "99", 4) → 29900. */
const scaled = (whole: string, fraction: string, scale: number) => BigInt(whole) * power(scale) + BigInt(fraction.padEnd(scale, '0') || '0')

/** Expected text sent to the API: whole part without leading zeros, the typed fraction kept as is. */
const expectedText = (whole: string, fraction?: string) => fraction === undefined ? BigInt(whole).toString() : `${BigInt(whole)}.${fraction}`

function referencePercent(whole: string, fraction?: string): string | null {
  if (fraction !== undefined && fraction.length > 4) return null
  return scaled(whole, fraction ?? '', 4) <= HUNDRED ? expectedText(whole, fraction) : null
}

function referenceFixedFee(whole: string, fraction?: string): string | null {
  if (fraction !== undefined && fraction.length > 2) return null
  return expectedText(whole, fraction)
}

/** API text of a scaled integer with exactly `scale` decimals: (29900, 4) → "2.9900". */
function apiText(value: bigint, scale: number): string {
  const digits = value.toString().padStart(scale + 1, '0')
  return `${digits.slice(0, -scale)}.${digits.slice(-scale)}`
}

/** Every API value 0.0000..100.0000 whose decimals come from the fraction list, as BigDecimal text. */
function* apiPercents(): Generator<{ text: string; value: bigint }> {
  for (const whole of ['0', '1', '2', '9', '10', '50', '99', '100']) {
    for (const fraction of ['0000', '0001', '0009', '0010', '0050', '0090', '0100', '0500', '1000', '1500', '9000', '9900', '9990', '9999', '0505', '5005']) {
      const value = scaled(whole, fraction, 4)
      if (value <= HUNDRED) yield { text: apiText(value, 4), value }
    }
  }
}

describe('lib/fee.ts contra a referência exata', () => {
  it('parseFeePercent aceita e recusa como a referência em todas as combinações (FCAD-04, FCAD-08)', () => {
    const mismatches = [...inputs()].filter(({ text, whole, fraction }) => parseFeePercent(text) !== referencePercent(whole, fraction))
      .map(({ text, whole, fraction }) => `${text}: got ${parseFeePercent(text)}, expected ${referencePercent(whole, fraction)}`)
    expect(mismatches).toEqual([])
  })

  it('parseFixedFee aceita e recusa como a referência em todas as combinações (FCAD-04, FCAD-09)', () => {
    const mismatches = [...inputs()].filter(({ text, whole, fraction }) => parseFixedFee(text) !== referenceFixedFee(whole, fraction))
      .map(({ text, whole, fraction }) => `${text}: got ${parseFixedFee(text)}, expected ${referenceFixedFee(whole, fraction)}`)
    expect(mismatches).toEqual([])
  })

  it('formatPercent mostra o mesmo valor, com vírgula e sem zeros à direita (FLIST-03)', () => {
    const mismatches = [...apiPercents()].filter(({ text, value }) => {
      const shown = formatPercent(text)
      const match = /^(\d+)(?:,(\d*[1-9]))?%$/.exec(shown)
      return !match || scaled(match[1], match[2] ?? '', 4) !== value
    }).map(({ text }) => `${text} → ${formatPercent(text)}`)
    expect(mismatches).toEqual([])
  })

  it('toInputDecimal preenche a edição com o mesmo valor, ao menos 2 casas, e salvar sem mudar o mantém (FEDIT-01, FEDIT-02)', () => {
    const mismatches = [...apiPercents()].filter(({ text, value }) => {
      const prefilled = toInputDecimal(text)
      const match = /^(\d+),(\d{2,4})$/.exec(prefilled)
      const parsed = parseFeePercent(prefilled)
      return !match || scaled(match[1], match[2], 4) !== value || parsed === null || scaled(...(parsed.split('.') as [string, string]), 4) !== value
    }).map(({ text }) => `${text} → ${toInputDecimal(text)}`)
    expect(mismatches).toEqual([])
  })
})

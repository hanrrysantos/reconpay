const CNPJ_LENGTH = 14
const FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
const SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]

export const cnpjDigits = (value: string) => value.replace(/\D/g, '')

/** Progressive `00.000.000/0000-00` mask; keeps at most 14 digits and drops anything else. */
export function maskCnpj(input: string): string {
  const value = cnpjDigits(input).slice(0, CNPJ_LENGTH)
  const parts = [value.slice(0, 2), value.slice(2, 5), value.slice(5, 8), value.slice(8, 12), value.slice(12)]
  return parts[0] + (parts[1] && `.${parts[1]}`) + (parts[2] && `.${parts[2]}`) + (parts[3] && `/${parts[3]}`) + (parts[4] && `-${parts[4]}`)
}

/** A 14-digit document formatted as CNPJ; any other value is shown as it is. */
export function formatCnpj(document: string): string {
  return cnpjDigits(document).length === CNPJ_LENGTH ? maskCnpj(document) : document
}

function checkDigit(digits: number[], weights: number[]): number {
  const remainder = weights.reduce((sum, weight, index) => sum + digits[index] * weight, 0) % 11
  return remainder < 2 ? 0 : 11 - remainder
}

/** Same rule as the API: only digits and CNPJ punctuation, 14 digits, not all equal, both check digits right. */
export function isValidCnpj(value: string): boolean {
  if (!/^[\d./\-\s]*$/.test(value)) return false
  const digits = cnpjDigits(value).split('').map(Number)
  if (digits.length !== CNPJ_LENGTH || digits.every(digit => digit === digits[0])) return false
  return checkDigit(digits, FIRST_WEIGHTS) === digits[12] && checkDigit(digits, SECOND_WEIGHTS) === digits[13]
}

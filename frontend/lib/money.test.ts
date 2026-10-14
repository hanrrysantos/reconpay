import { describe, expect, it } from 'vitest'
import { formatMoney } from './money'

describe('formatMoney', () => {
  it('formata o valor do card com R$, milhar e centavos', () => {
    expect(formatMoney('10389.38')).toBe('R$ 10.389,38')
    expect(formatMoney('0.1')).toBe('R$ 0,10')
    expect(formatMoney('100')).toBe('R$ 100,00')
  })

  it('formata o valor da tabela sem R$ e com o sinal de menos U+2212', () => {
    expect(formatMoney('4870.00', { currency: false })).toBe('4.870,00')
    expect(formatMoney('-4870.00', { currency: false })).toBe('−4.870,00')
    expect(formatMoney('-1000.00')).toBe('−R$ 1.000,00')
  })

  it('mostra + em valor positivo só quando pedido', () => {
    expect(formatMoney('35.50', { signed: true })).toBe('+R$ 35,50')
    expect(formatMoney('-990.10', { currency: false, signed: true })).toBe('−990,10')
    expect(formatMoney('0.00', { signed: true })).toBe('R$ 0,00')
    expect(formatMoney('-0.00', { currency: false })).toBe('0,00')
  })

  it('formata valores acima de 13 dígitos sem arredondar os centavos', () => {
    expect(formatMoney('9007199254740993.27')).toBe('R$ 9.007.199.254.740.993,27')
    expect(formatMoney('-12345678901234567.89', { currency: false })).toBe('−12.345.678.901.234.567,89')
  })
})

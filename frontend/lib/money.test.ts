import { describe, expect, it } from 'vitest'
import { formatMoney, subtractDecimal } from './money'

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

describe('subtractDecimal', () => {
  it('subtrai em centavos e devolve string decimal com sinal ASCII', () => {
    expect(subtractDecimal('1297.03', '2287.13')).toBe('-990.10')
    expect(subtractDecimal('2287.13', '1297.03')).toBe('990.10')
    expect(subtractDecimal('0.05', '0.10')).toBe('-0.05')
    expect(subtractDecimal('10.00', '10.00')).toBe('0.00')
  })

  it('trata o lado nulo como zero', () => {
    expect(subtractDecimal(null, '150.00')).toBe('-150.00')
    expect(subtractDecimal('150.5', null)).toBe('150.50')
    expect(subtractDecimal(null, null)).toBe('0.00')
  })

  it('não perde centavos em valores acima de 13 dígitos', () => {
    expect(subtractDecimal('99999999999999999.99', '0.01')).toBe('99999999999999999.98')
  })
})

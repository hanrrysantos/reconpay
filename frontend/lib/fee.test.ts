import { describe, expect, it } from 'vitest'
import { SINGLE_INSTALLMENT_METHODS, formatPercent, parseFeePercent, parseFixedFee, toInputDecimal } from './fee'

describe('parseFeePercent', () => {
  it('aceita vírgula ou ponto e devolve o valor exato com ponto (FCAD-04)', () => {
    expect(parseFeePercent('2,99')).toBe('2.99')
    expect(parseFeePercent('2.99')).toBe('2.99')
  })

  it('aceita os limites 0, 100 e 4 casas decimais', () => {
    expect(parseFeePercent('0')).toBe('0')
    expect(parseFeePercent('100')).toBe('100')
    expect(parseFeePercent('2,9999')).toBe('2.9999')
    expect(parseFeePercent('0100')).toBe('100')
  })

  it('recusa vazio, texto, negativo, acima de 100 e mais de 4 casas (FCAD-08)', () => {
    expect(parseFeePercent('')).toBeNull()
    expect(parseFeePercent('abc')).toBeNull()
    expect(parseFeePercent('-1')).toBeNull()
    expect(parseFeePercent('100,01')).toBeNull()
    expect(parseFeePercent('101')).toBeNull()
    expect(parseFeePercent('999')).toBeNull()
    expect(parseFeePercent('1000')).toBeNull()
    expect(parseFeePercent('12345')).toBeNull()
    expect(parseFeePercent('0101')).toBeNull()
    expect(parseFeePercent('2,99999')).toBeNull()
  })
})

describe('parseFeePercent: tabela de limites (FCAD-08)', () => {
  // Every form of a value at, below and above the bound: whole, 1 to 4 zero decimals, leading zeros, each digit count.
  it.each([
    ['0', '0'], ['00', '0'], ['0,0', '0.0'], ['0,0000', '0.0000'],
    ['1', '1'], ['9,5', '9.5'], ['99', '99'], ['99,9999', '99.9999'],
    ['100', '100'], ['100,0', '100.0'], ['100,00', '100.00'], ['100,000', '100.000'], ['100,0000', '100.0000'],
    ['100.00', '100.00'], ['100.0000', '100.0000'], ['0100', '100'], ['00100,00', '100.00'], ['  100,00  ', '100.00'],
  ])('aceita "%s" como %s', (input, expected) => {
    expect(parseFeePercent(input)).toBe(expected)
  })

  it.each([
    ['100,0001'], ['100,1'], ['100,01'], ['100.5'], ['100,10'], ['100,50'], ['100,9'], ['100,06'], ['100,0090'],
    ['101'], ['101,00'], ['199'], ['200'], ['999,99'], ['0101'], ['00101'],
    ['1000'], ['1000,00'], ['10000'], ['12345'], ['99999999'],
    ['-0'], ['-1'], ['-100'], [''], [' '], ['abc'], ['1,'], [',5'], ['1,00000'], ['1.2.3'], ['1 000'], ['1e2'],
  ])('recusa "%s"', input => {
    expect(parseFeePercent(input)).toBeNull()
  })
})

describe('parseFixedFee', () => {
  it('aceita vírgula e zero, devolvendo o valor exato com ponto (FCAD-04)', () => {
    expect(parseFixedFee('0,30')).toBe('0.30')
    expect(parseFixedFee('0')).toBe('0')
  })

  it.each([
    ['0', '0'], ['0,0', '0.0'], ['0,00', '0.00'], ['0,3', '0.3'], ['00,30', '0.30'], ['1.5', '1.5'], ['1234567,89', '1234567.89'],
  ])('aceita "%s" como %s (FCAD-09)', (input, expected) => {
    expect(parseFixedFee(input)).toBe(expected)
  })

  it.each([['-0'], ['-1'], ['0,001'], ['1,'], [',3'], ['1.2.3'], ['1e2'], [' ']])('recusa "%s" (FCAD-09)', input => {
    expect(parseFixedFee(input)).toBeNull()
  })

  it('recusa vazio, negativo, mais de 2 casas e texto (FCAD-09)', () => {
    expect(parseFixedFee('')).toBeNull()
    expect(parseFixedFee('-0,01')).toBeNull()
    expect(parseFixedFee('0,301')).toBeNull()
    expect(parseFixedFee('x')).toBeNull()
  })
})

describe('formatPercent', () => {
  it('usa vírgula e tira os zeros à direita (FLIST-03)', () => {
    expect(formatPercent('2.9900')).toBe('2,99%')
    expect(formatPercent('0.0000')).toBe('0%')
    expect(formatPercent('100.0000')).toBe('100%')
    expect(formatPercent('1.5000')).toBe('1,5%')
    expect(formatPercent('2.0500')).toBe('2,05%')
    expect(formatPercent('10.0000')).toBe('10%')
    expect(formatPercent('0.0500')).toBe('0,05%')
  })
})

describe('toInputDecimal', () => {
  it('troca o ponto por vírgula para preencher a edição', () => {
    expect(toInputDecimal('2.9900')).toBe('2,99')
    expect(toInputDecimal('0.30')).toBe('0,30')
    expect(toInputDecimal('2.9999')).toBe('2,9999')
    expect(toInputDecimal('2.9990')).toBe('2,999')
    expect(toInputDecimal('2.0500')).toBe('2,05')
    expect(toInputDecimal('100.0000')).toBe('100,00')
  })
})

describe('SINGLE_INSTALLMENT_METHODS', () => {
  it('são Pix, Boleto e Débito (FCAD-02)', () => {
    expect([...SINGLE_INSTALLMENT_METHODS].sort()).toEqual(['BOLETO', 'DEBIT_CARD', 'PIX'])
  })
})

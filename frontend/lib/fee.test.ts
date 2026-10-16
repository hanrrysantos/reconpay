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
  })

  it('recusa vazio, texto, negativo, acima de 100 e mais de 4 casas (FCAD-08)', () => {
    expect(parseFeePercent('')).toBeNull()
    expect(parseFeePercent('abc')).toBeNull()
    expect(parseFeePercent('-1')).toBeNull()
    expect(parseFeePercent('100,01')).toBeNull()
    expect(parseFeePercent('2,99999')).toBeNull()
  })
})

describe('parseFixedFee', () => {
  it('aceita vírgula e zero, devolvendo o valor exato com ponto (FCAD-04)', () => {
    expect(parseFixedFee('0,30')).toBe('0.30')
    expect(parseFixedFee('0')).toBe('0')
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
  })
})

describe('toInputDecimal', () => {
  it('troca o ponto por vírgula para preencher a edição', () => {
    expect(toInputDecimal('2.9900')).toBe('2,99')
    expect(toInputDecimal('0.30')).toBe('0,30')
  })
})

describe('SINGLE_INSTALLMENT_METHODS', () => {
  it('são Pix, Boleto e Débito (FCAD-02)', () => {
    expect([...SINGLE_INSTALLMENT_METHODS].sort()).toEqual(['BOLETO', 'DEBIT_CARD', 'PIX'])
  })
})

import { describe, expect, it } from 'vitest'
import { formatPercent } from './format'

describe('formatPercent', () => {
  it('mostra o percentual do servidor com vírgula, sem arredondar de novo (AD-010)', () => {
    expect(formatPercent('3.03')).toBe('3,03%')
    expect(formatPercent('267.6')).toBe('267,6%')
    expect(formatPercent('2.90')).toBe('2,90%')
  })

  it('usa o sinal de menos U+2212 e só mostra + quando pedido', () => {
    expect(formatPercent('-3.8', { signed: true })).toBe('−3,8%')
    expect(formatPercent('6.2', { signed: true })).toBe('+6,2%')
    expect(formatPercent('6.2')).toBe('6,2%')
  })

  it('zero não leva sinal', () => {
    expect(formatPercent('0.0', { signed: true })).toBe('0,0%')
    expect(formatPercent('-0.0', { signed: true })).toBe('0,0%')
  })
})

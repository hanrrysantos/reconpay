import { describe, expect, it } from 'vitest'
import { CHART_RANGE, coverProgress, formatCount, rangeProgress } from './scrollProgress'

describe('coverProgress', () => {
  it('é 0 com o topo do bloco na base da tela', () => {
    expect(coverProgress(800, 300, 800)).toBe(0)
  })

  it('é 1 com a base do bloco no topo da tela', () => {
    expect(coverProgress(-300, 300, 800)).toBe(1)
  })

  it('é 0,5 com o bloco no meio da tela', () => {
    expect(coverProgress(250, 300, 800)).toBe(0.5)
  })

  it('fica entre 0 e 1 com o bloco fora da tela', () => {
    expect(coverProgress(1200, 300, 800)).toBe(0)
    expect(coverProgress(-900, 300, 800)).toBe(1)
  })
})

describe('rangeProgress', () => {
  it('é 0 antes do início, 1 depois do fim e a fração no meio', () => {
    expect(rangeProgress(0.02, 0.05, 0.45)).toBe(0)
    expect(rangeProgress(0.05, 0.05, 0.45)).toBe(0)
    expect(rangeProgress(0.25, 0.05, 0.45)).toBeCloseTo(0.5)
    expect(rangeProgress(0.45, 0.05, 0.45)).toBe(1)
    expect(rangeProgress(0.9, 0.05, 0.45)).toBe(1)
  })

  it('usa o mesmo intervalo do CSS para os gráficos (cover 5% a cover 45%)', () => {
    expect(CHART_RANGE).toEqual({ start: 0.05, end: 0.45 })
  })
})

describe('formatCount (LAND-04 AC6)', () => {
  it.each([
    [94.2, { decimals: 1, suffix: '%' }, '94,2%'],
    [1209, {}, '1.209'],
    [10389.38, { decimals: 2, prefix: 'R$ ' }, 'R$ 10.389,38'],
    [121212.5, { decimals: 2, prefix: 'R$ ' }, 'R$ 121.212,50'],
    [3787.5, { decimals: 2, prefix: 'R$ ' }, 'R$ 3.787,50'],
    [964, {}, '964'],
    [912, {}, '912'],
    [52, {}, '52'],
    [1284, {}, '1.284'],
    [1271, {}, '1.271'],
    [312, {}, '312'],
  ] as const)('formata %s como %s', (value, options, expected) => {
    expect(formatCount(value, options)).toBe(expected)
  })

  it('formata o zero com as mesmas casas', () => {
    expect(formatCount(0, { decimals: 1, suffix: '%' })).toBe('0,0%')
    expect(formatCount(0, { decimals: 2, prefix: 'R$ ' })).toBe('R$ 0,00')
    expect(formatCount(0)).toBe('0')
  })

  it('arredonda o valor do meio do caminho para as casas pedidas', () => {
    expect(formatCount(1284 * 0.5004, {})).toBe('643')
    expect(formatCount(94.2 * 0.333, { decimals: 1, suffix: '%' })).toBe('31,4%')
  })
})

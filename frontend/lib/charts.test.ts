import { describe, expect, it } from 'vitest'
import { donutSegments, gapPolygon, heatBand, ringDash, seriesPath } from './charts'

describe('ringDash', () => {
  it('desenha o arco proporcional à taxa sobre a circunferência', () => {
    expect(ringDash('0.9420', 439.8)).toBe('414.3 439.8')
  })

  it('vai de arco vazio em 0 a volta completa em 1', () => {
    expect(ringDash('0', 439.8)).toBe('0 439.8')
    expect(ringDash('0.0000', 439.8)).toBe('0 439.8')
    expect(ringDash('1.0000', 439.8)).toBe('439.8 439.8')
  })
})

describe('seriesPath', () => {
  it('traça um ponto por valor, do primeiro ao último x, com y invertido pela escala', () => {
    expect(seriesPath(['0.00', '50.00', '100.00'], 100, 720, 200)).toBe('M 0 200 L 360 100 L 720 0')
  })

  it('mantém a linha no chão quando a escala é zero', () => {
    expect(seriesPath(['0.00', '0.00'], 0, 720, 200)).toBe('M 0 200 L 720 200')
  })
})

describe('gapPolygon', () => {
  it('fecha o polígono entre a linha esperada e a recebida', () => {
    expect(gapPolygon(['100.00', '100.00', '100.00'], ['100.00', '50.00', '100.00'], 100, 720, 200))
      .toBe('M 0 0 L 360 0 L 720 0 L 720 0 L 360 100 L 0 0 Z')
  })

  it('não marca falta onde o banco recebeu mais que o esperado', () => {
    expect(gapPolygon(['50.00', '50.00'], ['100.00', '25.00'], 100, 720, 200))
      .toBe('M 0 100 L 720 100 L 720 150 L 0 100 Z')
  })
})

describe('donutSegments', () => {
  it('divide a circunferência pela contagem, com folga e deslocamento acumulado', () => {
    expect(donutSegments({ MISSING_SETTLEMENT: 3, AMOUNT_MISMATCH: 1 }, 54, 3)).toEqual([
      { key: 'MISSING_SETTLEMENT', count: 3, dasharray: '251.5 87.8', dashoffset: '0' },
      { key: 'AMOUNT_MISMATCH', count: 1, dasharray: '81.8 257.5', dashoffset: '-254.5' },
    ])
  })

  it('ignora tipos zerados e não desenha nada sem divergência', () => {
    expect(donutSegments({ A: 0, B: 2 }, 54, 3).map(segment => segment.key)).toEqual(['B'])
    expect(donutSegments({ A: 0 }, 54, 3)).toEqual([])
    expect(donutSegments({}, 54, 3)).toEqual([])
  })
})

describe('heatBand', () => {
  it('classifica o dia nas cinco faixas da referência', () => {
    expect(heatBand(100, 100)).toBe('full')
    expect(heatBand(99, 100)).toBe('full')
    expect(heatBand(98, 100)).toBe('soft')
    expect(heatBand(97, 100)).toBe('soft')
    expect(heatBand(96, 100)).toBe('neutral')
    expect(heatBand(94, 100)).toBe('neutral')
    expect(heatBand(93, 100)).toBe('warn')
    expect(heatBand(90, 100)).toBe('warn')
    expect(heatBand(89, 100)).toBe('bad')
    expect(heatBand(0, 3)).toBe('bad')
  })

  it('usa a faixa inferior quando o percentual fica entre duas faixas', () => {
    expect(heatBand(197, 200)).toBe('soft')
    expect(heatBand(179, 200)).toBe('bad')
  })

  it('deixa vazio o dia sem item', () => {
    expect(heatBand(0, 0)).toBe('empty')
  })
})

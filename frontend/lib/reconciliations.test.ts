import { describe, expect, it } from 'vitest'
import type { Run } from './api'
import {
  MAX_WINDOW_DAYS, RESULT_FILTER_LABELS, RESULT_LABELS, RUN_STATUS_LABELS, anyInFlight, discrepancyBadge, itemsProgress,
  newRunWindowError, windowDays,
} from './reconciliations'

const run = (overrides: Partial<Run> = {}): Run => ({
  id: 'r-1', merchantId: 'm-1', fromDate: '2026-10-01', toDate: '2026-10-31', status: 'COMPLETED',
  totalItems: 10, matchedCount: 7, divergentCount: 3, createdAt: '2026-10-15T13:05:00Z', startedAt: null,
  finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 10, ...overrides,
})

describe('rótulos', () => {
  it('nomeia os status do run com os termos do operador (REC-02)', () => {
    expect(RUN_STATUS_LABELS).toEqual({ PENDING: 'Na fila', RUNNING: 'Em andamento', COMPLETED: 'Concluída', FAILED: 'Falhou' })
  })

  it('nomeia o resultado do item no singular e o filtro no plural (REC-08, REC-09)', () => {
    expect(RESULT_LABELS).toEqual({ MATCHED: 'Bateu', DIVERGENT: 'Divergente' })
    expect(RESULT_FILTER_LABELS).toEqual({ MATCHED: 'Bateram', DIVERGENT: 'Divergentes' })
  })
})

describe('windowDays e newRunWindowError (REC-06)', () => {
  it('conta os dias da janela com os dois extremos', () => {
    expect(MAX_WINDOW_DAYS).toBe(366)
    expect(windowDays({ fromDate: '2026-10-01', toDate: '2026-10-01' })).toBe(1)
    expect(windowDays({ fromDate: '2026-10-01', toDate: '2026-10-31' })).toBe(31)
    expect(windowDays({ fromDate: '2026-03-01', toDate: '2026-03-31' })).toBe(31)
  })

  it('pede as duas datas quando De ou Até está vazio', () => {
    expect(newRunWindowError('', '2026-10-31')).toBe('Informe as duas datas')
    expect(newRunWindowError('2026-10-01', '')).toBe('Informe as duas datas')
    expect(newRunWindowError('', '')).toBe('Informe as duas datas')
  })

  it('recusa De depois de Até com a mensagem de intervalo', () => {
    expect(newRunWindowError('2026-10-02', '2026-10-01')).toBe('A data inicial deve ser igual ou anterior à final')
  })

  it('aceita a janela de um dia', () => {
    expect(newRunWindowError('2026-10-15', '2026-10-15')).toBeNull()
  })

  it('aceita exatamente 366 dias e recusa 367 com a mensagem de limite', () => {
    expect(newRunWindowError('2026-01-01', '2027-01-01')).toBeNull()
    expect(newRunWindowError('2026-01-01', '2027-01-02')).toBe('Janela de conciliação excede o máximo de 366 dias')
  })

  it('aceita um ano bissexto inteiro (366 dias) e recusa um dia a mais', () => {
    expect(windowDays({ fromDate: '2024-01-01', toDate: '2024-12-31' })).toBe(366)
    expect(newRunWindowError('2024-01-01', '2024-12-31')).toBeNull()
    expect(newRunWindowError('2024-01-01', '2025-01-01')).toBe('Janela de conciliação excede o máximo de 366 dias')
  })
})

describe('itemsProgress (REC-03)', () => {
  it('em RUNNING mostra processados de total', () => {
    expect(itemsProgress(run({ status: 'RUNNING', processedItems: 3, totalItems: 10 }))).toBe('3 de 10')
  })

  it('em RUNNING com processedItems nulo mostra 0 de total', () => {
    expect(itemsProgress(run({ status: 'RUNNING', processedItems: null, totalItems: 10 }))).toBe('0 de 10')
  })

  it('em PENDING mostra travessão', () => {
    expect(itemsProgress(run({ status: 'PENDING', processedItems: null, totalItems: null }))).toBe('—')
  })

  it('nos demais status mostra o total', () => {
    expect(itemsProgress(run({ status: 'COMPLETED', processedItems: 4, totalItems: 10 }))).toBe('10')
    expect(itemsProgress(run({ status: 'FAILED', processedItems: 2, totalItems: 5 }))).toBe('5')
  })

  it('sem total conhecido mostra travessão', () => {
    expect(itemsProgress(run({ status: 'FAILED', processedItems: null, totalItems: null }))).toBe('—')
  })
})

describe('discrepancyBadge (REC-08 AC3)', () => {
  it('junta o rótulo do tipo e o do status', () => {
    expect(discrepancyBadge({ type: 'FEE_DIVERGENCE', status: 'OPEN' })).toBe('Taxa divergente · Aberta')
    expect(discrepancyBadge({ type: 'MISSING_SETTLEMENT', status: 'WRITTEN_OFF' })).toBe('Liquidação ausente · Baixada')
  })
})

describe('anyInFlight (REC-04)', () => {
  it('é verdadeiro com algum run PENDING ou RUNNING', () => {
    expect(anyInFlight([run(), run({ status: 'PENDING' })])).toBe(true)
    expect(anyInFlight([run({ status: 'FAILED' }), run({ status: 'RUNNING' })])).toBe(true)
  })

  it('é falso só com runs COMPLETED ou FAILED, ou sem runs', () => {
    expect(anyInFlight([run(), run({ status: 'FAILED' })])).toBe(false)
    expect(anyInFlight([])).toBe(false)
  })
})

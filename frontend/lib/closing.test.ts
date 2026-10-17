import { describe, expect, it } from 'vitest'
import { ApiError, type Period } from './api'
import { CLOSING_PAGE_SIZE, PERIOD_STATUS_LABELS, formatDuration, formatLockedAt, periodStatus, periodWindows } from './closing'

const period = (fields: Partial<Period> = {}): Period => ({
  runId: 'r-1', fromDate: '2026-10-01', toDate: '2026-10-31', totalItems: 4, matchedCount: 2, divergentCount: 2, matchRate: '0.5000',
  openAmount: '0.00', locked: false, lockedAt: null, closeDurationSeconds: null, ...fields,
})

describe('periodWindows', () => {
  it('devolve uma janela por par de datas, mesmo com vários runs da janela (FEC-01, edge de vários runs)', () => {
    const windows = periodWindows([
      { fromDate: '2026-10-01', toDate: '2026-10-31' },
      { fromDate: '2026-09-01', toDate: '2026-09-30' },
      { fromDate: '2026-10-01', toDate: '2026-10-31' },
      { fromDate: '2026-09-01', toDate: '2026-09-30' },
    ])
    expect(windows).toEqual([
      { fromDate: '2026-10-01', toDate: '2026-10-31' },
      { fromDate: '2026-09-01', toDate: '2026-09-30' },
    ])
  })

  it('ordena pela data inicial, mais recente primeiro, e no empate pela final, mais recente primeiro (FEC-01 AC2)', () => {
    const windows = periodWindows([
      { fromDate: '2026-08-01', toDate: '2026-08-31' },
      { fromDate: '2026-10-01', toDate: '2026-10-15' },
      { fromDate: '2026-09-01', toDate: '2026-09-30' },
      { fromDate: '2026-10-01', toDate: '2026-10-31' },
    ])
    expect(windows).toEqual([
      { fromDate: '2026-10-01', toDate: '2026-10-31' },
      { fromDate: '2026-10-01', toDate: '2026-10-15' },
      { fromDate: '2026-09-01', toDate: '2026-09-30' },
      { fromDate: '2026-08-01', toDate: '2026-08-31' },
    ])
  })

  it('sem runs devolve lista vazia (FEC-03 AC2)', () => {
    expect(periodWindows([])).toEqual([])
  })
})

describe('CLOSING_PAGE_SIZE', () => {
  it('é 10 janelas por página (FEC-03 AC1)', () => {
    expect(CLOSING_PAGE_SIZE).toBe(10)
  })
})

describe('formatDuration', () => {
  it.each<[number | null, string]>([
    [29, '29 s'],
    [59, '59 s'],
    [60, '1 min'],
    [180, '3 min'],
    [3599, '59 min'],
    [3600, '1 h'],
    [7200, '2 h'],
    [7500, '2 h 5 min'],
    [86399, '23 h 59 min'],
    [86400, '1 d'],
    [97200, '1 d 3 h'],
    [null, '—'],
  ])('%s segundos viram "%s" (FEC-02 AC4, edge de duração)', (seconds, text) => {
    expect(formatDuration(seconds)).toBe(text)
  })
})

describe('formatLockedAt', () => {
  it('mostra dd/mm/aaaa hh:mm na hora local (FEC-02 AC4)', () => {
    expect(formatLockedAt('2026-09-05T08:07:30')).toBe('05/09/2026 08:07')
  })

  it('sem data mostra "—" (FEC-02 AC4)', () => {
    expect(formatLockedAt(null)).toBe('—')
  })
})

describe('periodStatus', () => {
  it.each<[string, { data?: Period; error: unknown }, ReturnType<typeof periodStatus>]>([
    ['200 travado', { data: period({ locked: true }), error: null }, 'locked'],
    ['200 aberto', { data: period(), error: null }, 'open'],
    ['409', { error: new ApiError('Janela possui conciliação em andamento', 409, 'CONFLICT') }, 'running'],
    ['404', { error: new ApiError('Período não encontrado', 404, 'NOT_FOUND') }, 'none'],
    ['500', { error: new ApiError('A API retornou erro 500.', 500) }, 'error'],
    ['erro de rede', { error: new TypeError('Failed to fetch') }, 'error'],
    ['carregando', { error: null }, 'loading'],
  ])('%s vira %s (FEC-02 AC4–AC6, FEC-03 AC5)', (_case, query, status) => {
    expect(periodStatus(query)).toBe(status)
  })

  it('um erro depois de uma leitura boa vale o erro, não os dados antigos (FEC-02 AC5)', () => {
    expect(periodStatus({ data: period(), error: new ApiError('Janela possui conciliação em andamento', 409, 'CONFLICT') })).toBe('running')
  })
})

describe('PERIOD_STATUS_LABELS', () => {
  it('nomeia os quatro status da spec (FEC-02)', () => {
    expect(PERIOD_STATUS_LABELS).toEqual({
      locked: 'Travado', open: 'Aberto', running: 'Em andamento', none: 'Sem conciliação concluída',
    })
  })
})

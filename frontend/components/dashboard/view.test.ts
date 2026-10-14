import { describe, expect, it } from 'vitest'
import { ApiError, type Period, type PeriodSummary, type Run } from '@/lib/api'
import { deriveDashboardView, type DashboardViewInput } from './view'

const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }

const run = (status: Run['status']): Run => ({
  id: `run-${status}`, merchantId: 'm-1', ...september, status, totalItems: 40, matchedCount: null, divergentCount: null,
  createdAt: '2026-10-13T09:00:00Z', startedAt: null, finishedAt: null, errorMessage: status === 'FAILED' ? 'Falha' : null,
  supersededAt: null, processedItems: null,
})
const period = (locked: boolean): Period => ({
  runId: 'run-COMPLETED', ...september, totalItems: 40, matchedCount: 28, divergentCount: 12, matchRate: '70.00',
  openAmount: '10389.38', locked, lockedAt: locked ? '2026-10-05T10:00:00Z' : null, closeDurationSeconds: null,
})
const summary = { runId: 'run-COMPLETED', grossAmount: '125000.00' } as PeriodSummary

/** Merchant selected and every query settled with data, unless a test overrides a part. */
function input(overrides: Partial<DashboardViewInput> = {}): DashboardViewInput {
  return {
    merchant: { isLoading: false, merchantId: 'm-1' },
    window: september,
    run: { data: run('COMPLETED') },
    period: { data: period(false) },
    summary: { data: summary },
    ...overrides,
  }
}

describe('deriveDashboardView', () => {
  it('loading enquanto a lista de empresas carrega', () => {
    expect(deriveDashboardView(input({ merchant: { isLoading: true, merchantId: null }, run: {}, period: {}, summary: {} })))
      .toEqual({ kind: 'loading' })
  })

  it('loading enquanto o run da janela ainda não chegou', () => {
    expect(deriveDashboardView(input({ run: {}, period: {}, summary: {} }))).toEqual({ kind: 'loading' })
  })

  it('loading enquanto o período do run concluído ainda não chegou', () => {
    expect(deriveDashboardView(input({ period: {}, summary: {} }))).toEqual({ kind: 'loading' })
  })

  it('noMerchant quando o usuário não tem empresa no acesso', () => {
    expect(deriveDashboardView(input({ merchant: { isLoading: false, merchantId: null }, run: {}, period: {}, summary: {} })))
      .toEqual({ kind: 'noMerchant' })
  })

  it('forbidden quando a lista de runs responde 403', () => {
    const forbidden = new ApiError('Sem acesso', 403, 'FORBIDDEN')
    expect(deriveDashboardView(input({ run: { error: forbidden }, period: {}, summary: {} }))).toEqual({ kind: 'forbidden' })
  })

  it('forbidden quando o período responde 403', () => {
    const forbidden = new ApiError('Sem acesso', 403, 'FORBIDDEN')
    expect(deriveDashboardView(input({ period: { error: forbidden }, summary: {} }))).toEqual({ kind: 'forbidden' })
  })

  it('forbidden quando só o resumo responde 403, com run e período carregados (DASH-06 AC6)', () => {
    const forbidden = new ApiError('Sem acesso', 403, 'FORBIDDEN')
    expect(deriveDashboardView(input({ summary: { error: forbidden } }))).toEqual({ kind: 'forbidden' })
  })

  it('empty com a janela quando ela não tem run', () => {
    expect(deriveDashboardView(input({ run: { data: null }, period: {}, summary: {} })))
      .toEqual({ kind: 'empty', window: september })
  })

  it('empty quando /periods responde 404 (janela sem run vigente)', () => {
    const notFound = new ApiError('Nenhum run concluído', 404, 'NOT_FOUND')
    expect(deriveDashboardView(input({ period: { error: notFound }, summary: { error: notFound } })))
      .toEqual({ kind: 'empty', window: september })
  })

  it('running sem dados anteriores enquanto o run é PENDING', () => {
    expect(deriveDashboardView(input({ run: { data: run('PENDING') }, period: {}, summary: {} })))
      .toEqual({ kind: 'running', run: run('PENDING') })
  })

  it('running com os dados anteriores quando /periods responde 409 durante o run', () => {
    const conflict = new ApiError('Run em andamento', 409, 'CONFLICT')
    const view = deriveDashboardView(input({
      run: { data: run('RUNNING') },
      period: { data: period(false), error: conflict },
      summary: { data: summary, error: conflict },
    }))

    expect(view).toEqual({ kind: 'running', run: run('RUNNING'), previous: { period: period(false), summary } })
  })

  it('failed com o run e os dados anteriores quando o último run falhou', () => {
    expect(deriveDashboardView(input({ run: { data: run('FAILED') } })))
      .toEqual({ kind: 'failed', run: run('FAILED'), previous: { period: period(false), summary } })
  })

  it('failed sem dados anteriores quando a janela nunca concluiu um run', () => {
    const notFound = new ApiError('Nenhum run concluído', 404, 'NOT_FOUND')
    expect(deriveDashboardView(input({ run: { data: run('FAILED') }, period: { error: notFound }, summary: { error: notFound } })))
      .toEqual({ kind: 'failed', run: run('FAILED') })
  })

  it('open com período e resumo quando a janela concluída não está travada', () => {
    expect(deriveDashboardView(input())).toEqual({ kind: 'open', data: { period: period(false), summary } })
  })

  it('open mesmo sem o resumo, que tem erro próprio no bloco', () => {
    const failure = new ApiError('Erro interno', 500)
    expect(deriveDashboardView(input({ summary: { error: failure } })))
      .toEqual({ kind: 'open', data: { period: period(false), summary: undefined } })
  })

  it('locked quando period.locked é verdadeiro', () => {
    expect(deriveDashboardView(input({ period: { data: period(true) } })))
      .toEqual({ kind: 'locked', data: { period: period(true), summary } })
  })
})

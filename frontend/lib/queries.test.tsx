import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import { ApiError, type Period, type Run } from './api'
import {
  createQueryClient, useLatestRun, useLockPeriod, usePeriod, usePeriodSummary, useResolveDiscrepancy,
  useRunReconciliation, useUnlockPeriod,
} from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return {
    ...original,
    getRuns: vi.fn(),
    getPeriod: vi.fn(),
    getPeriodSummary: vi.fn(),
    updateDiscrepancy: vi.fn(),
    runReconciliation: vi.fn(),
    lockPeriod: vi.fn(),
    unlockPeriod: vi.fn(),
  }
})

const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }
const august = { fromDate: '2026-08-01', toDate: '2026-08-31' }

let client: QueryClient
let onUnauthorized: ReturnType<typeof vi.fn<() => void>>
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>

const run = (status: Run['status'], window = september): Run => ({
  id: `run-${status}`, merchantId: 'm-1', ...window, status, totalItems: 40, matchedCount: null, divergentCount: null,
  createdAt: '2026-10-13T09:00:00Z', startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: null,
})
const runsPage = (...runs: Run[]) => ({ content: runs, totalPages: 1 })
const period = (merchantId: string, openAmount: string) => ({ runId: `run-${merchantId}`, openAmount }) as unknown as Period

beforeEach(() => {
  onUnauthorized = vi.fn<() => void>()
  client = createQueryClient(onUnauthorized)
  vi.mocked(api.getRuns).mockReset()
  vi.mocked(api.getPeriod).mockReset()
})

afterEach(() => {
  client.clear()
  vi.useRealTimers()
})

describe('useLatestRun', () => {
  it.each(['COMPLETED', 'FAILED'] as const)('refaz o GET a cada 3 s enquanto PENDING/RUNNING e para em %s', async finalStatus => {
    vi.useFakeTimers()
    vi.mocked(api.getRuns)
      .mockResolvedValueOnce(runsPage(run('PENDING')))
      .mockResolvedValueOnce(runsPage(run('RUNNING')))
      .mockResolvedValueOnce(runsPage(run(finalStatus)))
      .mockResolvedValue(runsPage(run(finalStatus)))

    // The query cache notifies through setTimeout(0), which fake timers run 1 ms later: the last 2 ms of each
    // step flush those notifications, so `tick(ms)` still moves the clock by exactly `ms`.
    const tick = (ms: number) => act(async () => {
      await vi.advanceTimersByTimeAsync(ms - 2)
      await vi.advanceTimersByTimeAsync(1)
      await vi.advanceTimersByTimeAsync(1)
    })
    const { result } = renderHook(() => useLatestRun('m-1', september), { wrapper })
    await tick(2)
    expect(api.getRuns).toHaveBeenCalledTimes(1)
    expect(result.current.data?.status).toBe('PENDING')

    await tick(2996) // t = 2998 ms
    expect(api.getRuns).toHaveBeenCalledTimes(1)
    await tick(4) // t = 3002 ms
    expect(api.getRuns).toHaveBeenCalledTimes(2)
    expect(result.current.data?.status).toBe('RUNNING')

    await tick(3000)
    expect(api.getRuns).toHaveBeenCalledTimes(3)
    expect(result.current.data?.status).toBe(finalStatus)

    await tick(15000)
    expect(api.getRuns).toHaveBeenCalledTimes(3)
  })

  it('devolve o run mais recente da janela exata e null quando a janela não tem run', async () => {
    vi.mocked(api.getRuns).mockResolvedValue(runsPage(run('RUNNING', august), run('COMPLETED', september)))
    const { result, rerender } = renderHook(({ window }) => useLatestRun('m-1', window), { wrapper, initialProps: { window: september } })
    await waitFor(() => expect(result.current.data).toMatchObject({ status: 'COMPLETED', fromDate: '2026-09-01' }))

    rerender({ window: { fromDate: '2026-07-01', toDate: '2026-07-31' } })
    await waitFor(() => expect(result.current.data).toBeNull())
  })
})

describe('troca de empresa', () => {
  it('não entrega ao bloco atual a resposta da empresa anterior', async () => {
    let resolveSlowA!: (value: Period) => void
    vi.mocked(api.getPeriod).mockImplementation((_token, merchantId) => merchantId === 'm-a'
      ? new Promise(resolve => { resolveSlowA = resolve })
      : Promise.resolve(period('m-b', '20.00')))

    const { result, rerender } = renderHook(({ merchantId }) => usePeriod(merchantId, september), { wrapper, initialProps: { merchantId: 'm-a' } })
    rerender({ merchantId: 'm-b' })
    await waitFor(() => expect(result.current.data).toEqual(period('m-b', '20.00')))

    resolveSlowA(period('m-a', '10.00'))
    await waitFor(() => expect(client.getQueryData(['period', 'm-a', '2026-09-01', '2026-09-30'])).toEqual(period('m-a', '10.00')))
    expect(result.current.data).toEqual(period('m-b', '20.00'))
  })

  it('começa vazio ao trocar de empresa, sem mostrar o período da anterior', async () => {
    let resolveB!: (value: Period) => void
    vi.mocked(api.getPeriod).mockImplementation((_token, merchantId) => merchantId === 'm-a'
      ? Promise.resolve(period('m-a', '10.00'))
      : new Promise(resolve => { resolveB = resolve }))

    const { result, rerender } = renderHook(({ merchantId }) => usePeriod(merchantId, september), { wrapper, initialProps: { merchantId: 'm-a' } })
    await waitFor(() => expect(result.current.data).toEqual(period('m-a', '10.00')))

    rerender({ merchantId: 'm-b' })
    expect(result.current.data).toBeUndefined()
    resolveB(period('m-b', '20.00'))
    await waitFor(() => expect(result.current.data).toEqual(period('m-b', '20.00')))
  })

  it('mantém o período anterior da mesma empresa enquanto a nova janela carrega', async () => {
    vi.mocked(api.getPeriod).mockImplementation((_token, _merchantId, window) => window.fromDate === september.fromDate
      ? Promise.resolve(period('m-a', '10.00'))
      : new Promise(() => {}))
    vi.mocked(api.getPeriodSummary).mockResolvedValue({ runId: 'run-m-a' } as api.PeriodSummary)

    const { result, rerender } = renderHook(({ window }) => ({ period: usePeriod('m-a', window), summary: usePeriodSummary('m-a', window) }), { wrapper, initialProps: { window: september } })
    await waitFor(() => expect(result.current.summary.data).toEqual({ runId: 'run-m-a' }))
    rerender({ window: august })

    expect(result.current.period.data).toEqual(period('m-a', '10.00'))
    expect(result.current.period.isPlaceholderData).toBe(true)
    expect(result.current.summary.data).toEqual({ runId: 'run-m-a' })
    expect(result.current.summary.isPlaceholderData).toBe(true)
  })
})

describe('mutações', () => {
  const seed = () => {
    const sameWindow = [
      ['period', 'm-1', '2026-09-01', '2026-09-30'],
      ['summary', 'm-1', '2026-09-01', '2026-09-30'],
      ['openDiscrepancies', 'm-1', 'run-1', 'ALL'],
      ['openDiscrepancies', 'm-1', 'run-1', 'FEE_DIVERGENCE'],
      ['discrepancy', 'm-1', 'run-1', 'd-1'],
      ['runs', 'm-1'],
      ['runs', 'm-1', '2026-09-01', '2026-09-30'],
    ]
    const otherMerchant = [
      ['period', 'm-2', '2026-09-01', '2026-09-30'],
      ['summary', 'm-2', '2026-09-01', '2026-09-30'],
      ['openDiscrepancies', 'm-2', 'run-9', 'ALL'],
      ['discrepancy', 'm-2', 'run-9', 'd-9'],
      ['runs', 'm-2'],
    ]
    const otherWindow = [['period', 'm-1', '2026-08-01', '2026-08-31'], ['summary', 'm-1', '2026-08-01', '2026-08-31']]
    for (const key of [...sameWindow, ...otherMerchant, ...otherWindow]) client.setQueryData(key, { seeded: true })
    return { sameWindow, untouched: [...otherMerchant, ...otherWindow] }
  }
  const invalidated = (key: string[]) => client.getQueryState(key)?.isInvalidated

  it.each([
    ['resolver divergência', () => useResolveDiscrepancy('m-1', september), { runId: 'run-1', discrepancyId: 'd-1', update: { status: 'ACCEPTED' as const } }, api.updateDiscrepancy],
    ['conciliar período', () => useRunReconciliation('m-1', september), undefined, api.runReconciliation],
    ['travar período', () => useLockPeriod('m-1', september), undefined, api.lockPeriod],
    ['destravar período', () => useUnlockPeriod('m-1', september), undefined, api.unlockPeriod],
  ] as Array<[string, () => { mutateAsync: (input?: never) => Promise<unknown> }, unknown, (...args: never[]) => unknown]>)('%s invalida período, resumo, abertas, detalhe e runs da mesma chave', async (_name, hook, variables, endpoint) => {
    vi.mocked(endpoint as typeof api.lockPeriod).mockResolvedValue({} as never)
    const { sameWindow, untouched } = seed()
    const { result } = renderHook(hook, { wrapper })

    await act(() => result.current.mutateAsync(variables as never))

    expect(sameWindow.map(invalidated)).toEqual(sameWindow.map(() => true))
    expect(untouched.map(invalidated)).toEqual(untouched.map(() => false))
  })

  it('não invalida nada quando a mutação falha', async () => {
    vi.mocked(api.updateDiscrepancy).mockRejectedValue(new ApiError('Esta divergência mudou', 409, 'CONFLICT'))
    const { sameWindow } = seed()
    const { result } = renderHook(() => useResolveDiscrepancy('m-1', september), { wrapper })

    await act(() => result.current.mutateAsync({ runId: 'run-1', discrepancyId: 'd-1', update: { status: 'OPEN' } }).catch(() => {}))

    expect(sameWindow.map(invalidated)).toEqual(sameWindow.map(() => false))
  })
})

describe('401 global', () => {
  it('encerra a sessão quando uma query responde 401', async () => {
    vi.mocked(api.getPeriod).mockRejectedValue(new ApiError('Token expirado', 401, 'UNAUTHORIZED'))
    renderHook(() => usePeriod('m-1', september), { wrapper })
    await waitFor(() => expect(onUnauthorized).toHaveBeenCalledTimes(1))
  })

  it('encerra a sessão quando uma mutação responde 401', async () => {
    vi.mocked(api.lockPeriod).mockRejectedValue(new ApiError('Token expirado', 401, 'UNAUTHORIZED'))
    const { result } = renderHook(() => useLockPeriod('m-1', september), { wrapper })
    await act(() => result.current.mutateAsync().catch(() => {}))
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it.each([[403, 'FORBIDDEN'], [404, 'NOT_FOUND'], [409, 'CONFLICT']])('não encerra a sessão nem repete o GET em %i', async (status, code) => {
    vi.mocked(api.getPeriod).mockRejectedValue(new ApiError('Erro', status, code))
    const { result } = renderHook(() => usePeriod('m-1', september), { wrapper })
    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error).toMatchObject({ status, code })
    expect(api.getPeriod).toHaveBeenCalledTimes(1)
    expect(onUnauthorized).not.toHaveBeenCalled()
  })
})

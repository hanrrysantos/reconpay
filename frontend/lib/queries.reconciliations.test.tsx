import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import { ApiError, type Page, type Period, type PeriodSummary, type ReconciliationItem, type Run } from './api'
import {
  RUN_POLL_MS, createQueryClient, queryKeys, useLatestRun, usePeriod, usePeriodSummary, useRun, useRunItems, useRunPage,
  useStartReconciliation,
} from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return {
    ...original,
    getRuns: vi.fn(),
    getRun: vi.fn(),
    getRunItems: vi.fn(),
    runReconciliation: vi.fn(),
    getPeriod: vi.fn(),
    getPeriodSummary: vi.fn(),
  }
})

const october = { fromDate: '2026-10-01', toDate: '2026-10-31' }
const firstHalf = { fromDate: '2026-10-01', toDate: '2026-10-15' }
const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }

const run = (id: string, status: Run['status'], window = october): Run => ({
  id, merchantId: 'm-1', ...window, status, totalItems: 10, matchedCount: null, divergentCount: null,
  createdAt: '2026-10-15T13:05:00Z', startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: null,
})
const page = <T,>(...content: T[]): Page<T> => ({ content, totalPages: 1 })
const item = (id: string) => ({ id, externalReference: id }) as unknown as ReconciliationItem

let client: QueryClient
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>

// The query cache notifies through setTimeout(0), which fake timers run 1 ms later: the last 2 ms of each
// step flush those notifications, so `tick(ms)` still moves the clock by exactly `ms`.
const tick = (ms: number) => act(async () => {
  await vi.advanceTimersByTimeAsync(ms - 2)
  await vi.advanceTimersByTimeAsync(1)
  await vi.advanceTimersByTimeAsync(1)
})

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getRuns).mockReset()
  vi.mocked(api.getRun).mockReset()
  vi.mocked(api.getRunItems).mockReset()
  vi.mocked(api.runReconciliation).mockReset()
  vi.mocked(api.getPeriod).mockReset()
  vi.mocked(api.getPeriodSummary).mockReset()
})

afterEach(() => {
  client.clear()
  vi.useRealTimers()
})

describe('useRunPage', () => {
  it('consulta a empresa, a janela e a página pedidas; sem janela, não envia janela (REC-02 AC1, REC-05 AC7)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue(page(run('r-1', 'COMPLETED')))
    const { result, rerender } = renderHook(({ window, n }) => useRunPage('m-1', window, n), {
      wrapper, initialProps: { window: september as typeof september | null, n: 2 },
    })
    await waitFor(() => expect(result.current.data).toEqual(page(run('r-1', 'COMPLETED'))))
    expect(api.getRuns).toHaveBeenLastCalledWith('jwt', 'm-1', { page: 2, window: september })

    rerender({ window: null, n: 0 })
    await waitFor(() => expect(api.getRuns).toHaveBeenCalledTimes(2))
    expect(vi.mocked(api.getRuns).mock.calls[1][2]).toStrictEqual({ page: 0 })
  })

  it('recarrega a cada RUN_POLL_MS com um run em andamento e para quando todos terminam (REC-04 AC6)', async () => {
    vi.useFakeTimers()
    vi.mocked(api.getRuns)
      .mockResolvedValueOnce(page(run('r-2', 'PENDING'), run('r-1', 'COMPLETED')))
      .mockResolvedValueOnce(page(run('r-2', 'RUNNING'), run('r-1', 'COMPLETED')))
      .mockResolvedValue(page(run('r-2', 'FAILED'), run('r-1', 'COMPLETED')))
    const { result } = renderHook(() => useRunPage('m-1', null, 0), { wrapper })

    await tick(2)
    expect(api.getRuns).toHaveBeenCalledTimes(1)
    expect(RUN_POLL_MS).toBe(3000)
    await tick(2996) // t = 2998 ms
    expect(api.getRuns).toHaveBeenCalledTimes(1)
    await tick(4) // t = 3002 ms
    expect(api.getRuns).toHaveBeenCalledTimes(2)
    expect(result.current.data?.content[0].status).toBe('RUNNING')

    await tick(3000)
    expect(api.getRuns).toHaveBeenCalledTimes(3)
    expect(result.current.data?.content[0].status).toBe('FAILED')
    await tick(15000)
    expect(api.getRuns).toHaveBeenCalledTimes(3)
  })

  it('não recarrega sozinha quando nenhum run da página está em andamento (REC-04 AC6)', async () => {
    vi.useFakeTimers()
    vi.mocked(api.getRuns).mockResolvedValue(page(run('r-1', 'COMPLETED'), run('r-0', 'FAILED')))
    renderHook(() => useRunPage('m-1', null, 0), { wrapper })

    await tick(2)
    await tick(15000)
    expect(api.getRuns).toHaveBeenCalledTimes(1)
  })
})

describe('useRun', () => {
  it('busca o run pelo id e recarrega enquanto está em andamento (REC-09 AC8)', async () => {
    vi.useFakeTimers()
    vi.mocked(api.getRun)
      .mockResolvedValueOnce(run('r-1', 'PENDING'))
      .mockResolvedValueOnce(run('r-1', 'RUNNING'))
      .mockResolvedValue(run('r-1', 'COMPLETED'))
    const { result } = renderHook(() => useRun('m-1', 'r-1'), { wrapper })

    await tick(2)
    expect(api.getRun).toHaveBeenCalledWith('jwt', 'm-1', 'r-1')
    await tick(2996)
    expect(api.getRun).toHaveBeenCalledTimes(1)
    await tick(4)
    expect(api.getRun).toHaveBeenCalledTimes(2)
    await tick(3000)
    expect(api.getRun).toHaveBeenCalledTimes(3)
    expect(result.current.data?.status).toBe('COMPLETED')
    await tick(15000)
    expect(api.getRun).toHaveBeenCalledTimes(3)
  })

  it('entrega o 404 sem repetir o GET (REC-09 AC9)', async () => {
    vi.mocked(api.getRun).mockRejectedValue(new ApiError('Conciliação não encontrada com id: r-9', 404, 'NOT_FOUND'))
    const { result } = renderHook(() => useRun('m-1', 'r-9'), { wrapper })

    await waitFor(() => expect(result.current.error).toMatchObject({ status: 404 }))
    expect(api.getRun).toHaveBeenCalledTimes(1)
  })
})

describe('useRunItems', () => {
  it('consulta a empresa, o run, os filtros e a página pedidos (REC-09 AC4, AC5)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue(page(item('i-1')))
    const filters = { result: 'DIVERGENT' as const, discrepancyType: 'FEE_DIVERGENCE' as const }
    const { result, rerender } = renderHook(({ n }) => useRunItems('m-1', 'r-1', filters, n, false), { wrapper, initialProps: { n: 1 } })

    await waitFor(() => expect(result.current.data).toEqual(page(item('i-1'))))
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', filters, 1)
    rerender({ n: 2 })
    await waitFor(() => expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', filters, 2))
  })

  it('com polling recarrega a cada RUN_POLL_MS; sem polling não (REC-09 AC8)', async () => {
    vi.useFakeTimers()
    vi.mocked(api.getRunItems).mockResolvedValue(page(item('i-1')))
    const { rerender } = renderHook(({ polling }) => useRunItems('m-1', 'r-1', {}, 0, polling), { wrapper, initialProps: { polling: true } })

    await tick(2)
    expect(api.getRunItems).toHaveBeenCalledTimes(1)
    await tick(3000)
    expect(api.getRunItems).toHaveBeenCalledTimes(2)

    rerender({ polling: false })
    await tick(15000)
    expect(api.getRunItems).toHaveBeenCalledTimes(2)
  })
})

describe('useStartReconciliation', () => {
  /** Answers after a real delay, so a refetch the mutation does not await would still be pending when it resolves. */
  const later = <T,>(value: T) => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))

  function mountDashboardAndList() {
    let version = 'antes'
    vi.mocked(api.getRuns).mockImplementation(async (_token, _merchant, options) =>
      later(page(run(`r-${version}${options?.window ? '-janela' : ''}`, 'COMPLETED', firstHalf))))
    vi.mocked(api.getPeriod).mockImplementation(() => later({ runId: `r-${version}` } as Period))
    vi.mocked(api.getPeriodSummary).mockImplementation(() => later({ runId: `r-${version}` } as PeriodSummary))
    const hook = renderHook(() => ({
      list: useRunPage('m-1', null, 0),
      latest: useLatestRun('m-1', firstHalf),
      period: usePeriod('m-1', firstHalf),
      summary: usePeriodSummary('m-1', firstHalf),
      otherPeriod: usePeriod('m-1', september),
      start: useStartReconciliation('m-1'),
    }), { wrapper })
    return { ...hook, bump: () => { version = 'depois' } }
  }

  it('envia a janela e, quando aceita, a lista e a janela do dashboard já foram recarregadas (REC-07 AC4, AC8)', async () => {
    const { result, bump } = mountDashboardAndList()
    await waitFor(() => expect(result.current.summary.data).toEqual({ runId: 'r-antes' }))
    await waitFor(() => expect(result.current.list.data?.content[0].id).toBe('r-antes'))
    await waitFor(() => expect(result.current.otherPeriod.data).toEqual({ runId: 'r-antes' }))
    client.setQueryData(queryKeys.openDiscrepancies('m-1', 'r-antes'), page())
    client.setQueryData(queryKeys.discrepancy('m-1', 'r-antes', 'd-1'), {})

    const accepted = run('r-depois', 'PENDING', firstHalf)
    vi.mocked(api.runReconciliation).mockResolvedValue(accepted)
    bump()
    await act(async () => { await expect(result.current.start.mutateAsync(firstHalf)).resolves.toEqual(accepted) })

    expect(api.runReconciliation).toHaveBeenCalledWith('jwt', 'm-1', firstHalf)
    expect(client.getQueryData(queryKeys.runPage('m-1', null, 0))).toEqual(page(run('r-depois', 'COMPLETED', firstHalf)))
    expect(client.getQueryData(queryKeys.latestRun('m-1', firstHalf))).toMatchObject({ id: 'r-depois-janela' })
    expect(client.getQueryData(queryKeys.period('m-1', firstHalf))).toEqual({ runId: 'r-depois' })
    expect(client.getQueryData(queryKeys.summary('m-1', firstHalf))).toEqual({ runId: 'r-depois' })
    expect(client.getQueryState(queryKeys.openDiscrepancies('m-1', 'r-antes'))?.isInvalidated).toBe(true)
    expect(client.getQueryState(queryKeys.discrepancy('m-1', 'r-antes', 'd-1'))?.isInvalidated).toBe(true)
    expect(client.getQueryState(queryKeys.period('m-1', september))?.isInvalidated).toBe(false)
  })

  it('na recusa da API, nada é invalidado (REC-07 AC7)', async () => {
    const { result } = mountDashboardAndList()
    await waitFor(() => expect(result.current.summary.data).toEqual({ runId: 'r-antes' }))
    await waitFor(() => expect(result.current.list.data?.content[0].id).toBe('r-antes'))
    await waitFor(() => expect(result.current.latest.data).toBeTruthy())
    await waitFor(() => expect(result.current.period.data).toEqual({ runId: 'r-antes' }))
    vi.mocked(api.runReconciliation).mockRejectedValue(new ApiError('Já existe uma conciliação em andamento', 409, 'CONFLICT'))

    await act(async () => { await result.current.start.mutateAsync(firstHalf).catch(() => {}) })

    const keys = [
      queryKeys.runPage('m-1', null, 0), queryKeys.latestRun('m-1', firstHalf), queryKeys.period('m-1', firstHalf), queryKeys.summary('m-1', firstHalf),
    ]
    expect(keys.map(key => client.getQueryState(key)?.isInvalidated)).toEqual([false, false, false, false])
  })
})

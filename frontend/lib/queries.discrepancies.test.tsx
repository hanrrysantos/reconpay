import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import type { Page, ReconciliationItem, RunItemFilters } from './api'
import { createQueryClient, queryKeys, useResolveDiscrepancy, useRunItems } from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => ({ ...await importOriginal<typeof import('./api')>(), getRunItems: vi.fn(), updateDiscrepancy: vi.fn() }))

const october = { fromDate: '2026-10-01', toDate: '2026-10-31' }
const item = (id: string, status: ReconciliationItem['discrepancies'][number]['status']): ReconciliationItem => ({
  id, reconciliationRunId: 'r-1', externalReference: `REF-${id}`, result: 'DIVERGENT', internalTransactionId: 't-1', externalSettlementId: null,
  transactionAmount: '100.00', expectedNetAmount: '95.00', settlementAmount: null, settlementNetAmount: null, netDifference: '-95.00',
  paymentMethod: 'CREDIT_CARD', installments: 1, transactionStatus: 'APPROVED', settlementStatus: null, transactionDate: '2026-10-03',
  settlementDate: null, discrepancies: [{ id: `d-${id}`, status, type: 'MISSING_SETTLEMENT', expectedValue: null, actualValue: null }],
  createdAt: '2026-10-15T12:05:00Z',
})
const page = (content: ReconciliationItem[]): Page<ReconciliationItem> => ({ content, totalPages: 1 })

let client: QueryClient
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>
/** Answers after a real delay, so a refetch the mutation does not await would still be pending when it resolves. */
const later = <T,>(value: T) => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getRunItems).mockReset()
  vi.mocked(api.updateDiscrepancy).mockReset()
})
afterEach(() => client.clear())

describe('queryKeys.runItems', () => {
  it('separa as páginas por status da divergência (DIV-02)', () => {
    const open: RunItemFilters = { discrepancyStatus: 'OPEN' }
    const accepted: RunItemFilters = { discrepancyStatus: 'ACCEPTED' }
    expect(queryKeys.runItems('m-1', 'r-1', open, 0)).not.toEqual(queryKeys.runItems('m-1', 'r-1', accepted, 0))
    expect(queryKeys.runItems('m-1', 'r-1', {}, 0)).not.toEqual(queryKeys.runItems('m-1', 'r-1', open, 0))
  })
})

describe('useRunItems', () => {
  it('trocar o status consulta a API de novo com o status novo (DIV-02)', async () => {
    vi.mocked(api.getRunItems).mockImplementation((_token, _merchant, _run, filters) =>
      later(page([item('1', filters.discrepancyStatus ?? 'OPEN')])))
    const { result, rerender } = renderHook(({ filters }) => useRunItems('m-1', 'r-1', filters, 0, false), {
      wrapper, initialProps: { filters: { discrepancyStatus: 'OPEN' } as RunItemFilters },
    })
    await waitFor(() => expect(result.current.data?.content[0].discrepancies[0].status).toBe('OPEN'))

    rerender({ filters: { discrepancyStatus: 'ACCEPTED' } })

    await waitFor(() => expect(result.current.data?.content[0].discrepancies[0].status).toBe('ACCEPTED'))
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', { discrepancyStatus: 'ACCEPTED' }, 0)
  })
})

describe('useResolveDiscrepancy', () => {
  it('quando a resolução é aceita, a página de itens da empresa já foi recarregada (DIV-07)', async () => {
    let stored = [item('1', 'OPEN')]
    vi.mocked(api.getRunItems).mockImplementation(() => later(page(stored)))
    const { result } = renderHook(() => ({
      items: useRunItems('m-1', 'r-1', { discrepancyStatus: 'OPEN' }, 0, false),
      resolve: useResolveDiscrepancy('m-1', october),
    }), { wrapper })
    await waitFor(() => expect(result.current.items.data?.content).toHaveLength(1))

    vi.mocked(api.updateDiscrepancy).mockImplementation(async () => {
      stored = []
      return {} as Awaited<ReturnType<typeof api.updateDiscrepancy>>
    })
    await act(() => result.current.resolve.mutateAsync({ runId: 'r-1', discrepancyId: 'd-1', update: { status: 'ACCEPTED' } }))

    expect(client.getQueryData(queryKeys.runItems('m-1', 'r-1', { discrepancyStatus: 'OPEN' }, 0))).toEqual(page([]))
  })

  it('não recarrega itens de outra empresa', async () => {
    vi.mocked(api.getRunItems).mockImplementation(() => later(page([item('1', 'OPEN')])))
    const { result } = renderHook(() => ({
      other: useRunItems('m-2', 'r-9', { discrepancyStatus: 'OPEN' }, 0, false),
      resolve: useResolveDiscrepancy('m-1', october),
    }), { wrapper })
    await waitFor(() => expect(result.current.other.data).toBeDefined())
    const calls = vi.mocked(api.getRunItems).mock.calls.length

    vi.mocked(api.updateDiscrepancy).mockResolvedValue({} as Awaited<ReturnType<typeof api.updateDiscrepancy>>)
    await act(() => result.current.resolve.mutateAsync({ runId: 'r-1', discrepancyId: 'd-1', update: { status: 'ACCEPTED' } }))

    expect(api.getRunItems).toHaveBeenCalledTimes(calls)
  })
})

import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import type { Page, Transaction, TransactionFilters } from './api'
import { createQueryClient, queryKeys, useCreateTransaction, useTransactionPage, useUpdateTransactionStatus } from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return { ...original, getTransactionPage: vi.fn(), createTransaction: vi.fn(), updateTransactionStatus: vi.fn() }
})

const october: TransactionFilters = { window: { fromDate: '2026-10-01', toDate: '2026-10-31' } }
const transaction = (id: string, externalReference: string, status: Transaction['status'] = 'APPROVED'): Transaction => ({
  id, merchantId: 'm-1', externalReference, amount: '300.00', expectedNetAmount: '290.73', paymentMethod: 'CREDIT_CARD', installments: 3,
  status, transactionDate: '2026-10-03', createdAt: '2026-10-03T12:00:00Z', updatedAt: '2026-10-03T12:00:00Z',
})
const first = transaction('t-1', 'VENDA-1')
const added = transaction('t-2', 'AVULSA-1')
const page = (content: Transaction[]): Page<Transaction> => ({ content, totalPages: 1 })

let client: QueryClient
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>

/** Answers after a real delay, so a refetch the mutation does not await would still be pending when it resolves. */
const later = <T,>(value: T) => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))
function serve(byMerchant: Record<string, Transaction[]>) {
  vi.mocked(api.getTransactionPage).mockImplementation((_token, merchantId) => later(page(byMerchant[merchantId] ?? [])))
}

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getTransactionPage).mockReset()
  vi.mocked(api.createTransaction).mockReset()
  vi.mocked(api.updateTransactionStatus).mockReset()
})
afterEach(() => client.clear())

describe('useTransactionPage', () => {
  it('consulta a empresa, os filtros e a página pedidos (TXN-01)', async () => {
    serve({ 'm-1': [first] })
    const filters: TransactionFilters = { ...october, status: 'CANCELLED', paymentMethod: 'PIX' }
    const { result } = renderHook(() => useTransactionPage('m-1', filters, 2), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(page([first])))
    expect(api.getTransactionPage).toHaveBeenCalledWith('jwt', 'm-1', filters, 2)
  })

  it('filtros ou página diferentes são consultas diferentes', async () => {
    serve({ 'm-1': [first] })
    const { rerender } = renderHook(({ filters, n }) => useTransactionPage('m-1', filters, n), {
      wrapper, initialProps: { filters: october, n: 0 },
    })
    await waitFor(() => expect(api.getTransactionPage).toHaveBeenCalledTimes(1))

    rerender({ filters: { ...october, status: 'REFUNDED' }, n: 0 })
    await waitFor(() => expect(api.getTransactionPage).toHaveBeenLastCalledWith('jwt', 'm-1', { ...october, status: 'REFUNDED' }, 0))
    rerender({ filters: { ...october, status: 'REFUNDED' }, n: 1 })
    await waitFor(() => expect(api.getTransactionPage).toHaveBeenLastCalledWith('jwt', 'm-1', { ...october, status: 'REFUNDED' }, 1))
    expect(api.getTransactionPage).toHaveBeenCalledTimes(3)
  })
})

describe('useCreateTransaction', () => {
  const input = { externalReference: 'AVULSA-1', amount: '300.00', paymentMethod: 'CREDIT_CARD' as const, installments: 3, transactionDate: '2026-10-03' }

  it('quando o cadastro é aceito, a lista da empresa já foi recarregada; a de outra empresa não (TXN-07 AC6)', async () => {
    serve({ 'm-1': [first], 'm-2': [] })
    const { result } = renderHook(() => ({
      list: useTransactionPage('m-1', october, 0),
      other: useTransactionPage('m-2', october, 0),
      create: useCreateTransaction('m-1'),
    }), { wrapper })
    await waitFor(() => expect(result.current.list.data).toEqual(page([first])))
    await waitFor(() => expect(result.current.other.data).toEqual(page([])))

    vi.mocked(api.createTransaction).mockResolvedValue(added)
    serve({ 'm-1': [added, first], 'm-2': [] })
    await act(async () => { await expect(result.current.create.mutateAsync(input)).resolves.toEqual(added) })

    expect(api.createTransaction).toHaveBeenCalledWith('jwt', 'm-1', input)
    expect(client.getQueryData(queryKeys.transactions('m-1', october, 0))).toEqual(page([added, first]))
    expect(client.getQueryState(queryKeys.transactions('m-2', october, 0))?.isInvalidated).toBe(false)
  })
})

describe('useUpdateTransactionStatus', () => {
  it('quando a mudança é aceita, a lista da empresa já foi recarregada (TXN-09 AC6)', async () => {
    serve({ 'm-1': [first] })
    const { result } = renderHook(() => ({
      list: useTransactionPage('m-1', october, 0),
      update: useUpdateTransactionStatus('m-1'),
    }), { wrapper })
    await waitFor(() => expect(result.current.list.data).toEqual(page([first])))

    const refunded = transaction('t-1', 'VENDA-1', 'REFUNDED')
    vi.mocked(api.updateTransactionStatus).mockResolvedValue(refunded)
    serve({ 'm-1': [refunded] })
    await act(async () => { await result.current.update.mutateAsync({ transactionId: 't-1', status: 'REFUNDED' }) })

    expect(api.updateTransactionStatus).toHaveBeenCalledWith('jwt', 'm-1', 't-1', 'REFUNDED')
    expect(client.getQueryData(queryKeys.transactions('m-1', october, 0))).toEqual(page([refunded]))
  })
})

import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SelectedMerchantProvider, useSelectedMerchant } from '@/components/shell/MerchantMenu'
import * as api from './api'
import type { Merchant, MerchantRecord } from './api'
import { createQueryClient, useCreateMerchant, useDeactivateMerchant, useMerchantPage, useRenameMerchant } from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return {
    ...original,
    getMerchants: vi.fn(),
    getMerchantPage: vi.fn(),
    createMerchant: vi.fn(),
    renameMerchant: vi.fn(),
    deactivateMerchant: vi.fn(),
  }
})

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11111111000191' }
const record = (merchant: Merchant): MerchantRecord => ({ ...merchant, active: true, createdAt: '2026-10-01T12:00:00Z' })
const page = (...merchants: Merchant[]) => ({ content: merchants.map(record), totalPages: 1, totalElements: merchants.length })

let client: QueryClient
/** Cache state the moment the mutation resolves; the rendered hooks catch up on React Query's next notification. */
const menuCache = () => client.getQueryData(['merchants', 'u-1'])
const pageCache = () => client.getQueryData(['merchantPage', 0])
const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={client}><SelectedMerchantProvider>{children}</SelectedMerchantProvider></QueryClientProvider>
)

/**
 * The menu and the page list answer with `merchants` from the next request on, after a real delay: a mutation that
 * resolved before the refetch finished would leave the old lists in the cache.
 */
const later = <T,>(value: T) => () => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))
function serve(merchants: Merchant[]) {
  vi.mocked(api.getMerchants).mockImplementation(later(merchants))
  vi.mocked(api.getMerchantPage).mockImplementation(later(page(...merchants)))
}

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getMerchants).mockReset()
  vi.mocked(api.getMerchantPage).mockReset()
})

afterEach(() => client.clear())

describe('useMerchantPage', () => {
  it('busca a página pedida', async () => {
    serve([alfa])
    const { result } = renderHook(() => useMerchantPage(2), { wrapper })
    await waitFor(() => expect(result.current.data).toEqual(page(alfa)))
    expect(api.getMerchantPage).toHaveBeenCalledWith('jwt', 2)
  })
})

describe('mutações de empresa', () => {
  it('ao cadastrar, o menu já contém a nova empresa e ela é a selecionada quando a mutação resolve', async () => {
    serve([alfa])
    const { result } = renderHook(() => ({ selected: useSelectedMerchant(), list: useMerchantPage(0), create: useCreateMerchant() }), { wrapper })
    await waitFor(() => expect(result.current.selected.merchantId).toBe('m-1'))
    await waitFor(() => expect(result.current.list.data).toEqual(page(alfa)))

    vi.mocked(api.createMerchant).mockResolvedValue(record(beta))
    serve([alfa, beta])
    await act(() => result.current.create.mutateAsync({ name: 'Beta Comércio', document: '11111111000191' }))

    expect(api.createMerchant).toHaveBeenCalledWith('jwt', { name: 'Beta Comércio', document: '11111111000191' })
    expect(menuCache()).toEqual([alfa, beta])
    expect(pageCache()).toEqual(page(alfa, beta))
    expect(result.current.selected.merchants).toEqual([alfa, beta])
    expect(result.current.selected.merchantId).toBe('m-2')
    expect(result.current.list.data).toEqual(page(alfa, beta))
  })

  it('ao renomear, refaz a lista da página e a do menu', async () => {
    serve([alfa, beta])
    const { result } = renderHook(() => ({ selected: useSelectedMerchant(), list: useMerchantPage(0), rename: useRenameMerchant() }), { wrapper })
    await waitFor(() => expect(result.current.list.data).toEqual(page(alfa, beta)))
    await waitFor(() => expect(result.current.selected.merchants).toEqual([alfa, beta]))

    const renamed = { ...alfa, name: 'Alfa Matriz' }
    vi.mocked(api.renameMerchant).mockResolvedValue(record(renamed))
    serve([renamed, beta])
    await act(() => result.current.rename.mutateAsync({ merchantId: 'm-1', name: 'Alfa Matriz' }))

    expect(api.renameMerchant).toHaveBeenCalledWith('jwt', 'm-1', 'Alfa Matriz')
    expect(menuCache()).toEqual([renamed, beta])
    expect(pageCache()).toEqual(page(renamed, beta))
    await waitFor(() => expect(result.current.selected.merchants).toEqual([renamed, beta]))
    expect(result.current.list.data).toEqual(page(renamed, beta))
  })

  it('ao desativar a selecionada, refaz as duas listas e o menu cai na primeira restante', async () => {
    serve([alfa, beta])
    const { result } = renderHook(() => ({ selected: useSelectedMerchant(), list: useMerchantPage(0), deactivate: useDeactivateMerchant() }), { wrapper })
    await waitFor(() => expect(result.current.selected.merchants).toEqual([alfa, beta]))
    await waitFor(() => expect(result.current.list.data).toEqual(page(alfa, beta)))
    act(() => result.current.selected.select('m-2'))
    expect(result.current.selected.merchantId).toBe('m-2')

    vi.mocked(api.deactivateMerchant).mockResolvedValue(undefined)
    serve([alfa])
    await act(() => result.current.deactivate.mutateAsync('m-2'))

    expect(api.deactivateMerchant).toHaveBeenCalledWith('jwt', 'm-2')
    expect(menuCache()).toEqual([alfa])
    expect(pageCache()).toEqual(page(alfa))
    await waitFor(() => expect(result.current.selected.merchants).toEqual([alfa]))
    expect(result.current.selected.merchantId).toBe('m-1')
    expect(result.current.list.data).toEqual(page(alfa))
  })

  it('ao desativar a última empresa, o menu fica sem empresa selecionada', async () => {
    serve([alfa])
    const { result } = renderHook(() => ({ selected: useSelectedMerchant(), deactivate: useDeactivateMerchant() }), { wrapper })
    await waitFor(() => expect(result.current.selected.merchantId).toBe('m-1'))

    vi.mocked(api.deactivateMerchant).mockResolvedValue(undefined)
    serve([])
    await act(() => result.current.deactivate.mutateAsync('m-1'))

    expect(menuCache()).toEqual([])
    await waitFor(() => expect(result.current.selected.merchants).toEqual([]))
    expect(result.current.selected.merchantId).toBeNull()
    expect(result.current.selected.merchant).toBeNull()
  })
})

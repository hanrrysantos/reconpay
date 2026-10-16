import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import type { FeeRule } from './api'
import { createQueryClient, useCreateFeeRule, useDeactivateFeeRule, useFeeRules, useUpdateFeeRuleFees } from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return { ...original, getFeeRules: vi.fn(), createFeeRule: vi.fn(), updateFeeRuleFees: vi.fn(), deactivateFeeRule: vi.fn() }
})

const rule = (id: string, merchantId: string, overrides: Partial<FeeRule> = {}): FeeRule => ({
  id, merchantId, merchantName: merchantId === 'm-1' ? 'Alfa' : 'Beta', paymentMethod: 'PIX', installments: 1,
  feePercentage: '1.0000', fixedFee: '0.00', active: true, createdAt: '2026-10-01T12:00:00Z', ...overrides,
})
const pix = rule('r-1', 'm-1')
const credit = rule('r-2', 'm-1', { paymentMethod: 'CREDIT_CARD', installments: 3, feePercentage: '2.9900', fixedFee: '0.30' })
const betaRule = rule('r-9', 'm-2', { paymentMethod: 'BOLETO' })

let client: QueryClient
const cache = (merchantId: string) => client.getQueryData(['feeRules', merchantId])
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>

/** Each merchant answers with its rules from the next request on, after a real delay, so an unawaited refetch shows. */
const later = <T,>(value: T) => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))
function serve(rules: Record<string, FeeRule[]>) {
  vi.mocked(api.getFeeRules).mockImplementation((_token, merchantId) => later(rules[merchantId] ?? []))
}

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getFeeRules).mockReset()
})
afterEach(() => client.clear())

describe('useFeeRules', () => {
  it('ao trocar de empresa busca a lista da nova e não devolve as regras da anterior (FLIST-04)', async () => {
    serve({ 'm-1': [pix], 'm-2': [betaRule] })
    const { result, rerender } = renderHook(({ merchantId }) => useFeeRules(merchantId), { wrapper, initialProps: { merchantId: 'm-1' } })
    await waitFor(() => expect(result.current.data).toEqual([pix]))

    rerender({ merchantId: 'm-2' })
    expect(result.current.data).toBeUndefined()
    await waitFor(() => expect(result.current.data).toEqual([betaRule]))
    expect(api.getFeeRules).toHaveBeenLastCalledWith('jwt', 'm-2')
  })
})

describe('mutações de regra de taxa', () => {
  it('ao cadastrar, a lista da empresa já foi refeita quando a mutação resolve (FCAD-05)', async () => {
    serve({ 'm-1': [pix] })
    const { result } = renderHook(() => ({ list: useFeeRules('m-1'), create: useCreateFeeRule('m-1') }), { wrapper })
    await waitFor(() => expect(result.current.list.data).toEqual([pix]))

    vi.mocked(api.createFeeRule).mockResolvedValue(credit)
    serve({ 'm-1': [credit, pix] })
    const input = { paymentMethod: 'CREDIT_CARD' as const, installments: 3, feePercentage: '2.99', fixedFee: '0.30' }
    await act(() => result.current.create.mutateAsync(input))

    expect(api.createFeeRule).toHaveBeenCalledWith('jwt', 'm-1', input)
    expect(cache('m-1')).toEqual([credit, pix])
  })

  it('ao alterar taxas, a lista da empresa já foi refeita quando a mutação resolve (FEDIT-03)', async () => {
    serve({ 'm-1': [credit] })
    const { result } = renderHook(() => ({ list: useFeeRules('m-1'), update: useUpdateFeeRuleFees('m-1') }), { wrapper })
    await waitFor(() => expect(result.current.list.data).toEqual([credit]))

    const changed = { ...credit, feePercentage: '3.4900' }
    vi.mocked(api.updateFeeRuleFees).mockResolvedValue(changed)
    serve({ 'm-1': [changed] })
    await act(() => result.current.update.mutateAsync({ ruleId: 'r-2', fees: { feePercentage: '3.49', fixedFee: '0.30' } }))

    expect(api.updateFeeRuleFees).toHaveBeenCalledWith('jwt', 'm-1', 'r-2', { feePercentage: '3.49', fixedFee: '0.30' })
    expect(cache('m-1')).toEqual([changed])
  })

  it('ao desativar, a lista da empresa já foi refeita quando a mutação resolve (FDES-03)', async () => {
    serve({ 'm-1': [credit, pix] })
    const { result } = renderHook(() => ({ list: useFeeRules('m-1'), deactivate: useDeactivateFeeRule('m-1') }), { wrapper })
    await waitFor(() => expect(result.current.list.data).toEqual([credit, pix]))

    vi.mocked(api.deactivateFeeRule).mockResolvedValue(undefined)
    serve({ 'm-1': [credit] })
    await act(() => result.current.deactivate.mutateAsync('r-1'))

    expect(api.deactivateFeeRule).toHaveBeenCalledWith('jwt', 'm-1', 'r-1')
    expect(cache('m-1')).toEqual([credit])
  })
})

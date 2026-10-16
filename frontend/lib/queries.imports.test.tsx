import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from './api'
import type { ImportBatch, ImportKind, Page } from './api'
import { createQueryClient, useImportBatches, useImportCsv, useLatestImports } from './queries'

vi.mock('./auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('./api', async importOriginal => {
  const original = await importOriginal<typeof import('./api')>()
  return {
    ...original,
    getImportBatches: vi.fn(), importCsv: vi.fn(), getLatestSettlementImport: vi.fn(), getLatestBankStatementImportTime: vi.fn(),
  }
})

const batch = (id: string, fileName: string, createdAt: string): ImportBatch => ({ id, merchantId: 'm-1', fileName, totalRows: 2, createdAt })
const older = batch('i-1', 'liquidacao-set.csv', '2026-10-01T12:00:00Z')
const newer = batch('i-2', 'liquidacao-out.csv', '2026-10-16T13:05:00Z')
const page = (content: ImportBatch[]): Page<ImportBatch> => ({ content, totalPages: 1 })

let client: QueryClient
const wrapper = ({ children }: { children: ReactNode }) => <QueryClientProvider client={client}>{children}</QueryClientProvider>

/** Answers after a real delay, so a refetch the mutation does not await would still be pending when it resolves. */
const later = <T,>(value: T) => new Promise<T>(resolve => setTimeout(() => resolve(value), 20))
function serveBatches(batches: Record<string, ImportBatch[]>) {
  vi.mocked(api.getImportBatches).mockImplementation((_token, merchantId, kind) => later(page(batches[`${merchantId}:${kind}`] ?? [])))
}
function serveLatest(settlement: ImportBatch | null) {
  vi.mocked(api.getLatestSettlementImport).mockImplementation(() => later(settlement))
  vi.mocked(api.getLatestBankStatementImportTime).mockImplementation(() => later(null))
}

beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getImportBatches).mockReset()
  vi.mocked(api.importCsv).mockReset()
})
afterEach(() => client.clear())

describe('useImportBatches', () => {
  it('consulta a página pedida dos lotes do tipo e da empresa (IMP-10)', async () => {
    serveBatches({ 'm-1:settlements': [older] })
    const { result } = renderHook(() => useImportBatches('m-1', 'settlements', 2), { wrapper })

    await waitFor(() => expect(result.current.data).toEqual(page([older])))
    expect(api.getImportBatches).toHaveBeenCalledWith('jwt', 'm-1', 'settlements', 2)
  })
})

describe('useImportCsv', () => {
  const file = new File(['x'], 'liquidacao-out.csv')

  it('quando o import é aceito, a lista do tipo e os últimos imports do dashboard já foram refeitos (IMP-12)', async () => {
    serveBatches({ 'm-1:settlements': [older] })
    serveLatest(older)
    const { result } = renderHook(() => ({
      batches: useImportBatches('m-1', 'settlements', 0),
      latest: useLatestImports('m-1'),
      send: useImportCsv('m-1', 'settlements'),
    }), { wrapper })
    await waitFor(() => expect(result.current.batches.data).toEqual(page([older])))
    await waitFor(() => expect(result.current.latest.data?.settlementImportedAt).toBe(older.createdAt))

    vi.mocked(api.importCsv).mockResolvedValue(newer)
    serveBatches({ 'm-1:settlements': [newer, older] })
    serveLatest(newer)
    await act(() => result.current.send.mutateAsync(file))

    expect(api.importCsv).toHaveBeenCalledWith('jwt', 'm-1', 'settlements', file)
    expect(client.getQueryData(['importBatches', 'm-1', 'settlements', 0])).toEqual(page([newer, older]))
    expect(client.getQueryData<{ settlementImportedAt: string | null }>(['imports', 'm-1'])?.settlementImportedAt).toBe(newer.createdAt)
  })

  it.each<ImportKind>(['transactions', 'bankStatements'])('envia o arquivo de %s para o tipo do hook (IMP-03)', async kind => {
    vi.mocked(api.importCsv).mockResolvedValue(newer)
    const { result } = renderHook(() => useImportCsv('m-1', kind), { wrapper })
    await act(() => result.current.mutateAsync(file))
    expect(api.importCsv).toHaveBeenCalledWith('jwt', 'm-1', kind, file)
  })

  it('quando o arquivo é recusado, não refaz nenhuma lista', async () => {
    serveBatches({ 'm-1:settlements': [older] })
    serveLatest(older)
    const { result } = renderHook(() => ({
      batches: useImportBatches('m-1', 'settlements', 0),
      latest: useLatestImports('m-1'),
      send: useImportCsv('m-1', 'settlements'),
    }), { wrapper })
    await waitFor(() => expect(result.current.batches.data).toEqual(page([older])))
    await waitFor(() => expect(result.current.latest.data).toBeDefined())
    const batchCalls = vi.mocked(api.getImportBatches).mock.calls.length
    const latestCalls = vi.mocked(api.getLatestSettlementImport).mock.calls.length

    vi.mocked(api.importCsv).mockRejectedValue(new api.ApiError('Erro na importação do CSV', 400, 'VALIDATION_ERROR'))
    await act(() => result.current.send.mutateAsync(file).catch(() => undefined))

    expect(api.getImportBatches).toHaveBeenCalledTimes(batchCalls)
    expect(api.getLatestSettlementImport).toHaveBeenCalledTimes(latestCalls)
    expect(client.getQueryState(['importBatches', 'm-1', 'settlements', 0])?.isInvalidated).toBe(false)
  })
})

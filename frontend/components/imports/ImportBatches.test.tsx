import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { ImportBatch, ImportKind } from '@/lib/api'
import { formatImportedAt } from '@/lib/imports'
import { createQueryClient } from '@/lib/queries'
import { ImportBatches } from './ImportBatches'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), getImportBatches: vi.fn() }))

const batch = (id: string, fileName: string, totalRows: number, createdAt: string): ImportBatch => ({ id, merchantId: 'm-1', fileName, totalRows, createdAt })
const october = batch('i-2', 'liquidacao-out.csv', 120, '2026-10-16T13:05:00Z')
const september = batch('i-1', 'liquidacao-set.csv', 7, '2026-09-30T23:59:00Z')

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getImportBatches).mockReset()
})
afterEach(() => client.clear())

function show(props: { kind?: ImportKind; noun?: string; page?: number; onPageChange?: (page: number) => void } = {}) {
  const onPageChange = props.onPageChange ?? vi.fn()
  render(
    <QueryClientProvider client={client}>
      <ImportBatches merchantId="m-1" kind={props.kind ?? 'settlements'} noun={props.noun ?? 'liquidação'} page={props.page ?? 0} onPageChange={onPageChange} />
    </QueryClientProvider>,
  )
  return onPageChange
}

const rowsOf = (table: HTMLElement) => within(table).getAllByRole('row').slice(1).map(row => within(row).getAllByRole('cell').map(cell => cell.textContent))

describe('ImportBatches', () => {
  it('lista arquivo, linhas e data de cada lote na ordem da API (IMP-10)', async () => {
    vi.mocked(api.getImportBatches).mockResolvedValue({ content: [october, september], totalPages: 1 })
    show()

    const table = await screen.findByRole('table', { name: 'Lotes importados' })
    expect(within(table).getAllByRole('columnheader').map(th => th.textContent)).toEqual(['Arquivo', 'Linhas', 'Importado em'])
    expect(rowsOf(table)).toEqual([
      ['liquidacao-out.csv', '120', formatImportedAt(october.createdAt)],
      ['liquidacao-set.csv', '7', formatImportedAt(september.createdAt)],
    ])
    expect(rowsOf(table)[0][2]).toMatch(/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/)
    expect(api.getImportBatches).toHaveBeenCalledWith('jwt', 'm-1', 'settlements', 0)
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).toBeNull()
  })

  it('consulta a página recebida e navega para a vizinha (IMP-10)', async () => {
    vi.mocked(api.getImportBatches).mockResolvedValue({ content: [september], totalPages: 3 })
    const onPageChange = show({ page: 1 })

    const nav = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(api.getImportBatches).toHaveBeenCalledWith('jwt', 'm-1', 'settlements', 1)
    expect(within(nav).getByText('Página 2 de 3')).toBeInTheDocument()
    await userEvent.click(within(nav).getByRole('button', { name: 'Anterior' }))
    expect(onPageChange).toHaveBeenLastCalledWith(0)
    await userEvent.click(within(nav).getByRole('button', { name: 'Próxima' }))
    expect(onPageChange).toHaveBeenLastCalledWith(2)
  })

  it('desabilita Anterior na primeira página e Próxima na última (IMP-10)', async () => {
    vi.mocked(api.getImportBatches).mockResolvedValue({ content: [october], totalPages: 2 })
    show({ page: 0 })
    const first = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(within(first).getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(within(first).getByRole('button', { name: 'Próxima' })).toBeEnabled()

    client.clear()
    document.body.innerHTML = ''
    show({ page: 1 })
    const last = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(within(last).getByRole('button', { name: 'Anterior' })).toBeEnabled()
    expect(within(last).getByRole('button', { name: 'Próxima' })).toBeDisabled()
  })

  it.each<[ImportKind, string]>([
    ['transactions', 'transações'],
    ['settlements', 'liquidação'],
    ['bankStatements', 'extrato'],
  ])('sem lotes de %s mostra o estado vazio do tipo (IMP-11)', async (kind, noun) => {
    vi.mocked(api.getImportBatches).mockResolvedValue({ content: [], totalPages: 0 })
    show({ kind, noun })

    expect(await screen.findByText(`Nenhuma importação de ${noun} ainda.`)).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('mostra o erro da listagem e Tentar novamente refaz a consulta (IMP-11)', async () => {
    vi.mocked(api.getImportBatches)
      .mockRejectedValueOnce(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
      .mockResolvedValueOnce({ content: [october], totalPages: 1 })
    show()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Você não tem acesso a este comerciante')
    await userEvent.click(within(alert).getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('table', { name: 'Lotes importados' })).toHaveTextContent('liquidacao-out.csv')
    expect(api.getImportBatches).toHaveBeenCalledTimes(2)
  })
})

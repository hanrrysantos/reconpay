import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Run } from '@/lib/api'
import { formatImportedAt } from '@/lib/imports'
import type { Window } from '@/lib/period'
import { createQueryClient } from '@/lib/queries'
import { RunList } from './RunList'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), getRuns: vi.fn() }))

const run = (id: string, fields: Partial<Run> = {}): Run => ({
  id, merchantId: 'm-1', fromDate: '2026-10-01', toDate: '2026-10-31', status: 'COMPLETED',
  totalItems: 3, matchedCount: 2, divergentCount: 1, createdAt: '2026-10-15T12:05:00Z', startedAt: null,
  finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 3, ...fields,
})

const completed = run('r-1')
const running = run('r-2', { status: 'RUNNING', processedItems: 3, totalItems: 10, matchedCount: null, divergentCount: null })
const pending = run('r-3', {
  status: 'PENDING', fromDate: '2026-10-01', toDate: '2026-10-15', processedItems: null, totalItems: null, matchedCount: null, divergentCount: null,
})
const superseded = run('r-4', { supersededAt: '2026-10-15T13:00:00Z' })
const failed = run('r-5', { status: 'FAILED', errorMessage: 'Falha ao processar o lote', totalItems: null, matchedCount: null, divergentCount: null })

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getRuns).mockReset()
})
afterEach(() => {
  client.clear()
  vi.useRealTimers()
})

function show({ page = 0, window = null }: { page?: number; window?: Window | null } = {}) {
  const onPageChange = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <RunList merchantId="m-1" window={window} page={page} onPageChange={onPageChange} />
    </QueryClientProvider>,
  )
  return { onPageChange }
}

const rowsOf = (table: HTMLElement) => within(table).getAllByRole('row').slice(1)
const cellsOf = (row: HTMLElement) => within(row).getAllByRole('cell').map(cell => cell.textContent)

describe('RunList', () => {
  it('mostra as 7 colunas e a linha concluída nos formatos da spec, com o link Ver itens (REC-02, REC-05 AC11)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [completed], totalPages: 1 })
    show()

    const table = await screen.findByRole('table', { name: 'Conciliações' })
    expect(within(table).getAllByRole('columnheader').map(th => th.textContent))
      .toEqual(['Janela', 'Status', 'Itens', 'Conciliados', 'Divergentes', 'Criado em', 'Ação'])
    const [row] = rowsOf(table)
    const cells = cellsOf(row)
    expect(cells.slice(0, 5)).toEqual(['Outubro de 2026', 'Concluída', '3', '2', '1'])
    expect(cells[5]).toMatch(/^15\/10\/2026 \d{2}:\d{2}$/)
    expect(cells[5]).toBe(formatImportedAt(completed.createdAt))
    expect(within(row).getByRole('link', { name: 'Ver itens' })).toHaveAttribute('href', '/conciliacoes/r-1')
    expect(api.getRuns).toHaveBeenCalledWith('jwt', 'm-1', { page: 0 })
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).toBeNull()
  })

  it('mostra o progresso em RUNNING, travessão em PENDING, o selo Substituído e a mensagem da falha (REC-03)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [running, pending, superseded, failed], totalPages: 1 })
    show()

    const [runningRow, pendingRow, supersededRow, failedRow] = rowsOf(await screen.findByRole('table', { name: 'Conciliações' }))
    expect(cellsOf(runningRow).slice(1, 5)).toEqual(['Em andamento', '3 de 10', '—', '—'])
    expect(cellsOf(pendingRow).slice(0, 3)).toEqual(['01/10/2026 – 15/10/2026', 'Na fila', '—'])
    expect(within(supersededRow).getByText('Substituído')).toBeInTheDocument()
    expect(cellsOf(supersededRow)[1]).toBe('ConcluídaSubstituído')
    expect(within(runningRow).queryByText('Substituído')).toBeNull()
    expect(cellsOf(failedRow)[1]).toBe('FalhouFalha ao processar o lote')
    expect(within(failedRow).getByText('Falha ao processar o lote')).toBeInTheDocument()
  })

  it('RUNNING com processedItems nulo mostra 0 de total (REC-03 edge)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [run('r-6', { status: 'RUNNING', processedItems: null, totalItems: 40 })], totalPages: 1 })
    show()

    const [row] = rowsOf(await screen.findByRole('table', { name: 'Conciliações' }))
    expect(cellsOf(row)[2]).toBe('0 de 40')
  })

  it('consulta a janela recebida (REC-05 AC7)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [completed], totalPages: 1 })
    const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }
    show({ window: september })

    await screen.findByRole('table', { name: 'Conciliações' })
    expect(api.getRuns).toHaveBeenCalledWith('jwt', 'm-1', { page: 0, window: september })
  })

  it('recarrega a cada 3 s enquanto há run em andamento e para quando todos terminam (REC-04 AC6)', async () => {
    vi.useFakeTimers()
    vi.mocked(api.getRuns)
      .mockResolvedValueOnce({ content: [pending, completed], totalPages: 1 })
      .mockResolvedValueOnce({ content: [{ ...pending, status: 'RUNNING', processedItems: 5, totalItems: 8 }, completed], totalPages: 1 })
      .mockResolvedValue({ content: [{ ...pending, status: 'COMPLETED', processedItems: 8, totalItems: 8, matchedCount: 6, divergentCount: 2 }, completed], totalPages: 1 })
    const tick = (ms: number) => act(async () => {
      await vi.advanceTimersByTimeAsync(ms - 2)
      await vi.advanceTimersByTimeAsync(1)
      await vi.advanceTimersByTimeAsync(1)
    })
    show()

    await tick(2)
    const firstRow = () => rowsOf(screen.getByRole('table', { name: 'Conciliações' }))[0]
    expect(cellsOf(firstRow())[1]).toBe('Na fila')
    await tick(2996)
    expect(api.getRuns).toHaveBeenCalledTimes(1)
    await tick(4)
    expect(api.getRuns).toHaveBeenCalledTimes(2)
    expect(cellsOf(firstRow()).slice(1, 3)).toEqual(['Em andamento', '5 de 8'])
    await tick(3000)
    expect(api.getRuns).toHaveBeenCalledTimes(3)
    expect(cellsOf(firstRow()).slice(1, 5)).toEqual(['Concluída', '8', '6', '2'])
    await tick(15000)
    expect(api.getRuns).toHaveBeenCalledTimes(3)
  })

  it('consulta a página recebida e navega para a vizinha (REC-05 AC8)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [completed], totalPages: 3 })
    const { onPageChange } = show({ page: 1 })

    const nav = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(api.getRuns).toHaveBeenCalledWith('jwt', 'm-1', { page: 1 })
    expect(within(nav).getByText('Página 2 de 3')).toBeInTheDocument()
    await userEvent.click(within(nav).getByRole('button', { name: 'Anterior' }))
    expect(onPageChange).toHaveBeenLastCalledWith(0)
    await userEvent.click(within(nav).getByRole('button', { name: 'Próxima' }))
    expect(onPageChange).toHaveBeenLastCalledWith(2)
  })

  it('desabilita Anterior na primeira página e Próxima na última (REC-05 AC8)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [completed], totalPages: 2 })
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

  it('sem runs mostra Nenhuma conciliação encontrada. (REC-05 AC9)', async () => {
    vi.mocked(api.getRuns).mockResolvedValue({ content: [], totalPages: 0 })
    show()

    expect(await screen.findByText('Nenhuma conciliação encontrada.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('mostra o erro da busca e Tentar novamente refaz a consulta (REC-05 AC10)', async () => {
    vi.mocked(api.getRuns)
      .mockRejectedValueOnce(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
      .mockResolvedValueOnce({ content: [completed], totalPages: 1 })
    show()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Você não tem acesso a este comerciante')
    await userEvent.click(within(alert).getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('table', { name: 'Conciliações' })).toHaveTextContent('Outubro de 2026')
    expect(api.getRuns).toHaveBeenCalledTimes(2)
  })

  it('mostra a mensagem do cliente quando a resposta de erro não é JSON (edge)', async () => {
    vi.mocked(api.getRuns).mockRejectedValue(new api.ApiError('A API retornou erro 502.', 502))
    show()

    expect(await screen.findByRole('alert', {}, { timeout: 3000 })).toHaveTextContent('A API retornou erro 502.')
  })
})

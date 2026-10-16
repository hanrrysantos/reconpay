import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Transaction, TransactionFilters } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { TransactionList } from './TransactionList'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), getTransactionPage: vi.fn() }))

const transaction = (id: string, fields: Partial<Transaction> = {}): Transaction => ({
  id,
  merchantId: 'm-1',
  externalReference: `REF-${id}`,
  amount: '300.00',
  expectedNetAmount: '290.73',
  paymentMethod: 'CREDIT_CARD',
  installments: 3,
  status: 'APPROVED',
  transactionDate: '2026-10-03',
  createdAt: '2026-10-03T12:00:00Z',
  updatedAt: '2026-10-03T12:00:00Z',
  ...fields,
})

const approved = transaction('t-1', { externalReference: 'VENDA-001' })
const refunded = transaction('t-2', {
  externalReference: 'VENDA-002', amount: '1234.5', expectedNetAmount: '1200.07', paymentMethod: 'PIX', installments: 1,
  status: 'REFUNDED', transactionDate: '2026-10-01',
})
const cancelled = transaction('t-3', { status: 'CANCELLED', paymentMethod: 'BOLETO', installments: 1 })
const chargeback = transaction('t-4', { status: 'CHARGEBACK', paymentMethod: 'DEBIT_CARD', installments: 1 })

const filters: TransactionFilters = { window: { fromDate: '2026-10-01', toDate: '2026-10-31' }, status: 'APPROVED' }

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getTransactionPage).mockReset()
})
afterEach(() => client.clear())

function show(page = 0) {
  const onPageChange = vi.fn()
  const onChangeStatus = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <TransactionList merchantId="m-1" filters={filters} page={page} onPageChange={onPageChange} onChangeStatus={onChangeStatus} />
    </QueryClientProvider>,
  )
  return { onPageChange, onChangeStatus }
}

const rowsOf = (table: HTMLElement) => within(table).getAllByRole('row').slice(1)
const cellsOf = (row: HTMLElement) => within(row).getAllByRole('cell').map(cell => cell.textContent)

describe('TransactionList', () => {
  it('mostra as colunas da spec com os formatos de data, método, parcelas, dinheiro e status (TXN-01)', async () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved, refunded], totalPages: 1 })
    show()

    const table = await screen.findByRole('table', { name: 'Transações' })
    expect(within(table).getAllByRole('columnheader').map(th => th.textContent).slice(0, 7))
      .toEqual(['Data', 'Referência', 'Método', 'Parcelas', 'Bruto', 'Líquido esperado', 'Status'])
    const [first, second] = rowsOf(table)
    expect(cellsOf(first).slice(0, 7)).toEqual(['03/10/2026', 'VENDA-001', 'Crédito', '3x', 'R$ 300,00', 'R$ 290,73', 'Aprovada'])
    expect(cellsOf(second).slice(0, 7)).toEqual(['01/10/2026', 'VENDA-002', 'Pix', '1x', 'R$ 1.234,50', 'R$ 1.200,07', 'Estornada'])
    expect(api.getTransactionPage).toHaveBeenCalledWith('jwt', 'm-1', filters, 0)
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).toBeNull()
  })

  it('mostra Alterar status só nas aprovadas e entrega a transação ao clicar (TXN-08)', async () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved, refunded, cancelled, chargeback], totalPages: 1 })
    const { onChangeStatus } = show()

    const rows = rowsOf(await screen.findByRole('table', { name: 'Transações' }))
    expect(rows.map(row => within(row).queryByRole('button', { name: 'Alterar status' }) !== null)).toEqual([true, false, false, false])
    expect(cellsOf(rows[2])[6]).toBe('Cancelada')
    expect(cellsOf(rows[3])[6]).toBe('Chargeback')

    await userEvent.click(within(rows[0]).getByRole('button', { name: 'Alterar status' }))
    expect(onChangeStatus).toHaveBeenCalledWith(approved)
  })

  it('consulta a página recebida e navega para a vizinha (TXN-04)', async () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved], totalPages: 3 })
    const { onPageChange } = show(1)

    const nav = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(api.getTransactionPage).toHaveBeenCalledWith('jwt', 'm-1', filters, 1)
    expect(within(nav).getByText('Página 2 de 3')).toBeInTheDocument()
    await userEvent.click(within(nav).getByRole('button', { name: 'Anterior' }))
    expect(onPageChange).toHaveBeenLastCalledWith(0)
    await userEvent.click(within(nav).getByRole('button', { name: 'Próxima' }))
    expect(onPageChange).toHaveBeenLastCalledWith(2)
  })

  it('desabilita Anterior na primeira página e Próxima na última (TXN-04)', async () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved], totalPages: 2 })
    show(0)
    const first = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(within(first).getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(within(first).getByRole('button', { name: 'Próxima' })).toBeEnabled()

    client.clear()
    document.body.innerHTML = ''
    show(1)
    const last = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(within(last).getByRole('button', { name: 'Anterior' })).toBeEnabled()
    expect(within(last).getByRole('button', { name: 'Próxima' })).toBeDisabled()
  })

  it('sem transações mostra o estado vazio (TXN-04)', async () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [], totalPages: 0 })
    show()

    expect(await screen.findByText('Nenhuma transação encontrada para os filtros escolhidos.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('mostra o erro da busca e Tentar novamente refaz a consulta (TXN-04)', async () => {
    vi.mocked(api.getTransactionPage)
      .mockRejectedValueOnce(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
      .mockResolvedValueOnce({ content: [approved], totalPages: 1 })
    show()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Você não tem acesso a este comerciante')
    await userEvent.click(within(alert).getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('table', { name: 'Transações' })).toHaveTextContent('VENDA-001')
    expect(api.getTransactionPage).toHaveBeenCalledTimes(2)
  })
})

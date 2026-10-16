import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Transaction, TransactionFilters, TransactionStatus } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { StatusDialog } from './StatusDialog'
import { TransactionList } from './TransactionList'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({
  ...await importOriginal<typeof import('@/lib/api')>(),
  updateTransactionStatus: vi.fn(),
  getTransactionPage: vi.fn(),
}))

const approved: Transaction = {
  id: 't-1', merchantId: 'm-1', externalReference: 'VENDA-001', amount: '300.00', expectedNetAmount: '290.73',
  paymentMethod: 'CREDIT_CARD', installments: 3, status: 'APPROVED', transactionDate: '2026-10-03',
  createdAt: '2026-10-03T12:00:00Z', updatedAt: '2026-10-03T12:00:00Z',
}
const filters: TransactionFilters = { window: { fromDate: '2026-10-01', toDate: '2026-10-31' } }

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.updateTransactionStatus).mockReset()
  vi.mocked(api.getTransactionPage).mockReset()
})
afterEach(() => client.clear())

/** The dialog over the merchant's list, so a reload shows up as a new list request. */
function show() {
  const onClose = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <TransactionList merchantId="m-1" filters={filters} page={0} onPageChange={vi.fn()} onChangeStatus={vi.fn()} />
      <StatusDialog merchantId="m-1" transaction={approved} onClose={onClose} />
    </QueryClientProvider>,
  )
  return onClose
}

const dialog = () => screen.getByRole('alertdialog', { name: 'Alterar status' })
const confirmButton = () => within(dialog()).getByRole('button', { name: /^Confirm/ })

describe('StatusDialog', () => {
  it('cita a referência, avisa que não pode ser desfeita e oferece Cancelada, Estornada e Chargeback (TXN-08)', () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved], totalPages: 1 })
    show()

    expect(dialog()).toHaveAccessibleDescription('Transação VENDA-001. A mudança não pode ser desfeita.')
    const options = within(within(dialog()).getByRole('radiogroup', { name: 'Novo status' })).getAllByRole('radio')
    expect(options.map(option => option.closest('label')?.textContent)).toEqual(['Cancelada', 'Estornada', 'Chargeback'])
    expect(options.every(option => !(option as HTMLInputElement).checked)).toBe(true)
  })

  it('mantém Confirmar desabilitado sem opção escolhida (TXN-08)', async () => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved], totalPages: 1 })
    show()

    expect(confirmButton()).toHaveTextContent('Confirmar')
    expect(confirmButton()).toBeDisabled()
    await userEvent.click(confirmButton())
    expect(api.updateTransactionStatus).not.toHaveBeenCalled()

    await userEvent.click(within(dialog()).getByRole('radio', { name: 'Estornada' }))
    expect(confirmButton()).toBeEnabled()
  })

  it.each<[string, TransactionStatus]>([
    ['Cancelada', 'CANCELLED'],
    ['Estornada', 'REFUNDED'],
    ['Chargeback', 'CHARGEBACK'],
  ])('%s envia %s, mostra Confirmando… durante o envio e fecha no sucesso (TXN-09)', async (label, status) => {
    vi.mocked(api.getTransactionPage).mockResolvedValue({ content: [approved], totalPages: 1 })
    let resolve!: (transaction: Transaction) => void
    vi.mocked(api.updateTransactionStatus).mockReturnValue(new Promise(done => { resolve = done }))
    const onClose = show()

    await userEvent.click(within(dialog()).getByRole('radio', { name: label }))
    await userEvent.click(confirmButton())

    expect(api.updateTransactionStatus).toHaveBeenCalledWith('jwt', 'm-1', 't-1', status)
    expect(confirmButton()).toHaveTextContent('Confirmando…')
    expect(confirmButton()).toBeDisabled()
    await userEvent.click(confirmButton())
    expect(api.updateTransactionStatus).toHaveBeenCalledTimes(1)
    expect(onClose).not.toHaveBeenCalled()

    resolve({ ...approved, status })
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
  })

  it.each([
    ['400 de transição inválida', new api.ApiError('Transição de status inválida: REFUNDED -> CANCELLED', 400, 'BAD_REQUEST')],
    ['409 de período travado', new api.ApiError('Janela travada de 2026-10-01 a 2026-10-31', 409, 'CONFLICT')],
  ])('%s mantém o diálogo aberto com a mensagem e recarrega a lista (TXN-09)', async (_case, error) => {
    vi.mocked(api.getTransactionPage)
      .mockResolvedValueOnce({ content: [approved], totalPages: 1 })
      .mockResolvedValue({ content: [{ ...approved, status: 'REFUNDED' }], totalPages: 1 })
    vi.mocked(api.updateTransactionStatus).mockRejectedValue(error)
    const onClose = show()
    const table = await screen.findByRole('table', { name: 'Transações' })
    expect(table).toHaveTextContent('Aprovada')

    await userEvent.click(within(dialog()).getByRole('radio', { name: 'Cancelada' }))
    await userEvent.click(confirmButton())

    expect(await within(dialog()).findByRole('alert')).toHaveTextContent(error.message)
    await waitFor(() => expect(table).toHaveTextContent('Estornada'))
    expect(api.getTransactionPage).toHaveBeenCalledTimes(2)
    expect(onClose).not.toHaveBeenCalled()
    expect(within(dialog()).getByRole('radio', { name: 'Cancelada' })).toBeChecked()
    expect(confirmButton()).toHaveTextContent('Confirmar')
    expect(confirmButton()).toBeEnabled()
  })
})

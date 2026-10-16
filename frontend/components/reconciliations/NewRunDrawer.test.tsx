import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Run } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { NewRunDrawer } from './NewRunDrawer'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), runReconciliation: vi.fn() }))

const accepted = (fromDate: string, toDate: string): Run => ({
  id: 'r-9', merchantId: 'm-1', fromDate, toDate, status: 'PENDING', totalItems: null, matchedCount: null, divergentCount: null,
  createdAt: '2026-10-16T13:30:00Z', startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: null,
})

let client: QueryClient
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 16, 10, 30))
  client = createQueryClient(vi.fn())
  vi.mocked(api.runReconciliation).mockReset()
})
afterEach(() => {
  client.clear()
  vi.useRealTimers()
})

function show() {
  const onClose = vi.fn()
  const onStarted = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <NewRunDrawer merchantId="m-1" onClose={onClose} onStarted={onStarted} />
    </QueryClientProvider>,
  )
  return { onClose, onStarted }
}

const month = () => screen.getByLabelText<HTMLSelectElement>('Mês')
const from = () => screen.getByLabelText<HTMLInputElement>('De')
const to = () => screen.getByLabelText<HTMLInputElement>('Até')
const selectedMonth = () => month().selectedOptions[0].textContent
const setDates = (fromDate: string, toDate: string) => {
  fireEvent.change(from(), { target: { value: fromDate } })
  fireEvent.change(to(), { target: { value: toDate } })
}
const submit = () => userEvent.click(screen.getByRole('button', { name: /^Conciliar/ }))

describe('NewRunDrawer', () => {
  it('abre no mês atual com De e Até no primeiro e no último dia, entre os 12 meses e Personalizado (REC-06 AC1)', () => {
    show()

    expect(screen.getByRole('dialog', { name: 'Nova conciliação' })).toBeInTheDocument()
    expect(selectedMonth()).toBe('Outubro de 2026')
    expect(from()).toHaveValue('2026-10-01')
    expect(to()).toHaveValue('2026-10-31')
    const options = within(month()).getAllByRole('option').map(option => option.textContent)
    expect(options).toHaveLength(13)
    expect(options[0]).toBe('Outubro de 2026')
    expect(options[11]).toBe('Novembro de 2025')
    expect(options[12]).toBe('Personalizado')
  })

  it('escolher um mês preenche De e Até com o mês inteiro (REC-06 AC2)', async () => {
    show()

    await userEvent.selectOptions(month(), 'Fevereiro de 2026')

    expect(from()).toHaveValue('2026-02-01')
    expect(to()).toHaveValue('2026-02-28')
  })

  it.each([['De', from], ['Até', to]] as const)('editar %s muda Mês para Personalizado e mantém as datas', (_name, input) => {
    show()

    fireEvent.change(input(), { target: { value: '2026-10-15' } })

    expect(selectedMonth()).toBe('Personalizado')
    expect(input()).toHaveValue('2026-10-15')
  })

  it.each([
    ['De vazio', '', '2026-10-15', 'Informe as duas datas'],
    ['Até vazio', '2026-10-01', '', 'Informe as duas datas'],
    ['De depois de Até', '2026-10-16', '2026-10-15', 'A data inicial deve ser igual ou anterior à final'],
    ['367 dias', '2026-01-01', '2027-01-02', 'Janela de conciliação excede o máximo de 366 dias'],
  ])('%s mostra a mensagem e não chama a API (REC-06 AC3)', async (_name, fromDate, toDate, message) => {
    const { onStarted } = show()
    setDates(fromDate, toDate)

    await submit()

    expect(screen.getByRole('alert')).toHaveTextContent(message)
    expect(api.runReconciliation).not.toHaveBeenCalled()
    expect(onStarted).not.toHaveBeenCalled()
  })

  it.each([
    ['366 dias', '2026-01-01', '2027-01-01'],
    ['um dia', '2026-10-15', '2026-10-15'],
  ])('envia a janela de %s (REC-06 edges)', async (_name, fromDate, toDate) => {
    vi.mocked(api.runReconciliation).mockResolvedValue(accepted(fromDate, toDate))
    const { onStarted } = show()
    setDates(fromDate, toDate)

    await submit()

    await waitFor(() => expect(onStarted).toHaveBeenCalledWith(accepted(fromDate, toDate)))
    expect(api.runReconciliation).toHaveBeenCalledWith('jwt', 'm-1', { fromDate, toDate })
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('envia {fromDate, toDate}, mostra Conciliando… desabilitado e entrega o run aceito (REC-07 AC4, AC5, AC6)', async () => {
    let resolve!: (run: Run) => void
    vi.mocked(api.runReconciliation).mockReturnValue(new Promise(done => { resolve = done }))
    const { onStarted } = show()
    setDates('2026-10-01', '2026-10-15')

    await submit()

    const sending = await screen.findByRole('button', { name: 'Conciliando…' })
    expect(sending).toBeDisabled()
    expect(api.runReconciliation).toHaveBeenCalledTimes(1)
    expect(api.runReconciliation).toHaveBeenCalledWith('jwt', 'm-1', { fromDate: '2026-10-01', toDate: '2026-10-15' })
    expect(onStarted).not.toHaveBeenCalled()

    resolve(accepted('2026-10-01', '2026-10-15'))
    await waitFor(() => expect(onStarted).toHaveBeenCalledWith(accepted('2026-10-01', '2026-10-15')))
  })

  it.each([
    [409, 'CONFLICT', 'Já existe uma conciliação em andamento para esta janela'],
    [409, 'CONFLICT', 'Período travado para a janela informada'],
    [400, 'VALIDATION_ERROR', 'Janela de conciliação excede o máximo de 366 dias'],
  ])('%i mantém a gaveta aberta com a message da API (REC-07 AC7)', async (status, code, message) => {
    vi.mocked(api.runReconciliation).mockRejectedValue(new api.ApiError(message, status, code))
    const { onStarted, onClose } = show()

    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(screen.getByRole('dialog', { name: 'Nova conciliação' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Conciliar' })).toBeEnabled()
    expect(onStarted).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })
})

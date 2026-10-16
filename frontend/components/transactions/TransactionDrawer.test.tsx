import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Transaction, TransactionInput } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { TransactionDrawer } from './TransactionDrawer'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), createTransaction: vi.fn() }))

const TODAY = '2026-10-16'
const MISSING_RULE = 'Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas'

const created: Transaction = {
  id: 't-9', merchantId: 'm-1', externalReference: 'AVULSA-1', amount: '100.00', expectedNetAmount: '96.51',
  paymentMethod: 'CREDIT_CARD', installments: 3, status: 'APPROVED', transactionDate: TODAY,
  createdAt: '2026-10-16T13:00:00Z', updatedAt: '2026-10-16T13:00:00Z',
}

let client: QueryClient
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 16, 10, 30))
  client = createQueryClient(vi.fn())
  vi.mocked(api.createTransaction).mockReset()
})
afterEach(() => {
  client.clear()
  vi.useRealTimers()
})

function show() {
  const onClose = vi.fn()
  const onCreated = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <TransactionDrawer merchantId="m-1" onClose={onClose} onCreated={onCreated} />
    </QueryClientProvider>,
  )
  return { onClose, onCreated }
}

const reference = () => screen.getByLabelText('Referência externa')
const amount = () => screen.getByLabelText('Valor')
const method = () => screen.getByLabelText<HTMLSelectElement>('Método')
const installments = () => screen.getByLabelText('Parcelas')
const date = () => screen.getByLabelText('Data da transação')
const submit = () => userEvent.click(screen.getByRole('button', { name: /^Cadastrar/ }))

async function fill({ ref = 'AVULSA-1', value = '100,00', payment = 'CREDIT_CARD', count = '3', day = TODAY } = {}) {
  await userEvent.clear(reference())
  if (ref !== '') await userEvent.type(reference(), ref)
  await userEvent.clear(amount())
  if (value !== '') await userEvent.type(amount(), value)
  await userEvent.selectOptions(method(), payment)
  if (!(installments() as HTMLInputElement).disabled) {
    await userEvent.clear(installments())
    if (count !== '') await userEvent.type(installments(), count)
  }
  fireEvent.change(date(), { target: { value: day } })
}

const errorOf = (input: HTMLElement) => document.getElementById(input.getAttribute('aria-describedby') ?? '')?.textContent

describe('TransactionDrawer', () => {
  it('abre com Data = hoje, Parcelas = 1 e Método em Escolha o método (TXN-05)', () => {
    show()

    expect(screen.getByRole('dialog', { name: 'Nova transação' })).toBeInTheDocument()
    expect(reference()).toHaveValue('')
    expect(reference()).toHaveFocus()
    expect(amount()).toHaveValue('')
    expect(method()).toHaveValue('')
    expect(method().selectedOptions[0].textContent).toBe('Escolha o método')
    expect(installments()).toHaveValue('1')
    expect(installments()).toBeEnabled()
    expect(date()).toHaveValue(TODAY)
    expect(date()).toHaveAttribute('max', TODAY)
  })

  it.each(['PIX', 'BOLETO', 'DEBIT_CARD'])('%s fixa Parcelas em 1 e desabilita o campo (TXN-05)', async payment => {
    show()
    await userEvent.selectOptions(method(), 'CREDIT_CARD')
    await userEvent.clear(installments())
    await userEvent.type(installments(), '3')

    await userEvent.selectOptions(method(), payment)

    expect(installments()).toHaveValue('1')
    expect(installments()).toBeDisabled()
  })

  it('Crédito 3x → Pix → Crédito deixa Parcelas em 1 até o operador mudar (TXN-05)', async () => {
    show()
    await userEvent.selectOptions(method(), 'CREDIT_CARD')
    await userEvent.clear(installments())
    await userEvent.type(installments(), '3')
    await userEvent.selectOptions(method(), 'PIX')
    await userEvent.selectOptions(method(), 'CREDIT_CARD')

    expect(installments()).toHaveValue('1')
    expect(installments()).toBeEnabled()
  })

  it('formulário vazio mostra as mensagens de referência, valor e método sem chamar a API (TXN-06)', async () => {
    show()
    await submit()

    expect(errorOf(reference())).toBe('Informe a referência externa')
    expect(errorOf(amount())).toBe('Informe um valor maior que zero com até 2 casas decimais')
    expect(errorOf(method())).toBe('Escolha o método de pagamento')
    expect(reference()).toHaveAttribute('aria-invalid', 'true')
    expect(installments()).toHaveAttribute('aria-invalid', 'false')
    expect(date()).toHaveAttribute('aria-invalid', 'false')
    expect(api.createTransaction).not.toHaveBeenCalled()
  })

  it.each<[string, Parameters<typeof fill>[0], () => HTMLElement, string]>([
    ['referência só com espaços', { ref: '   ' }, reference, 'Informe a referência externa'],
    ['referência com 101 caracteres', { ref: 'R'.repeat(101) }, reference, 'Use até 100 caracteres'],
    ['valor zero', { value: '0,00' }, amount, 'Informe um valor maior que zero com até 2 casas decimais'],
    ['valor negativo', { value: '-5' }, amount, 'Informe um valor maior que zero com até 2 casas decimais'],
    ['valor com 3 casas', { value: '10,123' }, amount, 'Informe um valor maior que zero com até 2 casas decimais'],
    ['valor com 18 dígitos inteiros', { value: `${'9'.repeat(18)},00` }, amount, 'Informe um valor maior que zero com até 2 casas decimais'],
    ['parcelas zero', { count: '0' }, installments, 'Informe um número inteiro de parcelas a partir de 1'],
    ['parcelas fracionadas', { count: '1,5' }, installments, 'Informe um número inteiro de parcelas a partir de 1'],
    ['parcelas vazias', { count: '' }, installments, 'Informe um número inteiro de parcelas a partir de 1'],
    ['data vazia', { day: '' }, date, 'Informe a data da transação'],
    ['data de amanhã', { day: '2026-10-17' }, date, 'Data da transação não pode ser futura'],
  ])('%s mostra a mensagem do campo e não chama a API (TXN-06)', async (_case, values, input, message) => {
    show()
    await fill(values)
    await submit()

    expect(errorOf(input())).toBe(message)
    expect(input()).toHaveAttribute('aria-invalid', 'true')
    expect(api.createTransaction).not.toHaveBeenCalled()
  })

  it('aceita os limites: referência de 100 caracteres sem as bordas, 17 dígitos com 2 casas e data de hoje (TXN-06)', async () => {
    vi.mocked(api.createTransaction).mockResolvedValue(created)
    show()
    await fill({ ref: `  ${'R'.repeat(100)}  `, value: `${'9'.repeat(17)},99`, payment: 'PIX', day: TODAY })
    await submit()

    await waitFor(() => expect(api.createTransaction).toHaveBeenCalledTimes(1))
    expect(api.createTransaction).toHaveBeenCalledWith('jwt', 'm-1', {
      externalReference: 'R'.repeat(100),
      amount: `${'9'.repeat(17)}.99`,
      paymentMethod: 'PIX',
      installments: 1,
      transactionDate: TODAY,
    } satisfies TransactionInput)
  })

  it('envia a referência sem espaços e o valor exato, mostra Cadastrando… e entrega a transação criada (TXN-07)', async () => {
    let resolve!: (transaction: Transaction) => void
    vi.mocked(api.createTransaction).mockReturnValue(new Promise(done => { resolve = done }))
    const { onCreated, onClose } = show()
    await fill({ ref: '  AVULSA-1 ', value: '1234,5', payment: 'CREDIT_CARD', count: '3', day: '2026-10-03' })
    await submit()

    const sending = await screen.findByRole('button', { name: 'Cadastrando…' })
    expect(sending).toBeDisabled()
    fireEvent.submit(document.getElementById('transaction-form')!)
    expect(api.createTransaction).toHaveBeenCalledTimes(1)
    expect(api.createTransaction).toHaveBeenCalledWith('jwt', 'm-1', {
      externalReference: 'AVULSA-1', amount: '1234.5', paymentMethod: 'CREDIT_CARD', installments: 3, transactionDate: '2026-10-03',
    } satisfies TransactionInput)
    expect(onCreated).not.toHaveBeenCalled()

    resolve(created)
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created))
    expect(onClose).not.toHaveBeenCalled()
  })

  it.each([
    ['400', new api.ApiError('Data da transação não pode ser futura', 400, 'VALIDATION_ERROR')],
    ['409 de referência repetida', new api.ApiError('Transação já cadastrada com referência externa: AVULSA-1', 409, 'CONFLICT')],
    ['409 de período travado', new api.ApiError('Janela travada de 2026-10-01 a 2026-10-31', 409, 'CONFLICT')],
  ])('%s mantém a gaveta aberta com a mensagem e os campos, sem o link da regra (TXN-07, TXN-10)', async (_case, error) => {
    vi.mocked(api.createTransaction).mockRejectedValue(error)
    const { onCreated, onClose } = show()
    await fill({ ref: ' AVULSA-1 ', value: '100,00', payment: 'CREDIT_CARD', count: '3' })
    await submit()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(error.message)
    expect(within(alert).queryByRole('link', { name: 'Cadastrar regra de taxa' })).toBeNull()
    expect(screen.getByRole('dialog', { name: 'Nova transação' })).toBeInTheDocument()
    expect(reference()).toHaveValue(' AVULSA-1 ')
    expect(amount()).toHaveValue('100,00')
    expect(method()).toHaveValue('CREDIT_CARD')
    expect(installments()).toHaveValue('3')
    expect(date()).toHaveValue(TODAY)
    expect(screen.getByRole('button', { name: 'Cadastrar' })).toBeEnabled()
    expect(onCreated).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('sem regra de taxa ativa mostra a mensagem e o link para /taxas?cadastrar=1 (TXN-10)', async () => {
    vi.mocked(api.createTransaction).mockRejectedValue(new api.ApiError(MISSING_RULE, 409, 'CONFLICT'))
    show()
    await fill({ payment: 'PIX' })
    await submit()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent(MISSING_RULE)
    expect(within(alert).getByRole('link', { name: 'Cadastrar regra de taxa' })).toHaveAttribute('href', '/taxas?cadastrar=1')
  })
})

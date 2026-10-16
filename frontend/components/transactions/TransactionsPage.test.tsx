import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TransacoesPage from '@/app/(app)/transacoes/page'
import type { Merchant, Transaction } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

vi.mock('next/navigation', () => ({
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Call = { method: string; path: string; search: URLSearchParams; body?: string }

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11222333000181' }

const transaction = (id: string, merchantId: string, externalReference: string, transactionDate: string, fields: Partial<Transaction> = {}): Transaction => ({
  id, merchantId, externalReference, amount: '300.00', expectedNetAmount: '290.73', paymentMethod: 'CREDIT_CARD', installments: 3,
  status: 'APPROVED', transactionDate, createdAt: `${transactionDate}T12:00:00Z`, updatedAt: `${transactionDate}T12:00:00Z`, ...fields,
})

/**
 * In-memory API: the top menu lists `merchants`; `transactions` holds every merchant's sales, filtered like the
 * backend by merchant, window, status and method. Each list answers with `totalPages: 2` so paging is always on.
 */
let server: { merchants: Merchant[]; transactions: Transaction[] }
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })

function route(method: string, path: string, search: URLSearchParams, body?: string): Response {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const list = path.match(/^\/api\/merchants\/([^/]+)\/transactions$/)
  if (list && method === 'GET') {
    const content = server.transactions.filter(item =>
      item.merchantId === list[1]
      && item.transactionDate >= search.get('fromDate')! && item.transactionDate <= search.get('toDate')!
      && (!search.has('status') || item.status === search.get('status'))
      && (!search.has('paymentMethod') || item.paymentMethod === search.get('paymentMethod')))
    return json({ content, totalPages: 2 })
  }
  if (list && method === 'POST') {
    const input = JSON.parse(body!) as { externalReference: string; amount: number; paymentMethod: Transaction['paymentMethod']; installments: number; transactionDate: string }
    const stored = transaction(`t-${server.transactions.length + 1}`, list[1], input.externalReference, input.transactionDate, {
      amount: String(input.amount), expectedNetAmount: '96.51', paymentMethod: input.paymentMethod, installments: input.installments,
    })
    server.transactions.push(stored)
    return json(stored, 201)
  }
  const patch = path.match(/^\/api\/merchants\/([^/]+)\/transactions\/([^/]+)\/status$/)
  if (patch && method === 'PATCH') {
    const stored = server.transactions.find(item => item.id === patch[2])!
    stored.status = (JSON.parse(body!) as { status: Transaction['status'] }).status
    return json(stored)
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 16, 10, 30))
  calls = []
  server = {
    merchants: [alfa, beta],
    transactions: [
      transaction('t-1', 'm-1', 'VENDA-OUT-1', '2026-10-03'),
      transaction('t-2', 'm-1', 'VENDA-OUT-2', '2026-10-05', { status: 'CANCELLED', paymentMethod: 'PIX', installments: 1 }),
      transaction('t-3', 'm-1', 'VENDA-SET-1', '2026-09-12'),
      transaction('t-4', 'm-2', 'BETA-OUT-1', '2026-10-04', { status: 'CANCELLED' }),
    ],
  }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    const body = typeof init.body === 'string' ? init.body : undefined
    calls.push({ method, path: url.pathname, search: url.searchParams, body })
    return route(method, url.pathname, url.searchParams, body)
  }))
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.useRealTimers()
})

function renderPage() {
  render(
    <QueryClientProvider client={createQueryClient(vi.fn())}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <TransacoesPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const listCalls = () => calls.filter(call => call.method === 'GET' && /\/transactions$/.test(call.path))
const lastList = () => listCalls().at(-1)!
const query = (call: Call) => Object.fromEntries(call.search)
const table = () => screen.findByRole('table', { name: 'Transações' })
const references = () => within(screen.getByRole('table', { name: 'Transações' })).getAllByRole('row').slice(1).map(row => within(row).getAllByRole('cell')[1].textContent)
const nextPage = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(within(await screen.findByRole('navigation', { name: 'Paginação' })).getByRole('button', { name: 'Próxima' }))
  await waitFor(() => expect(lastList().search.get('page')).toBe('1'))
}
/** The success notice, whose reference sits in its own element. */
const notice = (text: string) => screen.findByText((_, element) => element?.getAttribute('role') === 'status' && element.textContent === text)
const switchTo = async (user: ReturnType<typeof userEvent.setup>, name: RegExp) => {
  await user.click(screen.getByRole('button', { name: 'Alfa Pagamentos' }))
  await user.click(screen.getByRole('option', { name }))
}

describe('TransactionsPage', () => {
  it('a rota busca o mês atual da empresa selecionada e mostra o título e o nome da empresa (TXN-01)', async () => {
    renderPage()

    await table()
    expect(screen.getByRole('heading', { level: 1, name: 'Transações' })).toBeInTheDocument()
    expect(screen.getAllByText('Alfa Pagamentos').length).toBeGreaterThan(0)
    expect(references()).toEqual(['VENDA-OUT-1', 'VENDA-OUT-2'])
    expect(listCalls()).toHaveLength(1)
    expect(lastList().path).toBe('/api/merchants/m-1/transactions')
    expect(query(lastList())).toEqual({
      fromDate: '2026-10-01', toDate: '2026-10-31', page: '0', size: '20', sort: 'transactionDate,desc',
    })
  })

  it('escolher um mês refaz a busca com a janela do mês e volta para a página 1 (TXN-02)', async () => {
    const user = renderPage()
    await table()
    await nextPage(user)

    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    const months = within(screen.getByRole('list', { name: 'Meses' })).getAllByRole('button')
    expect(months).toHaveLength(12)
    expect(months[0]).toHaveTextContent('Outubro de 2026')
    expect(months[11]).toHaveTextContent('Novembro de 2025')
    await user.click(screen.getByRole('button', { name: 'Setembro de 2026' }))

    await waitFor(() => expect(references()).toEqual(['VENDA-SET-1']))
    expect(query(lastList())).toMatchObject({ fromDate: '2026-09-01', toDate: '2026-09-30', page: '0' })
    expect(screen.getByRole('button', { name: /Setembro de 2026/ })).toHaveAttribute('aria-expanded', 'false')
  })

  it('intervalo personalizado refaz a busca com as datas digitadas e volta para a página 1 (TXN-02)', async () => {
    const user = renderPage()
    await table()
    await nextPage(user)

    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-09-10' } })
    fireEvent.change(screen.getByLabelText('Até'), { target: { value: '2026-10-04' } })
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    await waitFor(() => expect(references()).toEqual(['VENDA-OUT-1', 'VENDA-SET-1']))
    expect(query(lastList())).toMatchObject({ fromDate: '2026-09-10', toDate: '2026-10-04', page: '0' })
  })

  it('intervalo invertido mostra a mensagem no menu e não refaz a busca (TXN-02)', async () => {
    const user = renderPage()
    await table()
    const before = listCalls().length

    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-10-20' } })
    fireEvent.change(screen.getByLabelText('Até'), { target: { value: '2026-10-19' } })
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    expect(within(screen.getByRole('dialog', { name: 'Período' })).getByRole('alert'))
      .toHaveTextContent('A data inicial deve ser igual ou anterior à final')
    expect(listCalls()).toHaveLength(before)
    expect(screen.getByRole('button', { name: /Outubro de 2026/, expanded: true })).toBeInTheDocument()
  })

  it('o filtro de status envia status=<valor>, volta para a página 1 e Todos remove o parâmetro (TXN-03)', async () => {
    const user = renderPage()
    await table()
    const status = screen.getByLabelText('Status')
    expect(within(status).getAllByRole('option').map(option => option.textContent)).toEqual(['Todos', 'Aprovada', 'Cancelada', 'Estornada', 'Chargeback'])
    await nextPage(user)

    await user.selectOptions(status, 'Cancelada')
    await waitFor(() => expect(references()).toEqual(['VENDA-OUT-2']))
    expect(query(lastList())).toMatchObject({ status: 'CANCELLED', page: '0', fromDate: '2026-10-01', toDate: '2026-10-31' })

    await user.selectOptions(status, 'Todos')
    await waitFor(() => expect(references()).toEqual(['VENDA-OUT-1', 'VENDA-OUT-2']))
    expect(lastList().search.has('status')).toBe(false)
  })

  it('o filtro de método envia paymentMethod=<valor>, volta para a página 1 e Todos remove o parâmetro (TXN-03)', async () => {
    const user = renderPage()
    await table()
    const method = screen.getByLabelText('Método')
    expect(within(method).getAllByRole('option').map(option => option.textContent)).toEqual(['Todos', 'Crédito', 'Débito', 'Pix', 'Boleto'])
    await nextPage(user)

    await user.selectOptions(method, 'Pix')
    await waitFor(() => expect(references()).toEqual(['VENDA-OUT-2']))
    expect(query(lastList())).toMatchObject({ paymentMethod: 'PIX', page: '0' })

    await user.selectOptions(method, 'Todos')
    await waitFor(() => expect(references()).toEqual(['VENDA-OUT-1', 'VENDA-OUT-2']))
    expect(lastList().search.has('paymentMethod')).toBe(false)
  })

  it('cadastro aceito fecha a gaveta, mostra o aviso e recarrega a lista (TXN-07)', async () => {
    const user = renderPage()
    await table()

    await user.click(screen.getByRole('button', { name: 'Nova transação' }))
    const drawer = screen.getByRole('dialog', { name: 'Nova transação' })
    await user.type(within(drawer).getByLabelText('Referência externa'), ' AVULSA-1 ')
    await user.type(within(drawer).getByLabelText('Valor'), '100,00')
    await user.selectOptions(within(drawer).getByLabelText('Método'), 'CREDIT_CARD')
    await user.clear(within(drawer).getByLabelText('Parcelas'))
    await user.type(within(drawer).getByLabelText('Parcelas'), '3')
    await user.click(within(drawer).getByRole('button', { name: 'Cadastrar' }))

    expect(await notice('Transação AVULSA-1 cadastrada.')).toBeInTheDocument()
    expect(screen.queryByRole('dialog', { name: 'Nova transação' })).toBeNull()
    const post = calls.find(call => call.method === 'POST')!
    expect(post.body).toBe('{"externalReference":"AVULSA-1","amount":100.00,"paymentMethod":"CREDIT_CARD","installments":3,"transactionDate":"2026-10-16"}')
    await waitFor(() => expect(references()).toContain('AVULSA-1'))
    expect(listCalls()).toHaveLength(2)
  })

  it('mudança de status aceita mostra a linha com o novo status e sem a ação (TXN-09)', async () => {
    const user = renderPage()
    await table()
    const row = () => within(screen.getByRole('table', { name: 'Transações' })).getByRole('row', { name: /VENDA-OUT-1/ })

    await user.click(within(row()).getByRole('button', { name: 'Alterar status' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Alterar status' })
    await user.click(within(dialog).getByRole('radio', { name: 'Estornada' }))
    await user.click(within(dialog).getByRole('button', { name: 'Confirmar' }))

    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull())
    expect(calls.find(call => call.method === 'PATCH')).toMatchObject({ path: '/api/merchants/m-1/transactions/t-1/status', body: '{"status":"REFUNDED"}' })
    expect(within(row()).getAllByRole('cell')[6]).toHaveTextContent('Estornada')
    expect(within(row()).queryByRole('button', { name: 'Alterar status' })).toBeNull()
  })

  it('trocar de empresa fecha o diálogo, remove o aviso, mantém os filtros e busca a página 1 da nova empresa (TXN-11)', async () => {
    const user = renderPage()
    await table()
    await user.click(screen.getByRole('button', { name: 'Nova transação' }))
    const drawer = screen.getByRole('dialog', { name: 'Nova transação' })
    await user.type(within(drawer).getByLabelText('Referência externa'), 'AVULSA-2')
    await user.type(within(drawer).getByLabelText('Valor'), '50')
    await user.selectOptions(within(drawer).getByLabelText('Método'), 'PIX')
    await user.click(within(drawer).getByRole('button', { name: 'Cadastrar' }))
    expect(await notice('Transação AVULSA-2 cadastrada.')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Status'), 'Aprovada')
    await waitFor(() => expect(lastList().search.get('status')).toBe('APPROVED'))
    await nextPage(user)
    await user.click(within(screen.getByRole('table', { name: 'Transações' })).getAllByRole('button', { name: 'Alterar status' })[0])
    expect(screen.getByRole('alertdialog', { name: 'Alterar status' })).toBeInTheDocument()

    await switchTo(user, /Beta Comércio/)

    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(screen.queryByText((_, element) => element?.getAttribute('role') === 'status' && /cadastrada\.$/.test(element.textContent ?? ''))).toBeNull()
    await waitFor(() => expect(lastList().path).toBe('/api/merchants/m-2/transactions'))
    expect(query(lastList())).toMatchObject({ fromDate: '2026-10-01', toDate: '2026-10-31', status: 'APPROVED', page: '0' })
    expect(screen.getByLabelText('Status')).toHaveValue('APPROVED')
    expect(await screen.findByText('Nenhuma transação encontrada para os filtros escolhidos.')).toBeInTheDocument()
  })

  it('trocar de empresa fecha a gaveta aberta (TXN-11)', async () => {
    const user = renderPage()
    await table()
    await user.click(screen.getByRole('button', { name: 'Nova transação' }))
    expect(screen.getByRole('dialog', { name: 'Nova transação' })).toBeInTheDocument()

    await switchTo(user, /Beta Comércio/)

    expect(screen.queryByRole('dialog', { name: 'Nova transação' })).toBeNull()
    expect(await screen.findByText('BETA-OUT-1')).toBeInTheDocument()
  })

  it('sem empresa no acesso mostra o mesmo estado vazio de Regras de taxa, sem filtros nem lista (TXN-11)', async () => {
    server.merchants = []
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Status')).toBeNull()
    expect(screen.queryByLabelText('Método')).toBeNull()
    expect(screen.queryByRole('button', { name: /Outubro de 2026/ })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Nova transação' })).toBeNull()
    expect(screen.queryByRole('table')).toBeNull()
    expect(listCalls()).toHaveLength(0)
  })
})

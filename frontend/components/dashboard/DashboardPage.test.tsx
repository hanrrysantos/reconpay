import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { DiscrepancyDetail, Merchant, Period, PeriodSummary, ReconciliationItem, Run } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'
import { DashboardPage } from './DashboardPage'
import { makeDetail, makeItem, makePeriod, makeRun, makeSummary } from './fixtures'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: vi.fn(), push: vi.fn() }), usePathname: () => '/' }))

const august = { fromDate: '2026-08-01', toDate: '2026-08-31' }

type Reply = Response | (() => Response) | Error
type Call = { method: string; path: string; search: URLSearchParams; headers: Record<string, string>; body: unknown }

/** In-memory API: each field is what the matching endpoint answers now; tests change them between steps. */
let server: {
  merchants: Merchant[]
  runs: Record<string, Run[]>
  period: (merchantId: string, search: URLSearchParams) => Reply
  summary: () => Reply
  items: () => ReconciliationItem[]
  detail: () => DiscrepancyDetail
  patch: () => Reply
  postRun: () => Reply
  lock: () => Reply
  unlock: () => Reply
}
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })
const fail = (status: number, message: string) => json({ error: status === 409 ? 'CONFLICT' : 'ERROR', message }, status)
const page = <T,>(content: T[]) => ({ content, totalPages: 1, totalElements: content.length })
const resolve = (reply: Reply) => {
  if (reply instanceof Error) throw reply
  return typeof reply === 'function' ? reply() : reply
}

function route(method: string, path: string, search: URLSearchParams, body: unknown): Response {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const [, , , merchantId, ...rest] = path.split('/')
  const tail = rest.join('/')
  if (tail === 'reconciliations' && method === 'GET') {
    // Same contract as the API: fromDate and toDate together keep only the runs of that exact window.
    const runs = server.runs[merchantId] ?? []
    const fromDate = search.get('fromDate')
    const toDate = search.get('toDate')
    return json(page(fromDate && toDate ? runs.filter(run => run.fromDate === fromDate && run.toDate === toDate) : runs))
  }
  if (tail === 'reconciliations' && method === 'POST') return resolve(server.postRun())
  if (tail === 'periods') return resolve(server.period(merchantId, search))
  if (tail === 'periods/summary') return resolve(server.summary())
  if (tail === 'periods/lock') return resolve(server.lock())
  if (tail === 'periods/unlock') return resolve(server.unlock())
  if (tail.endsWith('/items')) return json(page(server.items()))
  if (tail.includes('/discrepancies/')) return method === 'PATCH' ? resolve(server.patch()) : json(server.detail())
  if (tail.endsWith('/export')) return new Response('externalReference;result\nPED-1;MATCHED\n', { headers: { 'Content-Type': 'text/csv' } })
  if (tail === 'external-settlements/imports' || tail === 'bank-statements/imports') {
    return json(page([{ id: 'i-1', merchantId, fileName: 'a.csv', totalRows: 1, createdAt: '2026-10-05T12:00:00Z' }]))
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path} ${body}`)
}

beforeEach(() => {
  calls = []
  server = {
    merchants: [{ id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000190' }, { id: 'm-2', name: 'Beta Comércio', document: '98765432000110' }],
    runs: { 'm-1': [makeRun(), makeRun({ id: 'run-8', ...august, createdAt: '2026-09-01T09:00:00Z' })], 'm-2': [makeRun({ id: 'run-b', merchantId: 'm-2', ...august })] },
    period: () => json(makePeriod()),
    summary: () => json(makeSummary()),
    items: () => [makeItem()],
    detail: () => makeDetail(),
    patch: () => json(makeDetail({ status: 'ACCEPTED' })),
    postRun: () => json(makeRun({ id: 'run-10', status: 'PENDING', processedItems: null })),
    lock: () => json(makePeriod({ locked: true, lockedAt: '2026-10-14T10:00:00Z' })),
    unlock: () => json(makePeriod()),
  }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    const body = init.body ? JSON.parse(String(init.body)) : undefined
    calls.push({ method, path: url.pathname, search: url.searchParams, headers: (init.headers ?? {}) as Record<string, string>, body })
    return route(method, url.pathname, url.searchParams, body)
  }))
})
afterEach(() => vi.unstubAllGlobals())

function renderPage() {
  const client = createQueryClient(vi.fn())
  render(
    <QueryClientProvider client={client}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <DashboardPage today={new Date(2026, 9, 14)} />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const requests = (method: string, suffix: string) => calls.filter(call => call.method === method && call.path.endsWith(suffix))
const heading = () => screen.getByRole('heading', { level: 1 })

describe('DashboardPage', () => {
  it('abre na janela do run mais recente (DASH-02 AC1)', async () => {
    renderPage()
    expect(await screen.findByRole('heading', { name: 'Faltam 12 divergências para fechar setembro' })).toBeInTheDocument()
    expect(screen.getByText('Fechamento · Setembro de 2026')).toBeInTheDocument()
    const period = requests('GET', '/periods')[0]
    expect([period.search.get('fromDate'), period.search.get('toDate')]).toEqual(['2026-09-01', '2026-09-30'])
    expect(screen.getByTestId('ring-value')).toHaveTextContent('94,2%')
    expect(screen.getByText('R$ 10.389,38')).toBeInTheDocument()
  })

  it('sem run, abre no mês atual com o estado vazio (DASH-02 AC2, DASH-06 AC3)', async () => {
    server.runs['m-1'] = []
    renderPage()
    expect(await screen.findByRole('heading', { name: 'Outubro ainda não foi conciliado' })).toBeInTheDocument()
    expect(screen.getByText('Fechamento · Outubro de 2026')).toBeInTheDocument()
    expect(screen.getByRole('list', { name: 'Primeiros passos' })).toBeInTheDocument()
    expect(screen.queryByText(/R\$/)).not.toBeInTheDocument()
  })

  it('sem empresa no acesso mostra o estado próprio, sem valores (DASH-06 AC2, AC7)', async () => {
    server.merchants = []
    renderPage()
    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cadastrar empresa' })).toHaveAttribute('href', '/empresas')
    expect(screen.queryByText(/R\$/)).not.toBeInTheDocument()
  })

  it('403 mostra "Você não tem acesso a esta empresa" com o menu de empresa (DASH-06 AC6)', async () => {
    server.runs = {}
    vi.mocked(fetch).mockImplementation(async (input, init = {}) => {
      const url = new URL(String(input))
      if (url.pathname.endsWith('/reconciliations')) return fail(403, 'Acesso negado')
      return route(init.method ?? 'GET', url.pathname, url.searchParams, undefined)
    })
    renderPage()
    const forbidden = await screen.findByRole('heading', { name: 'Você não tem acesso a esta empresa' })
    expect(within(forbidden.closest('section')!).getByRole('button', { name: /Alfa Pagamentos/ })).toBeInTheDocument()
  })

  it('trocar de empresa recarrega o dashboard dela na sua janela inicial (DASH-01 AC6)', async () => {
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Faltam 12 divergências para fechar setembro' })
    server.period = merchantId => json(makePeriod({ runId: 'run-b', ...(merchantId === 'm-2' ? august : {}), openAmount: '77.00' }))
    await user.click(screen.getByRole('button', { name: /Alfa Pagamentos/ }))
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))
    expect(await screen.findByText('Fechamento · Agosto de 2026')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('R$ 77,00')).toBeInTheDocument())
    const last = requests('GET', '/periods').at(-1)!
    expect([last.path, last.search.get('fromDate')]).toEqual(['/api/merchants/m-2/periods', '2026-08-01'])
  })

  it('janela personalizada com 400 mantém a janela anterior e mostra a mensagem (DASH-02 AC4, AC5)', async () => {
    server.period = (_, search) => search.get('fromDate') === '2025-01-01'
      ? fail(400, 'A janela não pode passar de 366 dias.')
      : json(makePeriod())
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Faltam 12 divergências para fechar setembro' })
    await user.click(screen.getByRole('button', { name: /Setembro de 2026/ }))
    const menu = screen.getByRole('dialog', { name: 'Período' })
    const [from, to] = within(menu).getAllByDisplayValue(/2026-09/)
    await user.clear(from)
    await user.type(from, '2025-01-01')
    await user.clear(to)
    await user.type(to, '2026-09-30')
    await user.click(within(menu).getByRole('button', { name: 'Aplicar' }))
    expect(await within(menu).findByRole('alert')).toHaveTextContent('A janela não pode passar de 366 dias.')
    expect(requests('GET', '/periods').some(call => call.search.get('fromDate') === '2025-01-01' && call.search.get('toDate') === '2026-09-30')).toBe(true)
    expect(screen.getByText('Fechamento · Setembro de 2026')).toBeInTheDocument()
  })

  it('resolver pela gaveta atualiza tabela, abertas, valor em aberto e régua (DASH-04 AC9)', async () => {
    const user = renderPage()
    await user.click(await screen.findByRole('button', { name: 'Resolver PED-10021' }))
    const drawer = screen.getByRole('dialog', { name: 'Resolver divergência' })
    await user.click(within(drawer).getByRole('radio', { name: /^Aceitar/ }))
    server.summary = () => json(makeSummary({ openByType: { MISSING_SETTLEMENT: 6, FEE_DIVERGENCE: 5 } }))
    server.period = () => json(makePeriod({ openAmount: '5519.38' }))
    server.items = () => []
    server.detail = () => makeDetail({ status: 'ACCEPTED' })
    await user.click(within(drawer).getByRole('button', { name: /^Aceitar/ }))

    expect(await screen.findByRole('heading', { name: 'Faltam 11 divergências para fechar setembro' })).toBeInTheDocument()
    expect(requests('PATCH', '/discrepancies/d-1')[0].body).toEqual({ status: 'ACCEPTED' })
    await waitFor(() => expect(screen.getByText('R$ 5.519,38')).toBeInTheDocument())
    expect(screen.getByText('Nenhuma divergência aberta')).toBeInTheDocument()
    expect(screen.getByText('11 abertas')).toBeInTheDocument()
    await waitFor(() => expect(within(drawer).getByTestId('drawer-status')).toHaveTextContent('Aceita'))
  })

  it('conciliar mostra o run em andamento e, ao concluir, recarrega período, resumo e divergências (DASH-05 AC1, AC2, AC4)', async () => {
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Faltam 12 divergências para fechar setembro' })
    server.runs['m-1'] = [makeRun({ id: 'run-10', status: 'RUNNING', processedItems: 321, totalItems: 1284 }), ...server.runs['m-1']]
    server.period = () => fail(409, 'Run em andamento')
    server.summary = () => fail(409, 'Run em andamento')
    await user.click(screen.getByRole('button', { name: 'Conciliar período' }))

    expect(requests('POST', '/reconciliations')[0].body).toEqual({ fromDate: '2026-09-01', toDate: '2026-09-30' })
    expect(await screen.findByRole('heading', { name: 'Conciliando setembro…' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Conciliando…' })).toBeDisabled()
    expect(screen.getByRole('progressbar', { name: 'Progresso do run' })).toHaveAttribute('aria-valuenow', '25')
    expect(screen.getByText('R$ 10.389,38')).toBeInTheDocument()

    const before = { period: requests('GET', '/periods').length, summary: requests('GET', '/summary').length, items: requests('GET', '/items').length }
    server.runs['m-1'][0] = makeRun({ id: 'run-10', status: 'COMPLETED', processedItems: 1284 })
    server.period = () => json(makePeriod({ runId: 'run-10', matchRate: '0.9800' }))
    server.summary = () => json(makeSummary({ runId: 'run-10', openByType: { FEE_DIVERGENCE: 3 } }))
    expect(await screen.findByRole('heading', { name: 'Faltam 3 divergências para fechar setembro' }, { timeout: 5000 })).toBeInTheDocument()
    expect(screen.getByTestId('ring-value')).toHaveTextContent('98%')
    expect(requests('GET', '/periods').length).toBeGreaterThan(before.period)
    expect(requests('GET', '/summary').length).toBeGreaterThan(before.summary)
    await waitFor(() => expect(requests('GET', '/items').some(call => call.path.includes('/run-10/'))).toBe(true))
    expect(requests('GET', '/items').length).toBeGreaterThan(before.items)
  }, 10000)

  it('409 no POST do run mostra a mensagem e acompanha o run existente (DASH-05 AC6)', async () => {
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Faltam 12 divergências para fechar setembro' })
    server.postRun = () => fail(409, 'Já existe um run em andamento para esta janela.')
    server.runs['m-1'] = [makeRun({ id: 'run-10', status: 'RUNNING', processedItems: null }), ...server.runs['m-1']]
    server.period = () => fail(409, 'Run em andamento')
    await user.click(screen.getByRole('button', { name: 'Conciliar período' }))

    expect(await screen.findByText('Já existe um run em andamento para esta janela.')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Conciliando setembro…' })).toBeInTheDocument()
  })

  it('travar envia o lock e mostra o estado travado da resposta (DASH-07 AC2, AC5)', async () => {
    server.summary = () => json(makeSummary({ openByType: {} }))
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Setembro sem divergências abertas' })
    server.period = () => json(makePeriod({ locked: true }))
    await user.click(screen.getByRole('button', { name: 'Travar período' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))

    expect(requests('POST', '/periods/lock')[0].body).toEqual({ fromDate: '2026-09-01', toDate: '2026-09-30' })
    expect(await screen.findByRole('heading', { name: 'Setembro fechado em 94,2%' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Conciliar período' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Exportar relatório' })).toBeInTheDocument()
  })

  it('409 no lock mostra a mensagem da API e recarrega o período (DASH-07 AC4)', async () => {
    server.summary = () => json(makeSummary({ openByType: {} }))
    server.lock = () => fail(409, 'O período tem divergências abertas.')
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Setembro sem divergências abertas' })
    const periodGets = requests('GET', '/periods').length
    await user.click(screen.getByRole('button', { name: 'Travar período' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))

    expect(await screen.findByText('O período tem divergências abertas.')).toBeInTheDocument()
    await waitFor(() => expect(requests('GET', '/periods').length).toBeGreaterThan(periodGets))
  })

  it('destravar envia só o unlock com a janela e volta ao estado aberto (DASH-07 AC3)', async () => {
    server.period = () => json(makePeriod({ locked: true }))
    server.summary = () => json(makeSummary({ openByType: {} }))
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Setembro fechado em 94,2%' })
    server.period = () => json(makePeriod())
    await user.click(screen.getByRole('button', { name: 'Destravar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Destravar' }))

    await waitFor(() => expect(requests('POST', '/periods/unlock')).toHaveLength(1))
    expect(requests('POST', '/periods/unlock')[0].body).toEqual({ fromDate: '2026-09-01', toDate: '2026-09-30' })
    expect(requests('POST', '/periods/lock')).toHaveLength(0)
    expect(await screen.findByRole('heading', { name: 'Setembro sem divergências abertas' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Conciliar período' })).toBeEnabled()
  })

  it('409 no unlock mostra a mensagem da API e recarrega o período (DASH-07 AC4)', async () => {
    server.period = () => json(makePeriod({ locked: true }))
    server.summary = () => json(makeSummary({ openByType: {} }))
    server.unlock = () => fail(409, 'O período já está aberto.')
    const user = renderPage()
    await screen.findByRole('heading', { name: 'Setembro fechado em 94,2%' })
    const periodGets = requests('GET', '/periods').length
    await user.click(screen.getByRole('button', { name: 'Destravar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Destravar' }))

    expect(await screen.findByText('O período já está aberto.')).toBeInTheDocument()
    await waitFor(() => expect(requests('GET', '/periods').length).toBeGreaterThan(periodGets))
    expect(requests('POST', '/periods/lock')).toHaveLength(0)
  })

  it('exportar baixa o CSV do run com o token da sessão (DASH-07 AC6)', async () => {
    server.period = () => json(makePeriod({ locked: true }))
    server.summary = () => json(makeSummary({ openByType: {} }))
    const createObjectURL = vi.fn(() => 'blob:csv')
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() }))
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const user = renderPage()
    await user.click(await screen.findByRole('button', { name: 'Exportar relatório' }))

    await waitFor(() => expect(click).toHaveBeenCalledOnce())
    const exported = requests('GET', '/reconciliations/run-9/export')[0]
    expect(exported.headers.Authorization).toBe('Bearer jwt')
    expect(await (createObjectURL.mock.calls[0] as unknown as [Blob])[0].text()).toBe('externalReference;result\nPED-1;MATCHED\n')
    const anchor = click.mock.contexts[0] as HTMLAnchorElement
    expect(anchor.download).toBe('reconpay-2026-09-01-2026-09-30.csv')
    expect(anchor.href).toBe('blob:csv')
    click.mockRestore()
  })

  it('erro no resumo mostra o erro só nos blocos dele e "Recarregar" refaz (DASH-03 AC9)', async () => {
    server.summary = () => fail(500, 'Erro interno')
    const user = renderPage()
    expect(await screen.findByText('O gráfico não carregou', {}, { timeout: 3000 })).toBeInTheDocument()
    expect(screen.getByTestId('ring-value')).toHaveTextContent('94,2%')
    expect(screen.getByText('R$ 10.389,38')).toBeInTheDocument()
    server.summary = () => json(makeSummary())
    await user.click(screen.getByRole('button', { name: 'Recarregar gráfico' }))
    expect(await screen.findByText('Esperado × recebido no banco')).toBeInTheDocument()
    expect(screen.queryByText('O gráfico não carregou')).not.toBeInTheDocument()
  })

  it('erro de rede mostra o banner, mantém os blocos e "Tentar de novo" refaz (DASH-06 AC4)', async () => {
    server.summary = () => { throw new TypeError('Failed to fetch') }
    const user = renderPage()
    expect(await screen.findByText('Não foi possível conectar à API', {}, { timeout: 3000 })).toBeInTheDocument()
    expect(screen.getByTestId('ring-value')).toHaveTextContent('94,2%')
    server.summary = () => json(makeSummary())
    await user.click(screen.getByRole('button', { name: 'Tentar de novo' }))
    await waitFor(() => expect(screen.queryByText('Não foi possível conectar à API')).not.toBeInTheDocument())
    expect(heading()).toHaveTextContent('Faltam 12 divergências para fechar setembro')
  })
})

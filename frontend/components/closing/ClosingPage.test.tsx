import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import FechamentoPage from '@/app/(app)/fechamento/page'
import type { Merchant, Period, Run } from '@/lib/api'
import { createQueryClient, queryKeys } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

vi.mock('next/navigation', () => ({
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Call = { method: string; path: string; search: URLSearchParams; body?: string }

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11222333000181' }

const run = (id: string, merchantId: string, fromDate: string, toDate: string): Run => ({
  id, merchantId, fromDate, toDate, status: 'COMPLETED', totalItems: 4, matchedCount: 2, divergentCount: 2,
  createdAt: `${toDate}T12:00:00Z`, startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 4,
})
const lastDay = (year: number, month: number) => new Date(year, month, 0).getDate()
/** Runs of the merchant for `count` whole months, newest (October 2026) first. */
const monthRuns = (merchantId: string, count: number) => Array.from({ length: count }, (_, index) => {
  const date = new Date(2026, 9 - index, 1)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  return run(`r-${merchantId}-${year}-${month}`, merchantId, `${year}-${month}-01`, `${year}-${month}-${lastDay(year, date.getMonth() + 1)}`)
})

/**
 * In-memory API like the backend. `/periods` answers the window's completed run (404 without one, 409 in `running`);
 * the lock answers 409 while `open` lists the window; `runsAnswer` overrides the runs list.
 */
let server: {
  merchants: Merchant[]; runs: Run[]; locked: Set<string>; open: Set<string>; running: Set<string>; failedOnly: Set<string>
  runsAnswer?: () => Response
}
let calls: Call[]
let client: QueryClient

const json = (body: unknown, status = 200) => Response.json(body, { status })
const fail = (status: number, message: string) => json({ error: 'ERROR', message }, status)
const windowOf = (search: URLSearchParams) => `${search.get('fromDate')}/${search.get('toDate')}`

function periodOf(merchantId: string, key: string): Period {
  const [fromDate, toDate] = key.split('/')
  const owner = server.runs.find(entry => entry.merchantId === merchantId && `${entry.fromDate}/${entry.toDate}` === key)!
  const locked = server.locked.has(key)
  return {
    runId: owner.id, fromDate, toDate, totalItems: 4, matchedCount: 2, divergentCount: 2, matchRate: '0.5000',
    openAmount: locked ? '0.00' : '12.30', locked, lockedAt: locked ? '2026-10-20T09:00:00' : null, closeDurationSeconds: locked ? 180 : null,
  }
}

function route(method: string, path: string, search: URLSearchParams, body?: string): Response {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const merchantId = path.split('/')[3]
  if (path.endsWith('/reconciliations') && method === 'GET') {
    if (server.runsAnswer) return server.runsAnswer()
    const content = server.runs.filter(entry => entry.merchantId === merchantId)
    return json({ content, totalPages: 1 })
  }
  if (path.endsWith('/periods')) {
    const key = windowOf(search)
    if (server.running.has(key)) return fail(409, 'Janela possui conciliação em andamento')
    if (server.failedOnly.has(key)) return fail(404, 'Período não encontrado')
    return json(periodOf(merchantId, key))
  }
  if (path.endsWith('/periods/lock')) {
    const { fromDate, toDate } = JSON.parse(body!) as { fromDate: string; toDate: string }
    const key = `${fromDate}/${toDate}`
    if (server.open.has(key)) return fail(409, 'Janela possui divergência em aberto')
    server.locked.add(key)
    return json(periodOf(merchantId, key))
  }
  if (path.endsWith('/periods/unlock')) {
    const { fromDate, toDate } = JSON.parse(body!) as { fromDate: string; toDate: string }
    server.locked.delete(`${fromDate}/${toDate}`)
    return json(periodOf(merchantId, `${fromDate}/${toDate}`))
  }
  if (path.endsWith('/export')) return new Response('externalReference;result\n', { headers: { 'Content-Type': 'text/csv' } })
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  calls = []
  client = createQueryClient(vi.fn())
  server = {
    merchants: [alfa, beta],
    runs: [
      run('r-oct-2', 'm-1', '2026-10-01', '2026-10-31'),
      run('r-half', 'm-1', '2026-10-01', '2026-10-15'),
      run('r-oct-1', 'm-1', '2026-10-01', '2026-10-31'),
      run('r-sep', 'm-1', '2026-09-01', '2026-09-30'),
      run('r-aug', 'm-1', '2026-08-01', '2026-08-31'),
      run('r-beta', 'm-2', '2026-07-01', '2026-07-31'),
    ],
    locked: new Set(['2026-09-01/2026-09-30']),
    open: new Set(),
    running: new Set(),
    failedOnly: new Set(['2026-08-01/2026-08-31']),
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
  client.clear()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

function renderRoute() {
  render(
    <QueryClientProvider client={client}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <FechamentoPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const table = () => screen.findByRole('table', { name: 'Períodos' })
/** The page's own title block; the top menu shows the merchant name too, so the name is looked up only here. */
const pageHeader = () => screen.getByRole('heading', { level: 1, name: 'Fechamento' }).parentElement!
const rows = () => within(screen.getByRole('table', { name: 'Períodos' })).getAllByRole('row').slice(1)
const rowText = () => rows().map(row => { const cells = within(row).getAllByRole('cell'); return `${cells[0].textContent}|${cells[1].textContent}` })
const rowOf = (name: string) => rows().find(row => within(row).getAllByRole('cell')[0].textContent === name)!
const periodReads = () => calls.filter(call => call.method === 'GET' && call.path.endsWith('/periods')).map(call => windowOf(call.search))
const nav = () => screen.getByRole('navigation', { name: 'Paginação' })
const switchTo = async (user: ReturnType<typeof userEvent.setup>, from: string, name: RegExp) => {
  await user.click(screen.getByRole('button', { name: from }))
  await user.click(screen.getByRole('option', { name }))
}
const lockRow = async (user: ReturnType<typeof userEvent.setup>, name: string) => {
  await user.click(await within(rowOf(name)).findByRole('button', { name: `Travar ${name}` }))
  await user.click(within(within(rowOf(name)).getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))
}

describe('ClosingPage', () => {
  it('a rota mostra o título, a empresa e uma linha por janela, em ordem, com o status de cada uma (FEC-01 AC1–AC3, edge de vários runs)', async () => {
    renderRoute()

    const periods = await table()
    expect(within(pageHeader()).getByText('Alfa Pagamentos')).toBeInTheDocument()
    expect(within(periods).getAllByRole('columnheader').map(header => header.textContent))
      .toEqual(['Período', 'Status', 'Match', 'Em aberto', 'Travado em', 'Duração', 'Ações'])
    await waitFor(() => expect(rowText()).toEqual([
      'Outubro de 2026|Aberto',
      '01/10/2026 – 15/10/2026|Aberto',
      'Setembro de 2026|Travado',
      'Agosto de 2026|Sem conciliação concluída',
    ]))
    expect(calls.find(call => call.path.endsWith('/reconciliations'))!.search.get('size')).toBe('100')
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).toBeNull()
  })

  it('com 12 janelas mostra 10 por página, com as bordas desabilitadas, e só lê as janelas da página (FEC-03 AC1, AC4)', async () => {
    server.runs = monthRuns('m-1', 12)
    server.locked = new Set()
    server.failedOnly = new Set()
    const user = renderRoute()

    await table()
    await waitFor(() => expect(rows()).toHaveLength(10))
    expect(rowText()[0]).toBe('Outubro de 2026|Aberto')
    expect(rowText()[9]).toBe('Janeiro de 2026|Aberto')
    expect(within(nav()).getByText('Página 1 de 2')).toBeInTheDocument()
    expect(within(nav()).getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(within(nav()).getByRole('button', { name: 'Próxima' })).toBeEnabled()
    await waitFor(() => expect(new Set(periodReads())).toEqual(new Set(server.runs.slice(0, 10).map(entry => `${entry.fromDate}/${entry.toDate}`))))

    await user.click(within(nav()).getByRole('button', { name: 'Próxima' }))

    await waitFor(() => expect(rowText()).toEqual(['Dezembro de 2025|Aberto', 'Novembro de 2025|Aberto']))
    expect(within(nav()).getByText('Página 2 de 2')).toBeInTheDocument()
    expect(within(nav()).getByRole('button', { name: 'Próxima' })).toBeDisabled()
    expect(within(nav()).getByRole('button', { name: 'Anterior' })).toBeEnabled()
    expect(new Set(periodReads())).toEqual(new Set(server.runs.map(entry => `${entry.fromDate}/${entry.toDate}`)))

    await user.click(within(nav()).getByRole('button', { name: 'Anterior' }))
    await waitFor(() => expect(rows()).toHaveLength(10))
    expect(within(nav()).getByText('Página 1 de 2')).toBeInTheDocument()
  })

  it('com exatamente 10 janelas mostra uma página só, sem paginação (edge de 10 janelas)', async () => {
    server.runs = monthRuns('m-1', 10)
    server.locked = new Set()
    server.failedOnly = new Set()
    renderRoute()

    await table()
    await waitFor(() => expect(rows()).toHaveLength(10))
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).toBeNull()
  })

  it('empresa sem runs mostra o aviso com o link para Conciliações, sem tabela (FEC-03 AC2)', async () => {
    server.runs = server.runs.filter(entry => entry.merchantId !== 'm-1')
    renderRoute()

    expect(await screen.findByText('Nenhuma conciliação ainda.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para conciliações' })).toHaveAttribute('href', '/conciliacoes')
    expect(screen.queryByRole('table', { name: 'Períodos' })).toBeNull()
    expect(periodReads()).toEqual([])
  })

  it('erro na busca dos runs mostra a mensagem e "Tentar novamente" refaz a busca (FEC-03 AC3)', async () => {
    server.runsAnswer = () => fail(403, 'Você não tem acesso a este comerciante')
    const user = renderRoute()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Você não tem acesso a este comerciante')
    expect(screen.queryByRole('table', { name: 'Períodos' })).toBeNull()
    server.runsAnswer = undefined
    await user.click(within(alert).getByRole('button', { name: 'Tentar novamente' }))

    await table()
    await waitFor(() => expect(rows()).toHaveLength(4))
  })

  it('uma trava recusada mostra o aviso acima da tabela, e a ação seguinte o limpa (FEC-06 AC1)', async () => {
    server.open = new Set(['2026-10-01/2026-10-31'])
    server.locked = new Set()
    const createObjectURL = vi.fn(() => 'blob:csv')
    vi.stubGlobal('URL', Object.assign(globalThis.URL, { createObjectURL, revokeObjectURL: vi.fn() }))
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const user = renderRoute()
    await table()

    await lockRow(user, 'Outubro de 2026')

    const notice = await screen.findByRole('alert')
    expect(notice).toHaveTextContent('Janela possui divergência em aberto')
    expect(notice.compareDocumentPosition(screen.getByRole('table', { name: 'Períodos' })) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(within(rowOf('Outubro de 2026')).getAllByRole('cell')[1]).toHaveTextContent('Aberto')

    await user.click(within(rowOf('Setembro de 2026')).getByRole('button', { name: 'Exportar CSV de Setembro de 2026' }))
    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull())
    await waitFor(() => expect(createObjectURL).toHaveBeenCalledTimes(1))
  })

  it('travar pela página grava a resposta no cache do período e relê a janela (FEC-05 AC3, AC6)', async () => {
    const user = renderRoute()
    await table()
    const october = { fromDate: '2026-10-01', toDate: '2026-10-31' }
    await waitFor(() => expect(periodReads().filter(key => key === '2026-10-01/2026-10-31')).toHaveLength(1))

    await lockRow(user, 'Outubro de 2026')

    await waitFor(() => expect(within(rowOf('Outubro de 2026')).getAllByRole('cell')[1]).toHaveTextContent('Travado'))
    expect(calls.filter(call => call.path.endsWith('/periods/lock')).map(call => JSON.parse(call.body!))).toEqual([october])
    await waitFor(() => expect(periodReads().filter(key => key === '2026-10-01/2026-10-31')).toHaveLength(2))
    expect(client.getQueryData<Period>(queryKeys.period('m-1', october))).toMatchObject({ locked: true, runId: 'r-oct-2' })
  })

  it('trocar de empresa na página 2 com aviso volta para a página 1 e limpa o aviso (FEC-08 AC1)', async () => {
    server.runs = [...monthRuns('m-1', 12), ...monthRuns('m-2', 12)]
    server.locked = new Set()
    server.failedOnly = new Set()
    server.open = new Set(['2025-12-01/2025-12-31'])
    const user = renderRoute()
    await table()
    await user.click(within(nav()).getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(rowText()[0]).toBe('Dezembro de 2025|Aberto'))
    await lockRow(user, 'Dezembro de 2025')
    expect(await screen.findByRole('alert')).toHaveTextContent('Janela possui divergência em aberto')

    await switchTo(user, 'Alfa Pagamentos', /Beta Comércio/)

    await waitFor(() => expect(within(pageHeader()).getByText('Beta Comércio')).toBeInTheDocument())
    await table()
    await waitFor(() => expect(rows()).toHaveLength(10))
    expect(rowText()[0]).toBe('Outubro de 2026|Aberto')
    expect(within(nav()).getByText('Página 1 de 2')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(calls.filter(call => call.method === 'GET' && call.path === '/api/merchants/m-2/periods')).toHaveLength(10)
  })

  it('voltar para uma empresa já carregada também volta para a página 1 sem o aviso (FEC-08 AC1)', async () => {
    server.runs = [...monthRuns('m-1', 12), ...monthRuns('m-2', 12)]
    server.locked = new Set()
    server.failedOnly = new Set()
    server.open = new Set(['2025-12-01/2025-12-31'])
    const user = renderRoute()
    await table()
    // Both merchants' runs get cached first, so the switch below shows no loading state that would remount the list.
    await switchTo(user, 'Alfa Pagamentos', /Beta Comércio/)
    await waitFor(() => expect(within(pageHeader()).getByText('Beta Comércio')).toBeInTheDocument())
    await table()
    await switchTo(user, 'Beta Comércio', /Alfa Pagamentos/)
    await waitFor(() => expect(within(pageHeader()).getByText('Alfa Pagamentos')).toBeInTheDocument())
    await user.click(within(nav()).getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(rowText()[0]).toBe('Dezembro de 2025|Aberto'))
    await lockRow(user, 'Dezembro de 2025')
    expect(await screen.findByRole('alert')).toHaveTextContent('Janela possui divergência em aberto')

    await switchTo(user, 'Alfa Pagamentos', /Beta Comércio/)

    await waitFor(() => expect(within(pageHeader()).getByText('Beta Comércio')).toBeInTheDocument())
    expect(screen.queryByRole('status', { name: 'Carregando períodos' })).toBeNull()
    expect(within(nav()).getByText('Página 1 de 2')).toBeInTheDocument()
    expect(rowText()[0]).toBe('Outubro de 2026|Aberto')
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('sem empresa no acesso mostra o estado vazio, sem tabela (FEC-08 AC2)', async () => {
    server.merchants = []
    renderRoute()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Fechamento' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
    expect(calls.filter(call => call.path.includes('/reconciliations') || call.path.includes('/periods'))).toEqual([])
  })
})

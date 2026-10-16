import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ConciliacoesPage from '@/app/(app)/conciliacoes/page'
import type { Merchant, Run } from '@/lib/api'
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

const run = (id: string, merchantId: string, fromDate: string, toDate: string, createdAt: string, fields: Partial<Run> = {}): Run => ({
  id, merchantId, fromDate, toDate, status: 'COMPLETED', totalItems: 3, matchedCount: 2, divergentCount: 1, createdAt,
  startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 3, ...fields,
})

/**
 * In-memory API: the top menu lists `merchants`; `runs` holds every merchant's runs, filtered like the backend by
 * merchant and exact window, newest first. Each list answers with `totalPages: 2` so paging is always on.
 */
let server: { merchants: Merchant[]; runs: Run[] }
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })

function route(method: string, path: string, search: URLSearchParams, body?: string): Response {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const list = path.match(/^\/api\/merchants\/([^/]+)\/reconciliations$/)
  if (list && method === 'GET') {
    const content = server.runs
      .filter(item => item.merchantId === list[1]
        && (!search.has('fromDate') || item.fromDate === search.get('fromDate'))
        && (!search.has('toDate') || item.toDate === search.get('toDate')))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return json({ content, totalPages: 2 })
  }
  if (list && method === 'POST') {
    const input = JSON.parse(body!) as { fromDate: string; toDate: string }
    const stored = run(`r-${server.runs.length + 1}`, list[1], input.fromDate, input.toDate, '2026-10-16T13:30:00Z', {
      status: 'PENDING', totalItems: null, matchedCount: null, divergentCount: null, processedItems: null,
    })
    server.runs.push(stored)
    return json(stored, 202)
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 16, 10, 30))
  calls = []
  server = {
    merchants: [alfa, beta],
    runs: [
      run('r-1', 'm-1', '2026-10-01', '2026-10-31', '2026-10-15T12:00:00Z'),
      run('r-0', 'm-1', '2026-10-01', '2026-10-31', '2026-10-14T12:00:00Z', { supersededAt: '2026-10-15T12:00:00Z' }),
      run('r-s', 'm-1', '2026-09-01', '2026-09-30', '2026-10-01T12:00:00Z'),
      run('r-b', 'm-2', '2026-10-01', '2026-10-31', '2026-10-15T12:00:00Z', { totalItems: 9 }),
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
        <ConciliacoesPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const listCalls = () => calls.filter(call => call.method === 'GET' && /\/reconciliations$/.test(call.path))
const lastList = () => listCalls().at(-1)!
const query = (call: Call) => Object.fromEntries(call.search)
const table = () => screen.findByRole('table', { name: 'Conciliações' })
const rows = () => within(screen.getByRole('table', { name: 'Conciliações' })).getAllByRole('row').slice(1)
const cells = (row: HTMLElement) => within(row).getAllByRole('cell').map(cell => cell.textContent)
const ids = () => rows().map(row => within(row).getByRole('link', { name: 'Ver itens' }).getAttribute('href'))
const windowFilter = () => screen.getByLabelText<HTMLSelectElement>('Janela')
/** The page's own title block; the top menu shows the merchant name too, so the name is looked up only here. */
const pageHeader = () => screen.getByRole('heading', { level: 1, name: 'Conciliações' }).parentElement!
const nextPage = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(within(await screen.findByRole('navigation', { name: 'Paginação' })).getByRole('button', { name: 'Próxima' }))
  await waitFor(() => expect(lastList().search.get('page')).toBe('1'))
}
const switchTo = async (user: ReturnType<typeof userEvent.setup>, name: RegExp) => {
  await user.click(screen.getByRole('button', { name: 'Alfa Pagamentos' }))
  await user.click(screen.getByRole('option', { name }))
}

describe('ReconciliationsPage', () => {
  it('a rota busca a página 1 sem janela e mostra o título e o nome da empresa no cabeçalho da página (REC-02 AC1)', async () => {
    renderPage()

    await table()
    expect(within(pageHeader()).getByText('Alfa Pagamentos')).toBeInTheDocument()
    expect(listCalls()).toHaveLength(1)
    expect(lastList().path).toBe('/api/merchants/m-1/reconciliations')
    expect(query(lastList())).toEqual({ page: '0', size: '20', sort: 'createdAt,desc' })
    expect(ids()).toEqual(['/conciliacoes/r-1', '/conciliacoes/r-0', '/conciliacoes/r-s'])
    expect(rows()[1]).toHaveTextContent('Substituído')
    expect(windowFilter()).toHaveValue('')
  })

  it('o filtro Janela envia fromDate e toDate do mês, volta para a página 1 e Todas remove os dois (REC-05 AC7)', async () => {
    const user = renderPage()
    await table()
    const options = within(windowFilter()).getAllByRole('option').map(option => option.textContent)
    expect(options).toHaveLength(13)
    expect(options.slice(0, 3)).toEqual(['Todas', 'Outubro de 2026', 'Setembro de 2026'])
    expect(options[12]).toBe('Novembro de 2025')
    await nextPage(user)

    await user.selectOptions(windowFilter(), 'Setembro de 2026')
    await waitFor(() => expect(ids()).toEqual(['/conciliacoes/r-s']))
    expect(query(lastList())).toEqual({ page: '0', size: '20', sort: 'createdAt,desc', fromDate: '2026-09-01', toDate: '2026-09-30' })

    await user.selectOptions(windowFilter(), 'Todas')
    await waitFor(() => expect(ids()).toHaveLength(3))
    expect(lastList().search.has('fromDate')).toBe(false)
    expect(lastList().search.has('toDate')).toBe(false)
  })

  it('disparo aceito fecha a gaveta, volta para Todas e a página 1 e mostra o run novo no topo como Na fila (REC-07 AC6)', async () => {
    const user = renderPage()
    await table()
    // Filter and page move away from their defaults first, so the reset after the start is visible.
    await user.selectOptions(windowFilter(), 'Setembro de 2026')
    await waitFor(() => expect(ids()).toEqual(['/conciliacoes/r-s']))
    await nextPage(user)

    await user.click(screen.getByRole('button', { name: 'Nova conciliação' }))
    const drawer = screen.getByRole('dialog', { name: 'Nova conciliação' })
    fireEvent.change(within(drawer).getByLabelText('De'), { target: { value: '2026-10-01' } })
    fireEvent.change(within(drawer).getByLabelText('Até'), { target: { value: '2026-10-15' } })
    await user.click(within(drawer).getByRole('button', { name: 'Conciliar' }))

    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Nova conciliação' })).toBeNull())
    expect(calls.find(call => call.method === 'POST')).toMatchObject({
      path: '/api/merchants/m-1/reconciliations', body: '{"fromDate":"2026-10-01","toDate":"2026-10-15"}',
    })
    expect(windowFilter()).toHaveValue('')
    await waitFor(() => expect(ids()[0]).toBe('/conciliacoes/r-5'))
    expect(cells(rows()[0]).slice(0, 3)).toEqual(['01/10/2026 – 15/10/2026', 'Na fila', '—'])
    expect(query(lastList())).toEqual({ page: '0', size: '20', sort: 'createdAt,desc' })
  })

  it('trocar de empresa fecha a gaveta, mantém o mês escolhido e busca a página 1 da nova empresa (REC-11 AC1)', async () => {
    const user = renderPage()
    await table()
    // Every kept value moves away from its default, so a reset of it would show after the switch.
    await user.selectOptions(windowFilter(), 'Outubro de 2026')
    await waitFor(() => expect(ids()).toEqual(['/conciliacoes/r-1', '/conciliacoes/r-0']))
    await nextPage(user)
    await user.click(screen.getByRole('button', { name: 'Nova conciliação' }))
    expect(screen.getByRole('dialog', { name: 'Nova conciliação' })).toBeInTheDocument()

    await switchTo(user, /Beta Comércio/)

    expect(screen.queryByRole('dialog', { name: 'Nova conciliação' })).toBeNull()
    await waitFor(() => expect(lastList().path).toBe('/api/merchants/m-2/reconciliations'))
    expect(query(lastList())).toEqual({ page: '0', size: '20', sort: 'createdAt,desc', fromDate: '2026-10-01', toDate: '2026-10-31' })
    expect(windowFilter()).toHaveValue('2026-10-01')
    expect(within(pageHeader()).getByText('Beta Comércio')).toBeInTheDocument()
    await waitFor(() => expect(ids()).toEqual(['/conciliacoes/r-b']))
  })

  it('sem empresa no acesso mostra o estado vazio, sem filtro, sem lista e sem Nova conciliação (REC-11 AC3)', async () => {
    server.merchants = []
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Janela')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Nova conciliação' })).toBeNull()
    expect(screen.queryByRole('table')).toBeNull()
    expect(listCalls()).toHaveLength(0)
  })
})

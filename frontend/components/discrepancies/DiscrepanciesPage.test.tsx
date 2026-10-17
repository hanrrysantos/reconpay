import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DivergenciasPage from '@/app/(app)/divergencias/page'
import type { Discrepancy, Merchant, ReconciliationItem, Run } from '@/lib/api'
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

const run = (id: string, merchantId: string, fromDate: string, toDate: string, fields: Partial<Run> = {}): Run => ({
  id, merchantId, fromDate, toDate, status: 'COMPLETED', totalItems: 4, matchedCount: 2, divergentCount: 2,
  createdAt: `${toDate}T12:00:00Z`, startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 4, ...fields,
})
const discrepancy = (id: string, type: Discrepancy['type'], status: Discrepancy['status']): Discrepancy =>
  ({ id, type, status, expectedValue: null, actualValue: null })
const item = (id: string, runId: string, reference: string, discrepancies: Discrepancy[], fields: Partial<ReconciliationItem> = {}): ReconciliationItem => ({
  id, reconciliationRunId: runId, externalReference: reference, result: 'DIVERGENT', internalTransactionId: 't-1', externalSettlementId: 's-1',
  transactionAmount: '150.00', expectedNetAmount: '145.22', settlementAmount: '150.00', settlementNetAmount: '145.22', netDifference: '0.00',
  paymentMethod: 'CREDIT_CARD', installments: 3, transactionStatus: 'REFUNDED', settlementStatus: 'APPROVED', transactionDate: '2026-10-02',
  settlementDate: '2026-10-11', discrepancies, createdAt: '2026-10-16T12:00:00Z', ...fields,
})

/**
 * In-memory API like the backend. `runs` lists every run (newest first); `/periods` answers the run's window or 404;
 * `items` filters by `result`, `discrepancyType` and `discrepancyStatus` like the API and always reports 2 pages.
 * With `live`, the latest-run answer follows `liveRuns` (last one repeats) and items appear only after a final status.
 */
let server: { merchants: Merchant[]; runs: Run[]; items: ReconciliationItem[]; locked: Set<string> }
let live: { liveRuns: Run[]; delivered: Run['status'] | null } | null
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })
const windowOf = (search: URLSearchParams) => `${search.get('fromDate')}/${search.get('toDate')}`
const allDiscrepancies = () => server.items.flatMap(entry => entry.discrepancies)

function detail(discrepancyId: string) {
  const found = allDiscrepancies().find(entry => entry.id === discrepancyId)!
  return { id: found.id, type: found.type, expectedValue: null, actualValue: null, status: found.status, adjustments: [], transitions: [] }
}

function route(method: string, path: string, search: URLSearchParams, body?: string): Response {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const merchantId = path.split('/')[3]
  const runsOf = server.runs.filter(entry => entry.merchantId === merchantId)
  if (path.endsWith('/reconciliations') && method === 'GET') {
    if (!search.has('fromDate')) return json({ content: runsOf, totalPages: 1 })
    const inWindow = runsOf.filter(entry => `${entry.fromDate}/${entry.toDate}` === windowOf(search))
    if (live && inWindow.length > 0) {
      const answer = live.liveRuns.length > 1 ? live.liveRuns.shift()! : live.liveRuns[0]
      live.delivered = answer.status
      return json({ content: [answer], totalPages: 1 })
    }
    return json({ content: inWindow, totalPages: inWindow.length ? 1 : 0 })
  }
  if (path.endsWith('/periods')) {
    const owner = runsOf.find(entry => `${entry.fromDate}/${entry.toDate}` === windowOf(search))
    if (!owner) return json({ error: 'NOT_FOUND', message: 'Período não encontrado' }, 404)
    return json({ runId: owner.id, fromDate: owner.fromDate, toDate: owner.toDate, totalItems: 4, matchedCount: 2, divergentCount: 2, matchRate: '0.5000',
      openAmount: '0.00', locked: server.locked.has(windowOf(search)), lockedAt: null, closeDurationSeconds: null })
  }
  const items = path.match(/\/reconciliations\/([^/]+)\/items$/)
  if (items) {
    const finished = !live || live.delivered === 'COMPLETED'
    const status = search.get('discrepancyStatus')
    const type = search.get('discrepancyType')
    const content = !finished ? [] : server.items.filter(entry => entry.reconciliationRunId === items[1]
      && (!search.has('result') || entry.result === search.get('result'))
      && entry.discrepancies.some(found => (!status || found.status === status) && (!type || found.type === type)))
    return json({ content, totalPages: 2 })
  }
  const one = path.match(/\/discrepancies\/([^/]+)$/)
  if (one && method === 'GET') return json(detail(one[1]))
  if (one && method === 'PATCH') {
    allDiscrepancies().find(entry => entry.id === one[1])!.status = (JSON.parse(body!) as { status: Discrepancy['status'] }).status
    return json(detail(one[1]))
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date(2026, 9, 16, 10, 30))
  calls = []
  live = null
  server = {
    merchants: [alfa, beta],
    runs: [
      run('r-oct', 'm-1', '2026-10-01', '2026-10-31'),
      run('r-aug', 'm-1', '2026-08-01', '2026-08-31'),
      run('r-beta', 'm-2', '2026-09-01', '2026-09-30'),
    ],
    items: [
      item('i-1', 'r-oct', 'AVULSA-1', [discrepancy('d-1', 'MISSING_SETTLEMENT', 'OPEN')], {
        externalSettlementId: null, settlementAmount: null, settlementNetAmount: null, settlementStatus: null, settlementDate: null,
        transactionStatus: 'APPROVED', transactionAmount: '100.00', expectedNetAmount: '96.71', netDifference: '-96.71', transactionDate: '2026-10-16',
      }),
      item('i-2', 'r-oct', 'IMP-T-002', [discrepancy('d-2', 'STATUS_MISMATCH', 'OPEN'), discrepancy('d-3', 'FEE_DIVERGENCE', 'ACCEPTED')]),
      item('i-8', 'r-aug', 'AGO-1', [discrepancy('d-8', 'STATUS_MISMATCH', 'OPEN')], { transactionDate: '2026-08-12' }),
      item('i-9', 'r-beta', 'BETA-1', [discrepancy('d-9', 'FEE_DIVERGENCE', 'ACCEPTED')]),
    ],
    locked: new Set(),
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

function renderRoute() {
  render(
    <QueryClientProvider client={createQueryClient(vi.fn())}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <DivergenciasPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const itemCalls = () => calls.filter(call => call.path.endsWith('/items'))
const lastItems = () => itemCalls().at(-1)!
const query = (call: Call) => Object.fromEntries(call.search)
/** The page's own title block; the top menu shows the merchant name too, so the name is looked up only here. */
const pageHeader = () => screen.getByRole('heading', { level: 1, name: 'Divergências' }).parentElement!
const rows = () => within(screen.getByRole('table', { name: 'Divergências' })).getAllByRole('row').slice(1)
  .map(row => { const cells = within(row).getAllByRole('cell'); return `${cells[0].textContent?.split(/[A-Z][a-zé]+ \d|—/)[0]}|${cells[1].textContent}|${cells[4].textContent}` })
const table = () => screen.findByRole('table', { name: 'Divergências' })
const switchTo = async (user: ReturnType<typeof userEvent.setup>, from: string, name: RegExp) => {
  await user.click(screen.getByRole('button', { name: from }))
  await user.click(screen.getByRole('option', { name }))
}

describe('DiscrepanciesPage', () => {
  it('abre na janela do run mais recente e lista as divergências abertas do run (DIV-01 AC1, AC2)', async () => {
    renderRoute()

    await table()
    expect(within(pageHeader()).getByText('Alfa Pagamentos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Outubro de 2026/ })).toBeInTheDocument()
    expect(lastItems().path).toBe('/api/merchants/m-1/reconciliations/r-oct/items')
    expect(query(lastItems())).toEqual({ page: '0', size: '20', sort: 'absDifference,desc', discrepancyStatus: 'OPEN' })
    expect(rows()).toEqual(['AVULSA-1|Liquidação ausente|Aberta', 'IMP-T-002|Status inconsistente|Aberta'])
  })

  it('status, tipo e "Todas" refazem a busca e voltam para a página 1 (DIV-01 AC3, AC4, AC5)', async () => {
    const user = renderRoute()
    await table()
    const next = async () => {
      await user.click(within(screen.getByRole('navigation', { name: 'Paginação' })).getByRole('button', { name: 'Próxima' }))
      await waitFor(() => expect(lastItems().search.get('page')).toBe('1'))
    }

    await next()
    await user.selectOptions(screen.getByLabelText('Status'), 'Aceitas')
    await waitFor(() => expect(query(lastItems())).toMatchObject({ discrepancyStatus: 'ACCEPTED', page: '0' }))
    await waitFor(() => expect(rows()).toEqual(['IMP-T-002|Taxa divergente|Aceita']))

    await next()
    await user.selectOptions(screen.getByLabelText('Status'), 'Todas')
    await waitFor(() => expect(query(lastItems())).toEqual({ page: '0', size: '20', sort: 'absDifference,desc', result: 'DIVERGENT' }))
    await waitFor(() => expect(rows()).toHaveLength(3))

    await next()
    await user.selectOptions(screen.getByLabelText('Tipo'), 'Liquidação ausente')
    await waitFor(() => expect(query(lastItems())).toMatchObject({ result: 'DIVERGENT', discrepancyType: 'MISSING_SETTLEMENT', page: '0' }))
    await waitFor(() => expect(rows()).toEqual(['AVULSA-1|Liquidação ausente|Aberta']))
  })

  it('janela sem run mostra o aviso com o link e não busca itens (DIV-03 AC6, DIV-04 AC1)', async () => {
    const user = renderRoute()
    await table()
    const before = itemCalls().length

    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    await user.click(screen.getByRole('button', { name: /^Setembro de 2026/ }))

    expect(await screen.findByText('Nenhuma conciliação nesta janela.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para conciliações' })).toHaveAttribute('href', '/conciliacoes')
    expect(screen.queryByRole('table', { name: 'Divergências' })).toBeNull()
    expect(itemCalls()).toHaveLength(before)
  })

  it('trocar de janela volta para a página 1 e mantém status e tipo (DIV-03 AC6)', async () => {
    const user = renderRoute()
    await table()
    // Both filters and the page move away from their defaults, so a reset of any of them would show.
    await user.selectOptions(screen.getByLabelText('Status'), 'Todas')
    await user.selectOptions(screen.getByLabelText('Tipo'), 'Status inconsistente')
    await waitFor(() => expect(query(lastItems())).toMatchObject({ result: 'DIVERGENT', discrepancyType: 'STATUS_MISMATCH' }))
    await user.click(within(await screen.findByRole('navigation', { name: 'Paginação' })).getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(lastItems().search.get('page')).toBe('1'))

    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    await user.click(screen.getByRole('button', { name: /^Agosto de 2026/ }))

    await waitFor(() => expect(lastItems().path).toBe('/api/merchants/m-1/reconciliations/r-aug/items'))
    expect(query(lastItems())).toEqual({ page: '0', size: '20', sort: 'absDifference,desc', result: 'DIVERGENT', discrepancyType: 'STATUS_MISMATCH' })
    expect(screen.getByLabelText('Status')).toHaveValue('ALL')
    expect(screen.getByLabelText('Tipo')).toHaveValue('STATUS_MISMATCH')
    await waitFor(() => expect(rows()).toEqual(['AGO-1|Status inconsistente|Aberta']))
  })

  it('empresa sem run abre no mês atual com o aviso de janela sem conciliação (DIV-01 AC1)', async () => {
    vi.setSystemTime(new Date(2026, 10, 5, 9, 0))
    server.runs = server.runs.filter(entry => entry.merchantId !== 'm-1')
    renderRoute()

    expect(await screen.findByText('Nenhuma conciliação nesta janela.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Novembro de 2026/ })).toBeInTheDocument()
    const periodCall = calls.find(call => call.path.endsWith('/periods'))!
    expect(query(periodCall)).toEqual({ fromDate: '2026-11-01', toDate: '2026-11-30' })
  })

  it('run com falha mostra o aviso e a mensagem do run, sem lista (DIV-04 AC3)', async () => {
    server.runs[0] = run('r-oct', 'm-1', '2026-10-01', '2026-10-31', { status: 'FAILED', errorMessage: 'Falha ao processar o lote' })
    renderRoute()

    expect(await screen.findByText('A conciliação desta janela falhou.')).toBeInTheDocument()
    expect(screen.getByText('Falha ao processar o lote')).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Divergências' })).toBeNull()
  })

  it('run em andamento mostra o aviso e, ao concluir, a lista com os itens finais, sem recarregar (DIV-04 AC2)', async () => {
    vi.useRealTimers()
    vi.useFakeTimers({ shouldAdvanceTime: false })
    vi.setSystemTime(new Date(2026, 9, 16, 10, 30))
    // Items commit together with COMPLETED: while the run is in flight the API would list none.
    live = {
      delivered: null,
      liveRuns: [
        run('r-oct', 'm-1', '2026-10-01', '2026-10-31', { status: 'RUNNING', processedItems: 1, matchedCount: null, divergentCount: null }),
        run('r-oct', 'm-1', '2026-10-01', '2026-10-31'),
      ],
    }
    const tick = (ms: number) => act(async () => { await vi.advanceTimersByTimeAsync(ms) })
    renderRoute()

    await tick(200)
    expect(screen.getByRole('status', { name: '' })).toHaveTextContent('Conciliação em andamento.')
    expect(screen.queryByRole('table', { name: 'Divergências' })).toBeNull()

    await tick(3000)
    expect(rows()).toEqual(['AVULSA-1|Liquidação ausente|Aberta', 'IMP-T-002|Status inconsistente|Aberta'])

    const settled = itemCalls().length
    await tick(15000)
    expect(itemCalls()).toHaveLength(settled)
  })

  it('aceitar com nota mostra o novo status na gaveta e, ao fechar, a linha sai das abertas (DIV-06 AC1, AC2, DIV-07 AC5, AC6)', async () => {
    const user = renderRoute()
    await table()

    await user.click(screen.getByRole('button', { name: 'Abrir AVULSA-1 · Liquidação ausente' }))
    const drawer = screen.getByRole('dialog', { name: 'Resolver divergência' })
    expect(within(drawer).getByText('1 de 2 · J / K navega')).toBeInTheDocument()
    await user.click(within(drawer).getByRole('radio', { name: /^Aceitar/ }))
    await user.type(within(drawer).getByLabelText('Nota (opcional)'), 'conferido')
    await user.click(within(drawer).getByRole('button', { name: /^Aceitar/ }))

    await waitFor(() => expect(calls.find(call => call.method === 'PATCH')).toBeDefined())
    const patch = calls.find(call => call.method === 'PATCH')!
    expect(patch.path).toBe('/api/merchants/m-1/reconciliations/r-oct/discrepancies/d-1')
    expect(JSON.parse(patch.body!)).toMatchObject({ status: 'ACCEPTED', note: 'conferido' })
    await waitFor(() => expect(within(screen.getByRole('dialog', { name: 'Resolver divergência' })).getByTestId('drawer-status')).toHaveTextContent('Aceita'))

    await user.click(within(screen.getByRole('dialog', { name: 'Resolver divergência' })).getByRole('button', { name: 'Fechar' }))
    await waitFor(() => expect(rows()).toEqual(['IMP-T-002|Status inconsistente|Aberta']))
  })

  it('J e K andam pelas divergências da página dentro da gaveta (DIV-07 AC7)', async () => {
    const user = renderRoute()
    await table()

    await user.click(screen.getByRole('button', { name: 'Abrir AVULSA-1 · Liquidação ausente' }))
    const drawer = () => screen.getByRole('dialog', { name: 'Resolver divergência' })
    expect(within(drawer()).getByText('1 de 2 · J / K navega')).toBeInTheDocument()
    expect(drawer()).toHaveTextContent('AVULSA-1')

    await user.keyboard('j')
    expect(within(drawer()).getByText('2 de 2 · J / K navega')).toBeInTheDocument()
    expect(drawer()).toHaveTextContent('IMP-T-002')
    expect(drawer()).toHaveTextContent('Status inconsistente')

    await user.keyboard('k')
    expect(within(drawer()).getByText('1 de 2 · J / K navega')).toBeInTheDocument()
    expect(drawer()).toHaveTextContent('AVULSA-1')
  })

  it('no filtro Aceitas, "Reabrir" envia OPEN (DIV-06 AC3)', async () => {
    const user = renderRoute()
    await table()
    await user.selectOptions(screen.getByLabelText('Status'), 'Aceitas')
    await waitFor(() => expect(rows()).toEqual(['IMP-T-002|Taxa divergente|Aceita']))

    await user.click(screen.getByRole('button', { name: 'Abrir IMP-T-002 · Taxa divergente' }))
    await user.click(await within(screen.getByRole('dialog', { name: 'Resolver divergência' })).findByRole('button', { name: 'Reabrir' }))

    await waitFor(() => expect(calls.find(call => call.method === 'PATCH')).toBeDefined())
    const patch = calls.find(call => call.method === 'PATCH')!
    expect(patch.path).toBe('/api/merchants/m-1/reconciliations/r-oct/discrepancies/d-3')
    expect(JSON.parse(patch.body!)).toEqual({ status: 'OPEN' })
  })

  it('período travado mostra o aviso e a gaveta abre sem ações (DIV-05, DIV-06 AC4)', async () => {
    server.locked.add('2026-10-01/2026-10-31')
    const user = renderRoute()
    await table()

    expect(await screen.findByText('Período travado: as divergências ficam só para consulta.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Abrir IMP-T-002 · Status inconsistente' }))
    const drawer = screen.getByRole('dialog', { name: 'Resolver divergência' })
    expect(within(drawer).queryByRole('radio')).toBeNull()
    expect(within(drawer).queryByRole('button', { name: /^(Aceitar|Salvar ajuste|Baixar|Reabrir)/ })).toBeNull()
  })

  it('trocar de empresa fecha a gaveta, vai para a janela da nova empresa e mantém os filtros (DIV-08 AC1)', async () => {
    const user = renderRoute()
    await table()
    // Both filters move away from their defaults, so a reset of either would show after the switch.
    await user.selectOptions(screen.getByLabelText('Status'), 'Aceitas')
    await user.selectOptions(screen.getByLabelText('Tipo'), 'Taxa divergente')
    await waitFor(() => expect(rows()).toEqual(['IMP-T-002|Taxa divergente|Aceita']))
    await user.click(screen.getByRole('button', { name: 'Abrir IMP-T-002 · Taxa divergente' }))
    expect(screen.getByRole('dialog', { name: 'Resolver divergência' })).toBeInTheDocument()

    await switchTo(user, 'Alfa Pagamentos', /Beta Comércio/)

    expect(screen.queryByRole('dialog', { name: 'Resolver divergência' })).toBeNull()
    await waitFor(() => expect(lastItems().path).toBe('/api/merchants/m-2/reconciliations/r-beta/items'))
    expect(query(lastItems())).toEqual({
      page: '0', size: '20', sort: 'absDifference,desc', discrepancyType: 'FEE_DIVERGENCE', discrepancyStatus: 'ACCEPTED',
    })
    expect(screen.getByRole('button', { name: /Setembro de 2026/ })).toBeInTheDocument()
    expect(screen.getByLabelText('Status')).toHaveValue('ACCEPTED')
    expect(screen.getByLabelText('Tipo')).toHaveValue('FEE_DIVERGENCE')
    expect(within(pageHeader()).getByText('Beta Comércio')).toBeInTheDocument()
    await waitFor(() => expect(rows()).toEqual(['BETA-1|Taxa divergente|Aceita']))
  })

  it('voltar a uma empresa já carregada reabre na janela dela, página 1 e sem a gaveta (DIV-08 AC1)', async () => {
    const user = renderRoute()
    await table()
    // Alfa moves to August, page 2 and an open drawer; its runs stay cached while Beta is selected.
    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    await user.click(screen.getByRole('button', { name: /^Agosto de 2026/ }))
    await waitFor(() => expect(rows()).toEqual(['AGO-1|Status inconsistente|Aberta']))
    await user.click(within(screen.getByRole('navigation', { name: 'Paginação' })).getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(query(lastItems())).toMatchObject({ page: '1' }))
    await user.click(screen.getByRole('button', { name: 'Abrir AGO-1 · Status inconsistente' }))
    expect(screen.getByRole('dialog', { name: 'Resolver divergência' })).toBeInTheDocument()

    await switchTo(user, 'Alfa Pagamentos', /Beta Comércio/)
    await waitFor(() => expect(lastItems().path).toBe('/api/merchants/m-2/reconciliations/r-beta/items'))
    expect(query(lastItems())).toMatchObject({ page: '0' })
    await switchTo(user, 'Beta Comércio', /Alfa Pagamentos/)

    await waitFor(() => expect(lastItems().path).toBe('/api/merchants/m-1/reconciliations/r-oct/items'))
    expect(query(lastItems())).toMatchObject({ page: '0' })
    expect(screen.getByRole('button', { name: /Outubro de 2026/ })).toBeInTheDocument()
    expect(screen.queryByRole('dialog', { name: 'Resolver divergência' })).toBeNull()
  })

  it('sem empresa no acesso mostra o estado vazio, sem filtros nem lista (DIV-08 AC2)', async () => {
    server.merchants = []
    renderRoute()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Status')).toBeNull()
    expect(screen.queryByLabelText('Tipo')).toBeNull()
    expect(screen.queryByRole('table')).toBeNull()
    expect(itemCalls()).toHaveLength(0)
  })
})

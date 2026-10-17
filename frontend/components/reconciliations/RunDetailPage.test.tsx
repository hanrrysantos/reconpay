import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ConciliacaoPage from '@/app/(app)/conciliacoes/[runId]/page'
import type { Merchant, ReconciliationItem, Run } from '@/lib/api'
import { formatImportedAt } from '@/lib/imports'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

const navigation = vi.hoisted(() => ({ params: { runId: 'r-1' }, replace: vi.fn() }))
vi.mock('next/navigation', () => ({
  useParams: () => navigation.params,
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ replace: navigation.replace, push: vi.fn() }),
}))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Call = { method: string; path: string; search: URLSearchParams }

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11222333000181' }

const run = (id: string, merchantId: string, fields: Partial<Run> = {}): Run => ({
  id, merchantId, fromDate: '2026-10-01', toDate: '2026-10-31', status: 'COMPLETED', totalItems: 3, matchedCount: 2, divergentCount: 1,
  createdAt: '2026-10-15T12:05:00Z', startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 3, ...fields,
})

const item = (id: string, fields: Partial<ReconciliationItem> = {}): ReconciliationItem => ({
  id, reconciliationRunId: 'r-1', externalReference: `REF-${id}`, result: 'MATCHED', internalTransactionId: 't-1', externalSettlementId: 's-1',
  transactionAmount: '100.00', expectedNetAmount: '95.00', settlementAmount: '100.00', settlementNetAmount: '95.00', netDifference: '0.00',
  paymentMethod: 'CREDIT_CARD', installments: 1, transactionStatus: 'APPROVED', settlementStatus: 'APPROVED', transactionDate: '2026-10-03',
  settlementDate: '2026-10-05', discrepancies: [], createdAt: '2026-10-15T12:05:00Z', ...fields,
})

/**
 * In-memory API like the backend: a run is found only under its own merchant, otherwise `404` in JSON.
 * `runAnswers` overrides the run answer per call (last one repeats); `exportAnswer` overrides the export.
 */
let server: { merchants: Merchant[]; runs: Run[]; items: ReconciliationItem[] }
let runAnswers: Run[]
let exportAnswer: (() => Promise<Response>) | null
let calls: Call[]
/**
 * Like the backend, whose items commit together with COMPLETED: with `live` set, items are empty while the last
 * delivered run answer is PENDING/RUNNING, and each answer waits `runDelay`/`itemsDelay` ms.
 */
let live: { delivered: Run['status'] | null; runDelay: number; itemsDelay: number } | null

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))
const json = (body: unknown, status = 200) => Response.json(body, { status })
const notFound = (runId: string) => json({ error: 'NOT_FOUND', message: `Conciliação não encontrada com id: ${runId}` }, 404)

async function route(method: string, path: string): Promise<Response> {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const match = path.match(/^\/api\/merchants\/([^/]+)\/reconciliations\/([^/]+)(\/items|\/export)?$/)
  if (match && method === 'GET') {
    const [, merchantId, runId, tail] = match
    const stored = server.runs.find(candidate => candidate.id === runId && candidate.merchantId === merchantId)
    if (!stored) return notFound(runId)
    if (tail === '/items') {
      if (!live) return json({ content: server.items, totalPages: 1 })
      const finished = live.delivered === 'COMPLETED' || live.delivered === 'FAILED'
      await wait(live.itemsDelay)
      return json({ content: finished ? server.items : [], totalPages: 1 })
    }
    if (tail === '/export') return exportAnswer ? exportAnswer() : new Response('externalReference;result\nREF-1;MATCHED\n', { headers: { 'Content-Type': 'text/csv' } })
    const answer = runAnswers.length > 1 ? runAnswers.shift()! : runAnswers[0] ?? stored
    if (live) {
      await wait(live.runDelay)
      live.delivered = answer.status
    }
    return json(answer)
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  navigation.params = { runId: 'r-1' }
  navigation.replace.mockReset()
  calls = []
  runAnswers = []
  exportAnswer = null
  live = null
  server = {
    merchants: [alfa, beta],
    runs: [
      run('r-1', 'm-1'),
      run('r-0', 'm-1', { supersededAt: '2026-10-15T13:00:00Z' }),
      run('r-b', 'm-2'),
    ],
    items: [item('1')],
  }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    calls.push({ method, path: url.pathname, search: url.searchParams })
    return route(method, url.pathname)
  }))
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
  vi.useRealTimers()
})

function renderRoute() {
  render(
    <QueryClientProvider client={createQueryClient(vi.fn())}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <ConciliacaoPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

/** The run's own header; the top menu shows the merchant name too, so header texts are looked up only here. */
const header = () => screen.findByRole('region', { name: 'Conciliação' })
const runCalls = () => calls.filter(call => /\/reconciliations\/[^/]+$/.test(call.path))
const itemCalls = () => calls.filter(call => call.path.endsWith('/items'))
const totalsOf = (region: HTMLElement) =>
  Object.fromEntries(within(region).getAllByRole('term').map(term => [term.textContent, term.nextElementSibling?.textContent]))

function stubDownload() {
  const createObjectURL = vi.fn(() => 'blob:csv')
  const revokeObjectURL = vi.fn()
  vi.stubGlobal('URL', Object.assign(globalThis.URL, { createObjectURL, revokeObjectURL }))
  const downloads: string[] = []
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) { downloads.push(this.download) })
  return { createObjectURL, revokeObjectURL, downloads }
}

describe('RunDetailPage', () => {
  it('a rota lê o runId da URL e mostra janela, status, criado em, totais e o link de volta (REC-08 AC1)', async () => {
    renderRoute()

    const region = await header()
    expect(runCalls().map(call => call.path)).toEqual(['/api/merchants/m-1/reconciliations/r-1'])
    expect(within(region).getByText('Alfa Pagamentos')).toBeInTheDocument()
    expect(within(region).getByRole('heading', { level: 1 })).toHaveTextContent(/^Outubro de 2026$/)
    expect(totalsOf(region)).toEqual({
      Status: 'Concluída', 'Criado em': formatImportedAt('2026-10-15T12:05:00Z'), Itens: '3', Conciliados: '2', Divergentes: '1',
    })
    expect(within(region).getByRole('link', { name: 'Voltar para conciliações' })).toHaveAttribute('href', '/conciliacoes')
    expect(within(region).queryByText('Substituído')).toBeNull()
    expect(await screen.findByRole('table', { name: 'Itens' })).toHaveTextContent('REF-1')
    expect(itemCalls()[0].path).toBe('/api/merchants/m-1/reconciliations/r-1/items')
    expect(Object.fromEntries(itemCalls()[0].search)).toEqual({ page: '0', size: '20', sort: 'absDifference,desc' })
    expect(navigation.replace).not.toHaveBeenCalled()
  })

  it('run substituído mostra o selo Substituído no cabeçalho (REC-08 AC1)', async () => {
    navigation.params = { runId: 'r-0' }
    renderRoute()

    const region = await header()
    expect(within(region).getByText('Substituído')).toBeInTheDocument()
  })

  it('404 do run mostra Conciliação não encontrada. e o link de volta (REC-09 AC9)', async () => {
    navigation.params = { runId: 'r-b' }
    renderRoute()

    expect(await screen.findByText('Conciliação não encontrada.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voltar para conciliações' })).toHaveAttribute('href', '/conciliacoes')
    expect(screen.queryByRole('region', { name: 'Conciliação' })).toBeNull()
    expect(screen.queryByRole('table')).toBeNull()
    expect(runCalls()).toHaveLength(1)
  })

  it('erro que não é JSON mostra a mensagem do cliente e Tentar novamente (edge)', async () => {
    vi.mocked(fetch).mockImplementation(async (input: string | URL | Request) => {
      const url = new URL(String(input))
      if (url.pathname === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
      return new Response('Bad Gateway', { status: 502 })
    })
    renderRoute()

    const alert = await screen.findByRole('alert', {}, { timeout: 3000 })
    expect(alert).toHaveTextContent('A API retornou erro 502.')
    expect(within(alert).getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
  })

  it.each([
    ['RUNNING', { status: 'RUNNING', processedItems: 1, totalItems: 3 }],
    ['PENDING', { status: 'PENDING', processedItems: null, totalItems: null }],
    ['FAILED', { status: 'FAILED', errorMessage: 'Falha ao processar o lote' }],
  ] as const)('Exportar CSV não aparece com o run %s (REC-10 AC1)', async (_status, fields) => {
    server.runs[0] = run('r-1', 'm-1', fields)
    renderRoute()

    await header()
    expect(screen.queryByRole('button', { name: 'Exportar CSV' })).toBeNull()
  })

  it('Exportar CSV baixa reconciliation-<runId>.csv, mostrando Exportando… durante o envio (REC-10 AC1–3)', async () => {
    const { createObjectURL, revokeObjectURL, downloads } = stubDownload()
    let answer!: (response: Response) => void
    exportAnswer = () => new Promise(resolve => { answer = resolve })
    const user = renderRoute()

    const region = await header()
    await user.click(within(region).getByRole('button', { name: 'Exportar CSV' }))

    expect(within(region).getByRole('button', { name: 'Exportando…' })).toBeDisabled()
    expect(calls.at(-1)!.path).toBe('/api/merchants/m-1/reconciliations/r-1/export')
    answer(new Response('externalReference;result\nREF-1;MATCHED\n', { headers: { 'Content-Type': 'text/csv' } }))

    await waitFor(() => expect(downloads).toEqual(['reconciliation-r-1.csv']))
    const [blob] = createObjectURL.mock.calls[0] as unknown as [Blob]
    expect(await blob.text()).toBe('externalReference;result\nREF-1;MATCHED\n')
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:csv')
    expect(within(region).getByRole('button', { name: 'Exportar CSV' })).toBeEnabled()
    expect(within(region).queryByRole('alert')).toBeNull()
  })

  it('falha do export mostra a message da API junto do botão (REC-10 AC4)', async () => {
    const { downloads } = stubDownload()
    exportAnswer = async () => notFound('r-1')
    const user = renderRoute()

    const region = await header()
    await user.click(within(region).getByRole('button', { name: 'Exportar CSV' }))

    const button = await within(region).findByRole('button', { name: 'Exportar CSV' })
    const alert = await within(region).findByRole('alert')
    expect(alert).toHaveTextContent('Conciliação não encontrada com id: r-1')
    expect(button.parentElement).toContainElement(alert)
    expect(downloads).toEqual([])
  })

  it('run em andamento recarrega run e itens a cada 3 s até concluir e então mostra Exportar CSV (REC-09 AC8)', async () => {
    vi.useFakeTimers()
    runAnswers = [
      run('r-1', 'm-1', { status: 'RUNNING', processedItems: 1, totalItems: 3, matchedCount: null, divergentCount: null }),
      run('r-1', 'm-1', { status: 'RUNNING', processedItems: 2, totalItems: 3, matchedCount: null, divergentCount: null }),
      run('r-1', 'm-1'),
    ]
    const tick = (ms: number) => act(async () => {
      await vi.advanceTimersByTimeAsync(ms - 2)
      await vi.advanceTimersByTimeAsync(1)
      await vi.advanceTimersByTimeAsync(1)
    })
    renderRoute()

    await tick(10)
    const region = screen.getByRole('region', { name: 'Conciliação' })
    expect(totalsOf(region)).toMatchObject({ Status: 'Em andamento', Itens: '1 de 3' })
    expect(within(region).queryByRole('button', { name: 'Exportar CSV' })).toBeNull()
    const itemsBefore = itemCalls().length
    expect(itemsBefore).toBeGreaterThan(0)

    await tick(3000)
    expect(runCalls()).toHaveLength(2)
    expect(totalsOf(screen.getByRole('region', { name: 'Conciliação' }))).toMatchObject({ Status: 'Em andamento', Itens: '2 de 3' })
    expect(itemCalls().length).toBeGreaterThan(itemsBefore)

    await tick(3000)
    expect(runCalls()).toHaveLength(3)
    const done = screen.getByRole('region', { name: 'Conciliação' })
    expect(totalsOf(done)).toMatchObject({ Status: 'Concluída', Itens: '3' })
    expect(within(done).getByRole('button', { name: 'Exportar CSV' })).toBeInTheDocument()

    const settled = { runs: runCalls().length, items: itemCalls().length }
    await tick(15000)
    expect(runCalls()).toHaveLength(settled.runs)
    expect(itemCalls()).toHaveLength(settled.items)
  })

  it('quando o run conclui, busca os itens de novo e mostra os finais, mesmo com o polling parado (REC-09 AC8)', async () => {
    vi.useFakeTimers()
    // Items reach the API only with COMPLETED, and the items answer is slower than the run answer.
    live = { delivered: null, runDelay: 20, itemsDelay: 60 }
    runAnswers = [
      run('r-1', 'm-1', { status: 'RUNNING', processedItems: 1, totalItems: 1, matchedCount: null, divergentCount: null }),
      run('r-1', 'm-1', { status: 'COMPLETED', processedItems: 1, totalItems: 1, matchedCount: 1, divergentCount: 0 }),
    ]
    const tick = (ms: number) => act(async () => { await vi.advanceTimersByTimeAsync(ms) })
    renderRoute()

    await tick(200)
    expect(totalsOf(screen.getByRole('region', { name: 'Conciliação' }))).toMatchObject({ Status: 'Em andamento' })
    expect(screen.getByText('Nenhum item encontrado para os filtros escolhidos.')).toBeInTheDocument()

    await tick(3000)
    expect(totalsOf(screen.getByRole('region', { name: 'Conciliação' }))).toMatchObject({ Status: 'Concluída' })
    const items = screen.getByRole('table', { name: 'Itens' })
    expect(within(items).getByText('REF-1')).toBeInTheDocument()

    const settled = { runs: runCalls().length, items: itemCalls().length }
    await tick(15000)
    expect(runCalls()).toHaveLength(settled.runs)
    expect(itemCalls()).toHaveLength(settled.items)
  })

  it('run com falha mostra o status e a mensagem do run no cabeçalho, sem Exportar CSV', async () => {
    server.runs[0] = run('r-1', 'm-1', { status: 'FAILED', errorMessage: 'Falha ao processar o lote', matchedCount: null, divergentCount: null })
    renderRoute()

    const region = await header()
    expect(totalsOf(region)).toMatchObject({ Status: 'Falhou' })
    expect(within(region).getByText('Falha ao processar o lote')).toBeInTheDocument()
    expect(within(region).queryByRole('button', { name: 'Exportar CSV' })).toBeNull()
  })

  it('sem empresa no acesso mostra o mesmo estado vazio das outras telas e não busca o run (REC-11 AC3)', async () => {
    server.merchants = []
    renderRoute()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Conciliação' })).toBeNull()
    expect(runCalls()).toHaveLength(0)
    expect(itemCalls()).toHaveLength(0)
  })

  it('trocar de empresa volta para /conciliacoes; o primeiro carregamento não navega (REC-11 AC2)', async () => {
    const user = renderRoute()
    await header()
    expect(navigation.replace).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Alfa Pagamentos' }))
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith('/conciliacoes'))
    expect(navigation.replace).toHaveBeenCalledTimes(1)
    expect(runCalls().every(call => call.path.startsWith('/api/merchants/m-1/'))).toBe(true)
  })
})

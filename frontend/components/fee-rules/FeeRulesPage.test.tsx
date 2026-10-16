import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TaxasPage from '@/app/(app)/taxas/page'
import type { FeeRule, Merchant } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Reply = Response | Error | (() => Response | Promise<Response>)
type Call = { method: string; path: string; search: URLSearchParams; body: string | undefined }

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11222333000181' }

/** A rule as the API stores it; the decimals are the JSON text the API sends (e.g. `2.9900`). */
const stored = (id: string, merchant: Merchant, paymentMethod: FeeRule['paymentMethod'], installments: number, feePercentage: string, fixedFee: string): FeeRule => ({
  id, merchantId: merchant.id, merchantName: merchant.name, paymentMethod, installments, feePercentage, fixedFee, active: true, createdAt: '2026-10-01T12:00:00Z',
})
const alfaPix = stored('r-1', alfa, 'PIX', 1, '0.0000', '0.00')
const alfaCredit = stored('r-2', alfa, 'CREDIT_CARD', 3, '2.9900', '0.30')
const betaBoleto = stored('r-3', beta, 'BOLETO', 1, '1.5000', '3.50')
const betaDebit = stored('r-4', beta, 'DEBIT_CARD', 1, '1.0000', '0.00')

/**
 * In-memory API: `merchants` answers the top menu; `rules` holds every merchant's active rules, listed by
 * payment method and installments like the API's sort. `list` overrides the answer of `GET .../fee-rules`.
 */
let server: { merchants: Merchant[]; rules: FeeRule[]; list: ((merchantId: string) => Reply) | null }
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })
const fail = (status: number, message: string) => json({ error: status === 409 ? 'CONFLICT' : 'ERROR', message }, status)
const page = <T,>(content: T[]) => ({ content, totalPages: 1, totalElements: content.length })
/** Writes the decimals as JSON numbers, as the API does with BigDecimal. */
const ruleJson = (rule: FeeRule) => JSON.stringify({ ...rule, feePercentage: '#P', fixedFee: '#F' })
  .replace('"#P"', rule.feePercentage).replace('"#F"', rule.fixedFee)
const rulesPage = (rules: FeeRule[]) => new Response(`{"content":[${rules.map(ruleJson).join(',')}],"totalPages":1,"totalElements":${rules.length}}`, {
  headers: { 'Content-Type': 'application/json' },
})
const sorted = (rules: FeeRule[]) => [...rules].sort((a, b) => a.paymentMethod.localeCompare(b.paymentMethod) || a.installments - b.installments)

const answer = async (reply: Reply) => {
  if (reply instanceof Error) throw reply
  return typeof reply === 'function' ? reply() : reply
}

async function route(method: string, path: string): Promise<Response> {
  if (path === '/api/me/merchants') return json(page(server.merchants))
  const merchantId = path.match(/^\/api\/merchants\/([^/]+)\/fee-rules$/)?.[1]
  if (merchantId && method === 'GET') {
    return answer(server.list?.(merchantId) ?? rulesPage(sorted(server.rules.filter(rule => rule.merchantId === merchantId))))
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  calls = []
  server = { merchants: [alfa, beta], rules: [alfaPix, alfaCredit, betaBoleto, betaDebit], list: null }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    calls.push({ method, path: url.pathname, search: url.searchParams, body: init.body ? String(init.body) : undefined })
    return route(method, url.pathname)
  }))
})
afterEach(() => vi.unstubAllGlobals())

function renderPage() {
  render(
    <QueryClientProvider client={createQueryClient(vi.fn())}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <TaxasPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const listRequests = () => calls.filter(call => call.method === 'GET' && call.path.endsWith('/fee-rules'))
const rowTexts = () => within(screen.getByRole('table', { name: 'Regras de taxa' })).getAllByRole('row').slice(1)
  .map(row => within(row).getAllByRole('cell').map(cell => cell.textContent))
/** The top menu trigger is the only button with `aria-expanded`. */
const menuButton = () => screen.getByRole('button', { expanded: false })

describe('FeeRulesPage', () => {
  it('busca as regras da empresa selecionada, até 100, por método e parcelas, com título e nome da empresa (FLIST-02)', async () => {
    renderPage()
    expect(screen.getByRole('heading', { level: 1, name: 'Regras de taxa' })).toBeInTheDocument()
    await screen.findByRole('table', { name: 'Regras de taxa' })

    expect(listRequests()).toHaveLength(1)
    expect(listRequests()[0].path).toBe('/api/merchants/m-1/fee-rules')
    expect(listRequests()[0].search.toString()).toBe('page=0&size=100&sort=paymentMethod%2Casc&sort=installments%2Casc')
    expect(listRequests()[0].search.getAll('sort')).toEqual(['paymentMethod,asc', 'installments,asc'])
    const header = screen.getByRole('heading', { level: 1, name: 'Regras de taxa' }).parentElement!
    expect(within(header).getByText('Alfa Pagamentos')).toBeInTheDocument()
  })

  it('mostra método, parcelas, percentual e taxa fixa formatados, Crédito antes de Pix (FLIST-03)', async () => {
    renderPage()
    await screen.findByRole('table', { name: 'Regras de taxa' })

    expect(rowTexts()).toEqual([
      ['Crédito', '3x', '2,99%', 'R$ 0,30'],
      ['Pix', '1x', '0%', 'R$ 0,00'],
    ])
  })

  it('ao trocar a empresa no menu mostra as regras da nova, sem exibir as da anterior (FLIST-04)', async () => {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Regras de taxa' })

    let release: () => void = () => {}
    server.list = merchantId => () => new Promise<Response>(resolve => {
      release = () => resolve(rulesPage(sorted(server.rules.filter(rule => rule.merchantId === merchantId))))
    })
    await user.click(menuButton())
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    expect(await screen.findByRole('status', { name: 'Carregando regras de taxa' })).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Regras de taxa' })).not.toBeInTheDocument()
    expect(screen.queryByText('Crédito')).not.toBeInTheDocument()
    release()

    await waitFor(() => expect(rowTexts()).toEqual([
      ['Boleto', '1x', '1,5%', 'R$ 3,50'],
      ['Débito', '1x', '1%', 'R$ 0,00'],
    ]))
    expect(listRequests().at(-1)!.path).toBe('/api/merchants/m-2/fee-rules')
    const header = screen.getByRole('heading', { level: 1, name: 'Regras de taxa' }).parentElement!
    expect(within(header).getByText('Beta Comércio')).toBeInTheDocument()
  })

  it('sem regras mostra "Nenhuma regra de taxa", o texto e "Cadastrar regra" (FLIST-05)', async () => {
    server.rules = []
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Nenhuma regra de taxa' })).toBeInTheDocument()
    expect(screen.getByText('Transações só entram com uma regra ativa para o método e as parcelas.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cadastrar regra' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('sem empresa no acesso mostra "Nenhuma empresa no seu acesso" com "Cadastrar empresa" (FLIST-06)', async () => {
    server.merchants = []
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cadastrar empresa' })).toHaveAttribute('href', '/empresas?cadastrar=1')
    expect(listRequests()).toHaveLength(0)
  })

  it('mostra o carregamento no lugar da tabela enquanto a lista carrega (FLIST-07)', async () => {
    let release: () => void = () => {}
    server.list = merchantId => () => new Promise<Response>(resolve => {
      release = () => resolve(rulesPage(sorted(server.rules.filter(rule => rule.merchantId === merchantId))))
    })
    renderPage()

    await waitFor(() => expect(listRequests()).toHaveLength(1))
    expect(screen.getByRole('status', { name: 'Carregando regras de taxa' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    release()

    await screen.findByRole('table', { name: 'Regras de taxa' })
    expect(screen.queryByRole('status', { name: 'Carregando regras de taxa' })).not.toBeInTheDocument()
  })

  it('falha na busca mostra a mensagem com "Tentar de novo", que refaz a busca (FLIST-08)', async () => {
    let failing = true
    server.list = merchantId => failing
      ? fail(400, 'Parâmetro de ordenação inválido')
      : rulesPage(sorted(server.rules.filter(rule => rule.merchantId === merchantId)))
    const user = renderPage()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Parâmetro de ordenação inválido')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    failing = false
    await user.click(within(alert).getByRole('button', { name: 'Tentar de novo' }))

    await screen.findByRole('table', { name: 'Regras de taxa' })
    expect(listRequests()).toHaveLength(2)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

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
 * payment method and installments like the API's sort. `list`, `create`, `update` and `remove` override the
 * answers of `GET`, `POST .../fee-rules`, `PUT` and `DELETE .../fee-rules/{id}`. Without an override, `POST` stores
 * the rule (201), or answers 409 when the merchant already has an active rule for the method and installments;
 * `PUT` stores the new rates (200); `DELETE` drops the rule from the active list (204).
 */
let server: {
  merchants: Merchant[]
  rules: FeeRule[]
  list: ((merchantId: string) => Reply) | null
  create: ((merchantId: string, body: string) => Reply) | null
  update: ((ruleId: string, body: string) => Reply) | null
  remove: ((ruleId: string) => Reply) | null
}
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

/** Reads a decimal field as the literal text sent, the way the API reads a BigDecimal. */
const literal = (body: string, key: string) => new RegExp(`"${key}":([^,}]+)`).exec(body)![1]

function store(merchantId: string, body: string): Response {
  const { paymentMethod, installments } = JSON.parse(body) as { paymentMethod: FeeRule['paymentMethod']; installments: number }
  if (server.rules.some(rule => rule.merchantId === merchantId && rule.paymentMethod === paymentMethod && rule.installments === installments)) {
    return fail(409, 'Regra de taxa já existe para este comerciante, método de pagamento e parcelas')
  }
  const merchant = server.merchants.find(item => item.id === merchantId)!
  const created = stored(`r-${server.rules.length + 10}`, merchant, paymentMethod, installments, literal(body, 'feePercentage'), literal(body, 'fixedFee'))
  server.rules = [...server.rules, created]
  return new Response(ruleJson(created), { status: 201, headers: { 'Content-Type': 'application/json' } })
}

function storeFees(ruleId: string, body: string): Response {
  const changed = { ...server.rules.find(rule => rule.id === ruleId)!, feePercentage: literal(body, 'feePercentage'), fixedFee: literal(body, 'fixedFee') }
  server.rules = server.rules.map(rule => rule.id === ruleId ? changed : rule)
  return new Response(ruleJson(changed), { headers: { 'Content-Type': 'application/json' } })
}

function drop(ruleId: string): Response {
  server.rules = server.rules.filter(rule => rule.id !== ruleId)
  return new Response(null, { status: 204 })
}

async function route(method: string, path: string, body: string | undefined): Promise<Response> {
  if (path === '/api/me/merchants') return json(page(server.merchants))
  const merchantId = path.match(/^\/api\/merchants\/([^/]+)\/fee-rules$/)?.[1]
  if (merchantId && method === 'GET') {
    return answer(server.list?.(merchantId) ?? rulesPage(sorted(server.rules.filter(rule => rule.merchantId === merchantId))))
  }
  if (merchantId && method === 'POST') return answer(server.create?.(merchantId, body!) ?? store(merchantId, body!))
  const ruleId = path.match(/^\/api\/merchants\/[^/]+\/fee-rules\/([^/]+)$/)?.[1]
  if (ruleId && method === 'PUT') return answer(server.update?.(ruleId, body!) ?? storeFees(ruleId, body!))
  if (ruleId && method === 'DELETE') return answer(server.remove?.(ruleId) ?? drop(ruleId))
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  calls = []
  server = { merchants: [alfa, beta], rules: [alfaPix, alfaCredit, betaBoleto, betaDebit], list: null, create: null, update: null, remove: null }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    const body = init.body ? String(init.body) : undefined
    calls.push({ method, path: url.pathname, search: url.searchParams, body })
    return route(method, url.pathname, body)
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

const createRequests = () => calls.filter(call => call.method === 'POST' && call.path.endsWith('/fee-rules'))
const listRequests = () => calls.filter(call => call.method === 'GET' && call.path.endsWith('/fee-rules'))
/** Método, Parcelas, Percentual and Taxa fixa of each row; the last cell holds the row's action buttons. */
const rowTexts = () => within(screen.getByRole('table', { name: 'Regras de taxa' })).getAllByRole('row').slice(1)
  .map(row => within(row).getAllByRole('cell').slice(0, 4).map(cell => cell.textContent))
const ruleRow = (label: string) => screen.getByRole('row', { name: new RegExp(`^${label} `) })
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

describe('cadastro de regra', () => {
  const drawer = () => screen.getByRole('dialog', { name: 'Cadastrar regra de taxa' })
  const methodField = () => within(drawer()).getByLabelText('Método')
  const installmentsField = () => within(drawer()).getByLabelText('Parcelas')
  const percentField = () => within(drawer()).getByLabelText('Percentual')
  const fixedFeeField = () => within(drawer()).getByLabelText('Taxa fixa')

  /** Alfa starts with Pix 1x only, so Crédito 3x is new. */
  async function openDrawer() {
    server.rules = [alfaPix, betaBoleto]
    const user = renderPage()
    await screen.findByRole('table', { name: 'Regras de taxa' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar regra' }))
    return user
  }

  async function fill(user: ReturnType<typeof userEvent.setup>, { method, installments, percent, fixedFee }: { method?: string; installments?: string; percent?: string; fixedFee?: string }) {
    if (method) await user.selectOptions(methodField(), method)
    if (installments !== undefined) { await user.clear(installmentsField()); if (installments) await user.type(installmentsField(), installments) }
    if (percent) await user.type(percentField(), percent)
    if (fixedFee) await user.type(fixedFeeField(), fixedFee)
  }

  it('"Cadastrar regra" abre a gaveta com Método, Parcelas, Percentual e Taxa fixa, sem método e com 1 parcela (FCAD-01)', async () => {
    await openDrawer()

    expect(drawer()).toBeInTheDocument()
    expect(methodField()).toHaveValue('')
    expect(within(methodField() as HTMLElement).getByRole('option', { selected: true })).toHaveTextContent('Escolha o método')
    expect(within(methodField() as HTMLElement).getAllByRole('option').map(option => option.textContent)).toEqual(['Escolha o método', 'Crédito', 'Débito', 'Pix', 'Boleto'])
    expect(installmentsField()).toHaveValue('1')
    expect(percentField()).toHaveValue('')
    expect(fixedFeeField()).toHaveValue('')
  })

  it('o estado vazio também abre a gaveta (FCAD-01, FLIST-05)', async () => {
    server.rules = []
    const user = renderPage()
    await user.click(await screen.findByRole('button', { name: 'Cadastrar regra' }))

    expect(drawer()).toBeInTheDocument()
  })

  it.each([['Pix'], ['Boleto'], ['Débito']])('com %s, Parcelas fica em 1 e desabilitado (FCAD-02)', async method => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3' })
    await user.selectOptions(methodField(), method)

    expect(installmentsField()).toHaveValue('1')
    expect(installmentsField()).toBeDisabled()
  })

  it('com Crédito, Parcelas aceita qualquer inteiro a partir de 1 (FCAD-03)', async () => {
    const user = await openDrawer()
    await user.selectOptions(methodField(), 'Pix')
    await user.selectOptions(methodField(), 'Crédito')

    expect(installmentsField()).toBeEnabled()
    await fill(user, { installments: '12', percent: '3,49', fixedFee: '0' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(createRequests()).toHaveLength(1))
    expect(JSON.parse(createRequests()[0].body!)).toMatchObject({ paymentMethod: 'CREDIT_CARD', installments: 12 })
  })

  it('envia método, parcelas inteiras e os decimais como números JSON com o valor digitado (FCAD-04)', async () => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3', percent: '2,99', fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(createRequests()).toHaveLength(1))
    expect(createRequests()[0].path).toBe('/api/merchants/m-1/fee-rules')
    expect(createRequests()[0].body).toBe('{"paymentMethod":"CREDIT_CARD","installments":3,"feePercentage":2.99,"fixedFee":0.30}')
  })

  it('com 201 fecha a gaveta e mostra a nova regra na lista (FCAD-05)', async () => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3', percent: '2,99', fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(rowTexts()).toEqual([
      ['Crédito', '3x', '2,99%', 'R$ 0,30'],
      ['Pix', '1x', '0%', 'R$ 0,00'],
    ])
  })

  it('sem método bloqueia o envio e mostra "Escolha o método de pagamento" (FCAD-06)', async () => {
    const user = await openDrawer()
    await fill(user, { percent: '2,99', fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    expect(within(drawer()).getByText('Escolha o método de pagamento')).toBeInTheDocument()
    expect(methodField()).toHaveAttribute('aria-invalid', 'true')
    expect(createRequests()).toHaveLength(0)
  })

  it.each([['0'], ['1,5'], ['abc'], ['']])('Parcelas "%s" bloqueia o envio com a mensagem (FCAD-07)', async installments => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments, percent: '2,99', fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    expect(within(drawer()).getByText('Informe um número inteiro de parcelas a partir de 1')).toBeInTheDocument()
    expect(installmentsField()).toHaveAttribute('aria-invalid', 'true')
    expect(createRequests()).toHaveLength(0)
  })

  it.each([[''], ['abc'], ['-1'], ['100,01'], ['2,99999']])('Percentual "%s" bloqueia o envio com a mensagem (FCAD-08)', async percent => {
    const user = await openDrawer()
    await fill(user, { method: 'Pix', percent, fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    expect(within(drawer()).getByText('Informe um percentual de 0 a 100 com até 4 casas decimais')).toBeInTheDocument()
    expect(percentField()).toHaveAttribute('aria-invalid', 'true')
    expect(createRequests()).toHaveLength(0)
  })

  it.each([[''], ['x'], ['-0,01'], ['0,301']])('Taxa fixa "%s" bloqueia o envio com a mensagem (FCAD-09)', async fixedFee => {
    const user = await openDrawer()
    await fill(user, { method: 'Boleto', percent: '2,99', fixedFee })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    expect(within(drawer()).getByText('Informe uma taxa fixa a partir de 0 com até 2 casas decimais')).toBeInTheDocument()
    expect(fixedFeeField()).toHaveAttribute('aria-invalid', 'true')
    expect(createRequests()).toHaveLength(0)
  })

  it('com 409 mantém a gaveta aberta com os valores digitados e mostra a mensagem (FCAD-10)', async () => {
    const user = await openDrawer()
    await fill(user, { method: 'Pix', percent: '2,99', fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    expect(await within(drawer()).findByRole('alert')).toHaveTextContent('Regra de taxa já existe para este comerciante, método de pagamento e parcelas')
    expect(methodField()).toHaveValue('PIX')
    expect(installmentsField()).toHaveValue('1')
    expect(percentField()).toHaveValue('2,99')
    expect(fixedFeeField()).toHaveValue('0,30')
  })

  it.each([
    ['outro erro da API', () => fail(500, 'Erro interno ao cadastrar'), 'Erro interno ao cadastrar'],
    ['falha de rede', () => new TypeError('Failed to fetch'), 'Não foi possível conectar à API. Tente novamente.'],
  ] as const)('com %s mantém a gaveta aberta com os valores e mostra a mensagem (FCAD-10)', async (_case, reply, message) => {
    server.create = () => { const result = reply(); if (result instanceof Error) throw result; return result }
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3', percent: '2,99', fixedFee: '0,30' })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    expect(await within(drawer()).findByRole('alert')).toHaveTextContent(message)
    expect(methodField()).toHaveValue('CREDIT_CARD')
    expect(installmentsField()).toHaveValue('3')
    expect(percentField()).toHaveValue('2,99')
    expect(fixedFeeField()).toHaveValue('0,30')
  })

  it('durante o envio desabilita "Cadastrar" e cliques repetidos geram um único POST (FCAD-11)', async () => {
    let release: () => void = () => {}
    server.create = (merchantId, body) => () => new Promise<Response>(resolve => { release = () => resolve(store(merchantId, body)) })
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3', percent: '2,99', fixedFee: '0,30' })
    const submit = within(drawer()).getByRole('button', { name: 'Cadastrar' })
    await user.click(submit)
    await user.click(submit)
    await user.dblClick(submit)

    expect(submit).toBeDisabled()
    expect(createRequests()).toHaveLength(1)
    release()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(createRequests()).toHaveLength(1)
  })

  it.each([['Esc', '{Escape}'], ['Cancelar', null]] as const)('%s fecha a gaveta sem chamar a API (FCAD-12)', async (_case, key) => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3', percent: '2,99', fixedFee: '0,30' })
    if (key) await user.keyboard(key)
    else await user.click(within(drawer()).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(createRequests()).toHaveLength(0)
  })

  it.each([
    ['0 e 0 (regra sem taxa)', '0', '0', '"feePercentage":0,"fixedFee":0'],
    ['percentual com 4 casas', '2,9999', '0,30', '"feePercentage":2.9999,"fixedFee":0.30'],
    ['percentual 100', '100', '0,30', '"feePercentage":100,"fixedFee":0.30'],
  ])('aceita %s', async (_case, percent, fixedFee, decimals) => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '6', percent, fixedFee })
    await user.click(within(drawer()).getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(createRequests()).toHaveLength(1))
    expect(createRequests()[0].body).toBe(`{"paymentMethod":"CREDIT_CARD","installments":6,${decimals}}`)
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('trocar de empresa no menu com a gaveta aberta fecha a gaveta sem chamar a API', async () => {
    const user = await openDrawer()
    await fill(user, { method: 'Crédito', installments: '3', percent: '2,99', fixedFee: '0,30' })
    await user.click(menuButton())
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(createRequests()).toHaveLength(0)
    await user.click(screen.getByRole('button', { expanded: false }))
    await user.click(screen.getByRole('option', { name: /Alfa Pagamentos/ }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})

describe('alterar taxas', () => {
  const puts = () => calls.filter(call => call.method === 'PUT')
  const drawer = () => screen.getByRole('dialog', { name: 'Editar regra de taxa' })
  const field = (label: string) => within(drawer()).getByLabelText(label)
  const saveButton = () => within(drawer()).getByRole('button', { name: 'Salvar' })

  /** Opens "Editar" on Alfa's Crédito 3x (2.9900 and 0.30). */
  async function openEdit() {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Regras de taxa' })
    await user.click(within(ruleRow('Crédito')).getByRole('button', { name: 'Editar' }))
    return user
  }

  async function change(user: ReturnType<typeof userEvent.setup>, percent: string, fixedFee: string) {
    await user.clear(field('Percentual'))
    if (percent) await user.type(field('Percentual'), percent)
    await user.clear(field('Taxa fixa'))
    if (fixedFee) await user.type(field('Taxa fixa'), fixedFee)
    await user.click(saveButton())
  }

  it('"Editar" abre a gaveta com Método e Parcelas somente leitura e as taxas atuais com vírgula (FEDIT-01)', async () => {
    await openEdit()

    expect(field('Método')).toHaveValue('Crédito')
    expect(field('Método')).toHaveAttribute('readonly')
    expect(field('Parcelas')).toHaveValue('3')
    expect(field('Parcelas')).toHaveAttribute('readonly')
    expect(field('Percentual')).toHaveValue('2,99')
    expect(field('Percentual')).not.toHaveAttribute('readonly')
    expect(field('Taxa fixa')).toHaveValue('0,30')
    expect(field('Taxa fixa')).not.toHaveAttribute('readonly')
  })

  it('salvar chama PUT /api/merchants/{id}/fee-rules/{ruleId} só com feePercentage e fixedFee como números JSON digitados (FEDIT-02)', async () => {
    const user = await openEdit()
    await change(user, '3,49', '0,35')

    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0].path).toBe('/api/merchants/m-1/fee-rules/r-2')
    expect(puts()[0].body).toBe('{"feePercentage":3.49,"fixedFee":0.35}')
  })

  it('200 fecha a gaveta e mostra os novos valores na lista (FEDIT-03)', async () => {
    const user = await openEdit()
    await change(user, '3,49', '0,35')

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(rowTexts()).toEqual([
      ['Crédito', '3x', '3,49%', 'R$ 0,35'],
      ['Pix', '1x', '0%', 'R$ 0,00'],
    ]))
  })

  it.each([
    ['Percentual "100,01"', '100,01', '0,30', 'Percentual', 'Informe um percentual de 0 a 100 com até 4 casas decimais'],
    ['Percentual vazio', '', '0,30', 'Percentual', 'Informe um percentual de 0 a 100 com até 4 casas decimais'],
    ['Taxa fixa "0,301"', '2,99', '0,301', 'Taxa fixa', 'Informe uma taxa fixa a partir de 0 com até 2 casas decimais'],
    ['Taxa fixa negativa', '2,99', '-1', 'Taxa fixa', 'Informe uma taxa fixa a partir de 0 com até 2 casas decimais'],
  ])('%s bloqueia o envio sem chamar a API, com a mensagem do cadastro (FEDIT-04)', async (_case, percent, fixedFee, label, message) => {
    const user = await openEdit()
    await change(user, percent, fixedFee)

    expect(within(drawer()).getByText(message)).toBeInTheDocument()
    expect(field(label)).toHaveAttribute('aria-invalid', 'true')
    expect(puts()).toHaveLength(0)
  })

  it.each([
    ['erro da API', () => fail(500, 'Erro interno ao alterar'), 'Erro interno ao alterar'],
    ['falha de rede', () => new TypeError('Failed to fetch'), 'Não foi possível conectar à API. Tente novamente.'],
  ] as const)('com %s mantém a gaveta aberta com os valores digitados e mostra a mensagem (FEDIT-05)', async (_case, reply, message) => {
    server.update = () => { const result = reply(); if (result instanceof Error) throw result; return result }
    const user = await openEdit()
    await change(user, '3,49', '0,35')

    expect(await within(drawer()).findByRole('alert')).toHaveTextContent(message)
    expect(field('Percentual')).toHaveValue('3,49')
    expect(field('Taxa fixa')).toHaveValue('0,35')
  })

  it('durante o envio desabilita "Salvar" e gera um único PUT (FEDIT-06)', async () => {
    let release: () => void = () => {}
    server.update = (ruleId, body) => () => new Promise<Response>(resolve => { release = () => resolve(storeFees(ruleId, body)) })
    const user = await openEdit()
    await change(user, '3,49', '0,35')

    expect(saveButton()).toBeDisabled()
    await user.click(saveButton())
    expect(puts()).toHaveLength(1)
    release()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(puts()).toHaveLength(1)
  })

  it('404 ao editar mostra a mensagem e recarrega a lista (edge case)', async () => {
    server.update = ruleId => {
      server.rules = server.rules.filter(rule => rule.id !== ruleId)
      return fail(404, `Regra de taxa não encontrada com id: ${ruleId}`)
    }
    const user = await openEdit()
    const before = listRequests().length
    await change(user, '3,49', '0,35')

    expect(await within(drawer()).findByRole('alert')).toHaveTextContent('Regra de taxa não encontrada com id: r-2')
    await waitFor(() => expect(rowTexts()).toEqual([['Pix', '1x', '0%', 'R$ 0,00']]))
    expect(listRequests().length).toBeGreaterThan(before)
  })

  it('trocar de empresa no menu com a gaveta de edição aberta fecha a gaveta sem chamar a API (edge case)', async () => {
    const user = await openEdit()
    await user.clear(field('Percentual'))
    await user.type(field('Percentual'), '3,49')
    await user.click(menuButton())
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(puts()).toHaveLength(0)
    await waitFor(() => expect(rowTexts()).toEqual([
      ['Boleto', '1x', '1,5%', 'R$ 3,50'],
      ['Débito', '1x', '1%', 'R$ 0,00'],
    ]))
  })
})

describe('desativar regra', () => {
  const deletes = () => calls.filter(call => call.method === 'DELETE')
  const dialog = () => screen.getByRole('alertdialog')
  const confirmButton = () => within(dialog()).getByRole('button', { name: 'Desativar' })

  /** Opens "Desativar" on one of Alfa's rules (Crédito 3x or Pix 1x). */
  async function openDialog(label = 'Crédito') {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Regras de taxa' })
    await user.click(within(ruleRow(label)).getByRole('button', { name: 'Desativar' }))
    return user
  }

  it.each([
    ['Crédito', 'Desativar a regra Crédito 3x?'],
    ['Pix', 'Desativar a regra Pix 1x?'],
  ])('"Desativar" em %s abre a confirmação com método, parcelas e o aviso das transações recusadas (FDES-01)', async (label, title) => {
    await openDialog(label)

    expect(dialog()).toHaveAccessibleName(title)
    expect(dialog()).toHaveAccessibleDescription('Novas transações dessa combinação serão recusadas até existir outra regra.')
    expect(deletes()).toHaveLength(0)
  })

  it('confirmar chama DELETE /api/merchants/{id}/fee-rules/{ruleId} (FDES-02)', async () => {
    const user = await openDialog()
    await user.click(confirmButton())

    await waitFor(() => expect(deletes()).toHaveLength(1))
    expect(deletes()[0].path).toBe('/api/merchants/m-1/fee-rules/r-2')
    expect(deletes()[0].body).toBeUndefined()
  })

  it('204 fecha o diálogo e tira a regra da lista (FDES-03)', async () => {
    const user = await openDialog()
    await user.click(confirmButton())

    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    await waitFor(() => expect(rowTexts()).toEqual([['Pix', '1x', '0%', 'R$ 0,00']]))
  })

  it.each([
    ['Cancelar', (user: ReturnType<typeof userEvent.setup>) => user.click(within(dialog()).getByRole('button', { name: 'Cancelar' }))],
    ['Esc', (user: ReturnType<typeof userEvent.setup>) => user.keyboard('{Escape}')],
  ])('%s fecha o diálogo sem chamar a API (FDES-04)', async (_case, close) => {
    const user = await openDialog()
    await close(user)

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(deletes()).toHaveLength(0)
    expect(rowTexts()).toEqual([
      ['Crédito', '3x', '2,99%', 'R$ 0,30'],
      ['Pix', '1x', '0%', 'R$ 0,00'],
    ])
  })

  it.each([
    ['erro da API', () => fail(500, 'Erro interno ao desativar'), 'Erro interno ao desativar'],
    ['falha de rede', () => new TypeError('Failed to fetch'), 'Não foi possível conectar à API. Tente novamente.'],
  ] as const)('com %s mantém o diálogo aberto e mostra a mensagem (FDES-05)', async (_case, reply, message) => {
    server.remove = () => { const result = reply(); if (result instanceof Error) throw result; return result }
    const user = await openDialog()
    await user.click(confirmButton())

    expect(await within(dialog()).findByRole('alert')).toHaveTextContent(message)
    expect(dialog()).toHaveAccessibleName('Desativar a regra Crédito 3x?')
  })

  it('404 ao desativar mostra a mensagem e recarrega a lista (edge case)', async () => {
    server.remove = ruleId => {
      server.rules = server.rules.filter(rule => rule.id !== ruleId)
      return fail(404, `Regra de taxa não encontrada com id: ${ruleId}`)
    }
    const user = await openDialog()
    const before = listRequests().length
    await user.click(confirmButton())

    expect(await within(dialog()).findByRole('alert')).toHaveTextContent('Regra de taxa não encontrada com id: r-2')
    await waitFor(() => expect(rowTexts()).toEqual([['Pix', '1x', '0%', 'R$ 0,00']]))
    expect(listRequests().length).toBeGreaterThan(before)
  })

  it('trocar de empresa no menu com o diálogo aberto fecha o diálogo sem chamar a API (edge case)', async () => {
    const user = await openDialog()
    await user.click(menuButton())
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(deletes()).toHaveLength(0)
  })
})

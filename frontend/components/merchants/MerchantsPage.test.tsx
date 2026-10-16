import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import EmpresasPage from '@/app/(app)/empresas/page'
import type { MerchantRecord } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

const navigation = vi.hoisted(() => ({ search: new URLSearchParams(), replace: vi.fn() }))
vi.mock('next/navigation', () => ({ useSearchParams: () => navigation.search, useRouter: () => ({ replace: navigation.replace }) }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Reply = Response | Error | (() => Response | Promise<Response>)
type Call = { method: string; path: string; search: URLSearchParams; body: unknown }

const record = (id: string, name: string, document: string): MerchantRecord => ({ id, name, document, active: true, createdAt: '2026-10-01T12:00:00Z' })
const alfa = record('m-1', 'Alfa Pagamentos', '12345678000195')
const beta = record('m-2', 'Beta Comércio', '11222333000181')

/**
 * In-memory API: `merchants` answers the top menu and, by default, `GET /api/merchants`; `list`, `create`, `rename`
 * and `deactivate` override those answers. Without an override, `POST` stores the merchant (201), `PUT` renames it
 * (200) and `DELETE` removes it (204).
 */
let server: {
  merchants: MerchantRecord[]
  list: ((search: URLSearchParams) => Reply) | null
  create: ((body: { name: string; document: string }) => Reply) | null
  rename: ((merchantId: string, body: { name: string }) => Reply) | null
  deactivate: ((merchantId: string) => Reply) | null
}
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })
const fail = (status: number, message: string) => json({ error: status === 409 ? 'CONFLICT' : 'ERROR', message }, status)
const page = <T,>(content: T[], totalPages = 1) => ({ content, totalPages, totalElements: content.length })

const answer = async (reply: Reply) => {
  if (reply instanceof Error) throw reply
  return typeof reply === 'function' ? reply() : reply
}
const byName = (merchants: MerchantRecord[]) => [...merchants].sort((a, b) => a.name.localeCompare(b.name))

function store(body: { name: string; document: string }): Response {
  const created = record(`m-${server.merchants.length + 1}`, body.name, body.document)
  server.merchants = byName([...server.merchants, created])
  return json(created, 201)
}

function storeName(merchantId: string, name: string): Response {
  server.merchants = byName(server.merchants.map(merchant => merchant.id === merchantId ? { ...merchant, name } : merchant))
  return json(server.merchants.find(merchant => merchant.id === merchantId))
}

function remove(merchantId: string): Response {
  server.merchants = server.merchants.filter(merchant => merchant.id !== merchantId)
  return new Response(null, { status: 204 })
}

async function route(method: string, path: string, search: URLSearchParams, body: unknown): Promise<Response> {
  if (path === '/api/me/merchants') return json(page(server.merchants.map(({ id, name, document }) => ({ id, name, document }))))
  if (path === '/api/merchants' && method === 'GET') return answer(server.list?.(search) ?? json(page(server.merchants)))
  if (path === '/api/merchants' && method === 'POST') {
    const input = body as { name: string; document: string }
    return answer(server.create?.(input) ?? store(input))
  }
  const merchantId = path.match(/^\/api\/merchants\/([^/]+)$/)?.[1]
  if (merchantId && method === 'PUT') {
    const input = body as { name: string }
    return answer(server.rename?.(merchantId, input) ?? storeName(merchantId, input.name))
  }
  if (merchantId && method === 'DELETE') return answer(server.deactivate?.(merchantId) ?? remove(merchantId))
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  calls = []
  server = { merchants: [alfa, beta], list: null, create: null, rename: null, deactivate: null }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    const body = init.body ? JSON.parse(String(init.body)) : undefined
    calls.push({ method, path: url.pathname, search: url.searchParams, body })
    return route(method, url.pathname, url.searchParams, body)
  }))
})
afterEach(() => {
  vi.unstubAllGlobals()
  navigation.search = new URLSearchParams()
  navigation.replace.mockReset()
})

function renderPage() {
  render(
    <QueryClientProvider client={createQueryClient(vi.fn())}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <EmpresasPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const listRequests = () => calls.filter(call => call.method === 'GET' && call.path === '/api/merchants')
const row = (name: string) => screen.getByRole('row', { name: new RegExp(name) })
/** The top menu trigger is the only button with `aria-expanded`. */
const menuButton = () => screen.getByRole('button', { expanded: false })

describe('MerchantsPage', () => {
  it('busca a primeira página por nome e lista nome, CNPJ formatado e data de cadastro (MLIST-01)', async () => {
    renderPage()
    expect(screen.getByRole('heading', { level: 1, name: 'Empresas' })).toBeInTheDocument()
    const alfaRow = await screen.findByRole('row', { name: /Alfa Pagamentos/ })

    const request = listRequests()[0]
    expect(request.search.toString()).toBe('page=0&size=20&sort=name%2Casc')
    expect(request.search.get('sort')).toBe('name,asc')
    expect(within(alfaRow).getByText('12.345.678/0001-95')).toBeInTheDocument()
    expect(within(alfaRow).getByText('01/10/2026')).toBeInTheDocument()
    expect(within(row('Beta Comércio')).getByText('11.222.333/0001-81')).toBeInTheDocument()
    const names = within(screen.getByRole('table', { name: 'Empresas' })).getAllByRole('row').slice(1).map(item => item.textContent)
    expect(names).toEqual([expect.stringContaining('Alfa Pagamentos'), expect.stringContaining('Beta Comércio')])
  })

  it('marca a empresa selecionada no menu do topo (MLIST-02)', async () => {
    renderPage()
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    await waitFor(() => expect(row('Alfa Pagamentos')).toHaveAttribute('aria-current', 'true'))
    expect(within(row('Alfa Pagamentos')).getByText('Selecionada')).toBeInTheDocument()
    expect(row('Beta Comércio')).not.toHaveAttribute('aria-current')
    expect(within(row('Beta Comércio')).queryByText('Selecionada')).not.toBeInTheDocument()
  })

  it('com mais de uma página mostra Anterior e Próxima, desabilitados nas pontas (MLIST-03)', async () => {
    server.list = search => json(page(search.get('page') === '0' ? [alfa] : [beta], 2))
    const user = renderPage()
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Próxima' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await screen.findByRole('row', { name: /Beta Comércio/ })
    expect(listRequests().at(-1)!.search.get('page')).toBe('1')
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Próxima' })).toBeDisabled()
  })

  it('com uma página só não mostra a paginação (MLIST-03)', async () => {
    renderPage()
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    expect(screen.queryByRole('button', { name: 'Anterior' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Próxima' })).not.toBeInTheDocument()
  })

  it('clicar no nome torna a empresa a selecionada no menu do topo (MLIST-04)', async () => {
    const user = renderPage()
    await screen.findByRole('row', { name: /Beta Comércio/ })
    await waitFor(() => expect(menuButton()).toHaveTextContent('Alfa Pagamentos'))

    await user.click(within(row('Beta Comércio')).getByRole('button', { name: 'Beta Comércio' }))
    expect(menuButton()).toHaveTextContent('Beta Comércio')
    expect(row('Beta Comércio')).toHaveAttribute('aria-current', 'true')
  })

  it('lista vazia mostra "Nenhuma empresa no seu acesso" com "Cadastrar empresa" (MLIST-05)', async () => {
    server.merchants = []
    renderPage()
    const heading = await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })
    expect(within(heading.closest('section')!).getByRole('button', { name: 'Cadastrar empresa' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('mostra o carregamento no lugar da tabela enquanto a lista carrega (MLIST-06)', async () => {
    let answer!: () => void
    server.list = () => () => new Promise<Response>(resolve => { answer = () => resolve(json(page([alfa]))) })
    renderPage()
    expect(await screen.findByRole('status', { name: 'Carregando empresas' })).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    answer()
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    expect(screen.queryByRole('status', { name: 'Carregando empresas' })).not.toBeInTheDocument()
  })

  it('falha na busca mostra a mensagem com "Tentar de novo", que refaz a busca (MLIST-07)', async () => {
    let failing = true
    server.list = () => failing ? fail(400, 'Parâmetro de ordenação inválido') : json(page([alfa]))
    const user = renderPage()
    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Parâmetro de ordenação inválido')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    failing = false
    await user.click(within(alert).getByRole('button', { name: 'Tentar de novo' }))
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    expect(listRequests()).toHaveLength(2)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('falha de rede na busca mostra a mensagem de conexão com "Tentar de novo" (MLIST-07)', async () => {
    let failing = true
    server.list = () => failing ? new TypeError('Failed to fetch') : json(page([alfa]))
    const user = renderPage()
    // Network errors get the query client's single retry (about 1 s) before the error state appears.
    const alert = await screen.findByRole('alert', {}, { timeout: 4000 })
    expect(alert).toHaveTextContent('Não foi possível conectar à API. Tente novamente.')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()

    failing = false
    await user.click(within(alert).getByRole('button', { name: 'Tentar de novo' }))
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('documento gravado sem 14 dígitos aparece como está, sem máscara (MLIST-08)', async () => {
    server.merchants = [record('m-9', 'Legado Ltda', 'ABC-123')]
    renderPage()
    const legacy = await screen.findByRole('row', { name: /Legado Ltda/ })
    expect(within(legacy).getByText('ABC-123')).toBeInTheDocument()
  })
})

describe('Cadastrar empresa', () => {
  it('chegando com ?cadastrar=1 a gaveta já abre e o parâmetro sai da URL (MCAD-11)', async () => {
    navigation.search = new URLSearchParams('cadastrar=1')
    renderPage()
    expect(await screen.findByRole('dialog', { name: 'Cadastrar empresa' })).toBeInTheDocument()
    expect(navigation.replace).toHaveBeenCalledWith('/empresas')
  })

  const posts = () => calls.filter(call => call.method === 'POST' && call.path === '/api/merchants')
  const drawer = () => screen.getByRole('dialog', { name: 'Cadastrar empresa' })
  const nameField = () => within(drawer()).getByLabelText('Nome')
  const cnpjField = () => within(drawer()).getByLabelText('CNPJ')
  const submitButton = () => within(drawer()).getByRole('button', { name: 'Cadastrar' })

  async function openDrawer() {
    const user = renderPage()
    await screen.findByRole('row', { name: /Alfa Pagamentos/ })
    await user.click(screen.getByRole('button', { name: 'Cadastrar empresa' }))
    return user
  }

  async function fill(user: ReturnType<typeof userEvent.setup>, name: string, cnpj: string) {
    if (name) await user.type(nameField(), name)
    if (cnpj) await user.type(cnpjField(), cnpj)
  }

  it('"Cadastrar empresa" abre a gaveta com Nome e CNPJ vazios (MCAD-01)', async () => {
    await openDrawer()
    expect(nameField()).toHaveValue('')
    expect(cnpjField()).toHaveValue('')
  })

  it('o estado vazio também abre a gaveta (MCAD-01, MLIST-05)', async () => {
    server.merchants = []
    const user = renderPage()
    const heading = await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })
    await user.click(within(heading.closest('section')!).getByRole('button', { name: 'Cadastrar empresa' }))
    expect(nameField()).toHaveValue('')
    expect(cnpjField()).toHaveValue('')
  })

  it('o CNPJ digitado recebe a máscara e para em 14 dígitos (MCAD-02)', async () => {
    const user = await openDrawer()
    await user.type(cnpjField(), '12345')
    expect(cnpjField()).toHaveValue('12.345')
    await user.type(cnpjField(), '6780001959')
    expect(cnpjField()).toHaveValue('12.345.678/0001-95')
  })

  it('CNPJ colado já formatado é aceito e reformatado (edge case)', async () => {
    const user = await openDrawer()
    await user.click(cnpjField())
    await user.paste('12.345.678/0001-95')
    expect(cnpjField()).toHaveValue('12.345.678/0001-95')
    await user.clear(cnpjField())
    await user.paste('12 345 678 0001 95')
    expect(cnpjField()).toHaveValue('12.345.678/0001-95')
  })

  it('envia POST /api/merchants com o nome sem espaços nas pontas e só os 14 dígitos (MCAD-03)', async () => {
    const user = await openDrawer()
    await fill(user, '  Loja Teste  ', '12345678000195')
    await user.click(submitButton())
    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(posts()[0].body).toEqual({ name: 'Loja Teste', document: '12345678000195' })
  })

  it('201 fecha a gaveta e mostra a empresa na lista e no menu, já selecionada (MCAD-04)', async () => {
    server.merchants = [alfa, beta]
    const user = await openDrawer()
    await fill(user, 'Loja Teste', '11444777000161')
    await user.click(submitButton())

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(row('Loja Teste')).toHaveAttribute('aria-current', 'true'))
    expect(within(row('Loja Teste')).getByText('11.444.777/0001-61')).toBeInTheDocument()
    expect(menuButton()).toHaveTextContent('Loja Teste')
    await user.click(menuButton())
    expect(screen.getByRole('option', { name: /Loja Teste/ })).toHaveAttribute('aria-selected', 'true')
  })

  it('nome vazio após remover espaços bloqueia o envio e mostra a mensagem do campo (MCAD-05)', async () => {
    const user = await openDrawer()
    await fill(user, '   ', '12345678000195')
    await user.click(submitButton())
    expect(within(drawer()).getByText('Informe o nome da empresa')).toBeInTheDocument()
    expect(nameField()).toHaveAttribute('aria-invalid', 'true')
    expect(posts()).toHaveLength(0)
  })

  it('nome com 151 caracteres bloqueia o envio; com 150 é aceito (MCAD-05, edge case)', async () => {
    const user = await openDrawer()
    await user.click(nameField())
    await user.paste('a'.repeat(151))
    await user.type(cnpjField(), '12345678000195')
    await user.click(submitButton())
    expect(within(drawer()).getByText('Use no máximo 150 caracteres')).toBeInTheDocument()
    expect(posts()).toHaveLength(0)

    await user.type(nameField(), '{Backspace}')
    await user.click(submitButton())
    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(posts()[0].body).toEqual({ name: 'a'.repeat(150), document: '12345678000195' })
  })

  it.each([
    ['13 dígitos', '1234567800019'],
    ['dígito verificador errado', '12345678000196'],
    ['dígitos todos iguais', '11111111111111'],
  ])('CNPJ com %s mostra "CNPJ inválido" sem chamar a API (MCAD-06)', async (_case, cnpj) => {
    const user = await openDrawer()
    await fill(user, 'Loja Teste', cnpj)
    await user.click(submitButton())
    expect(within(drawer()).getByText('CNPJ inválido')).toBeInTheDocument()
    expect(cnpjField()).toHaveAttribute('aria-invalid', 'true')
    expect(posts()).toHaveLength(0)
  })

  it('409 mantém a gaveta com os valores e mostra a mensagem do StandardError (MCAD-07)', async () => {
    server.create = () => fail(409, 'Já existe uma empresa com o documento 12345678000195')
    const user = await openDrawer()
    await fill(user, 'Loja Teste', '12345678000195')
    await user.click(submitButton())
    expect(await within(drawer()).findByRole('alert')).toHaveTextContent('Já existe uma empresa com o documento 12345678000195')
    expect(nameField()).toHaveValue('Loja Teste')
    expect(cnpjField()).toHaveValue('12.345.678/0001-95')
  })

  it.each([
    ['outro erro da API', () => fail(500, 'Erro interno ao cadastrar'), 'Erro interno ao cadastrar'],
    ['falha de rede', () => new TypeError('Failed to fetch'), 'Não foi possível conectar à API. Tente novamente.'],
  ])('%s mantém a gaveta com os valores e mostra a mensagem (MCAD-08)', async (_case, reply, message) => {
    server.create = reply
    const user = await openDrawer()
    await fill(user, 'Loja Teste', '12345678000195')
    await user.click(submitButton())
    expect(await within(drawer()).findByRole('alert')).toHaveTextContent(message)
    expect(nameField()).toHaveValue('Loja Teste')
    expect(cnpjField()).toHaveValue('12.345.678/0001-95')
  })

  it('durante o envio o botão fica desabilitado e cliques repetidos geram um só POST (MCAD-09)', async () => {
    let respond!: () => void
    server.create = body => () => new Promise<Response>(resolve => { respond = () => resolve(store(body)) })
    const user = await openDrawer()
    await fill(user, 'Loja Teste', '12345678000195')
    await user.dblClick(submitButton())
    await user.click(submitButton())
    expect(submitButton()).toBeDisabled()
    expect(posts()).toHaveLength(1)

    respond()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(posts()).toHaveLength(1)
  })

  it.each([
    ['Esc', (user: ReturnType<typeof userEvent.setup>) => user.keyboard('{Escape}')],
    ['Cancelar', (user: ReturnType<typeof userEvent.setup>) => user.click(within(drawer()).getByRole('button', { name: 'Cancelar' }))],
  ])('%s fecha a gaveta sem chamar a API (MCAD-10)', async (_case, close) => {
    const user = await openDrawer()
    await fill(user, 'Loja Teste', '12345678000195')
    await close(user)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(posts()).toHaveLength(0)
  })
})

describe('Renomear empresa', () => {
  const puts = () => calls.filter(call => call.method === 'PUT')
  const drawer = () => screen.getByRole('dialog', { name: 'Editar empresa' })
  const nameField = () => within(drawer()).getByLabelText('Nome')
  const saveButton = () => within(drawer()).getByRole('button', { name: 'Salvar' })

  async function openEdit(name = 'Alfa Pagamentos') {
    const user = renderPage()
    await screen.findByRole('row', { name: new RegExp(name) })
    await user.click(within(row(name)).getByRole('button', { name: 'Editar' }))
    return user
  }

  async function rename(user: ReturnType<typeof userEvent.setup>, name: string) {
    await user.clear(nameField())
    if (name) await user.type(nameField(), name)
    await user.click(saveButton())
  }

  it('"Editar" abre a gaveta com o nome atual editável e o CNPJ formatado somente leitura (MEDIT-01)', async () => {
    await openEdit()
    expect(nameField()).toHaveValue('Alfa Pagamentos')
    expect(nameField()).not.toHaveAttribute('readonly')
    const cnpj = within(drawer()).getByLabelText('CNPJ')
    expect(cnpj).toHaveValue('12.345.678/0001-95')
    expect(cnpj).toHaveAttribute('readonly')
  })

  it('salvar chama PUT /api/merchants/{id} só com o nome sem espaços nas pontas (MEDIT-02)', async () => {
    const user = await openEdit()
    await rename(user, '  Alfa Matriz  ')
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0].path).toBe('/api/merchants/m-1')
    expect(puts()[0].body).toEqual({ name: 'Alfa Matriz' })
  })

  it('200 fecha a gaveta e mostra o novo nome na lista e no menu do topo (MEDIT-03)', async () => {
    const user = await openEdit()
    await waitFor(() => expect(menuButton()).toHaveTextContent('Alfa Pagamentos'))
    await rename(user, 'Alfa Matriz')

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    await waitFor(() => expect(within(row('Alfa Matriz')).getByRole('button', { name: 'Alfa Matriz' })).toBeInTheDocument())
    expect(screen.queryByRole('row', { name: /Alfa Pagamentos/ })).not.toBeInTheDocument()
    expect(menuButton()).toHaveTextContent('Alfa Matriz')
  })

  it.each([
    ['vazio após remover espaços', '   ', 'Informe o nome da empresa'],
    ['com 151 caracteres', 'b'.repeat(151), 'Use no máximo 150 caracteres'],
  ])('nome %s bloqueia o envio sem chamar a API (MEDIT-04)', async (_case, name, message) => {
    const user = await openEdit()
    await user.clear(nameField())
    await user.click(nameField())
    await user.paste(name)
    await user.click(saveButton())
    expect(within(drawer()).getByText(message)).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  it.each([
    ['erro da API', () => fail(500, 'Erro interno ao renomear'), 'Erro interno ao renomear'],
    ['falha de rede', () => new TypeError('Failed to fetch'), 'Não foi possível conectar à API. Tente novamente.'],
  ])('%s mantém a gaveta com o nome digitado e mostra a mensagem (MEDIT-05)', async (_case, reply, message) => {
    server.rename = reply
    const user = await openEdit()
    await rename(user, 'Alfa Matriz')
    expect(await within(drawer()).findByRole('alert')).toHaveTextContent(message)
    expect(nameField()).toHaveValue('Alfa Matriz')
  })

  it('durante o envio o botão de salvar fica desabilitado (MEDIT-06)', async () => {
    let respond!: () => void
    server.rename = (merchantId, body) => () => new Promise<Response>(resolve => { respond = () => resolve(storeName(merchantId, body.name)) })
    const user = await openEdit()
    await rename(user, 'Alfa Matriz')
    expect(saveButton()).toBeDisabled()
    await user.click(saveButton())
    expect(puts()).toHaveLength(1)

    respond()
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('404 ao editar mostra a mensagem e recarrega a lista (edge case)', async () => {
    server.rename = merchantId => {
      server.merchants = server.merchants.filter(merchant => merchant.id !== merchantId)
      return fail(404, 'Empresa não encontrada')
    }
    const user = await openEdit()
    const before = listRequests().length
    await rename(user, 'Alfa Matriz')
    expect(await within(drawer()).findByRole('alert')).toHaveTextContent('Empresa não encontrada')
    await waitFor(() => expect(screen.queryByRole('row', { name: /Alfa Pagamentos/ })).not.toBeInTheDocument())
    expect(listRequests().length).toBeGreaterThan(before)
    expect(row('Beta Comércio')).toBeInTheDocument()
  })
})

describe('Desativar empresa', () => {
  const deletes = () => calls.filter(call => call.method === 'DELETE')
  const dialog = () => screen.getByRole('alertdialog')
  const confirmButton = () => within(dialog()).getByRole('button', { name: 'Desativar' })

  async function openDialog(name = 'Alfa Pagamentos') {
    const user = renderPage()
    await screen.findByRole('row', { name: new RegExp(name) })
    await waitFor(() => expect(menuButton()).toHaveTextContent('Alfa Pagamentos'))
    await user.click(within(row(name)).getByRole('button', { name: 'Desativar' }))
    return user
  }

  it('"Desativar" abre a confirmação com o nome e o aviso do CNPJ reservado (MDES-01)', async () => {
    await openDialog()
    expect(dialog()).toHaveAccessibleName('Desativar Alfa Pagamentos?')
    expect(dialog()).toHaveAccessibleDescription(/O CNPJ 12\.345\.678\/0001-95 não poderá ser cadastrado de novo\./)
    expect(deletes()).toHaveLength(0)
  })

  it('confirmar chama DELETE /api/merchants/{id} (MDES-02)', async () => {
    const user = await openDialog()
    await user.click(confirmButton())
    await waitFor(() => expect(deletes()).toHaveLength(1))
    expect(deletes()[0].path).toBe('/api/merchants/m-1')
    expect(deletes()[0].body).toBeUndefined()
  })

  it('204 fecha o diálogo e tira a empresa da lista e do menu (MDES-03)', async () => {
    const user = await openDialog('Beta Comércio')
    await user.click(confirmButton())

    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
    await waitFor(() => expect(screen.queryByRole('row', { name: /Beta Comércio/ })).not.toBeInTheDocument())
    await user.click(menuButton())
    expect(screen.queryByRole('option', { name: /Beta Comércio/ })).not.toBeInTheDocument()
    expect(screen.getByRole('option', { name: /Alfa Pagamentos/ })).toBeInTheDocument()
  })

  it('desativar a selecionada passa o menu para a primeira empresa restante (MDES-04)', async () => {
    const user = await openDialog('Alfa Pagamentos')
    await user.click(confirmButton())
    await waitFor(() => expect(screen.queryByRole('row', { name: /Alfa Pagamentos/ })).not.toBeInTheDocument())
    expect(menuButton()).toHaveTextContent('Beta Comércio')
    expect(row('Beta Comércio')).toHaveAttribute('aria-current', 'true')
  })

  it('desativar a última empresa mostra o estado sem empresa (MDES-04)', async () => {
    server.merchants = [alfa]
    const user = await openDialog('Alfa Pagamentos')
    await user.click(confirmButton())
    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(menuButton()).toHaveTextContent('Nenhuma empresa')
  })

  it.each([
    ['Cancelar', (user: ReturnType<typeof userEvent.setup>) => user.click(within(dialog()).getByRole('button', { name: 'Cancelar' }))],
    ['Esc', (user: ReturnType<typeof userEvent.setup>) => user.keyboard('{Escape}')],
  ])('%s fecha o diálogo sem chamar a API (MDES-05)', async (_case, close) => {
    const user = await openDialog()
    await close(user)
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(deletes()).toHaveLength(0)
    expect(row('Alfa Pagamentos')).toBeInTheDocument()
  })

  it.each([
    ['erro da API', () => fail(500, 'Erro interno ao desativar'), 'Erro interno ao desativar'],
    ['falha de rede', () => new TypeError('Failed to fetch'), 'Não foi possível conectar à API. Tente novamente.'],
  ])('%s mantém o diálogo aberto e mostra a mensagem (MDES-06)', async (_case, reply, message) => {
    server.deactivate = reply
    const user = await openDialog()
    await user.click(confirmButton())
    expect(await within(dialog()).findByRole('alert')).toHaveTextContent(message)
    expect(dialog()).toHaveAccessibleName('Desativar Alfa Pagamentos?')
  })

  it('403 ao desativar mostra a mensagem e recarrega a lista (edge case)', async () => {
    server.deactivate = merchantId => {
      server.merchants = server.merchants.filter(merchant => merchant.id !== merchantId)
      return fail(403, 'Você não tem acesso a esta empresa')
    }
    const user = await openDialog()
    const before = listRequests().length
    await user.click(confirmButton())
    expect(await within(dialog()).findByRole('alert')).toHaveTextContent('Você não tem acesso a esta empresa')
    await waitFor(() => expect(screen.queryByRole('row', { name: /Alfa Pagamentos/ })).not.toBeInTheDocument())
    expect(listRequests().length).toBeGreaterThan(before)
  })
})

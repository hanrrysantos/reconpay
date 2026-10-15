import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import EmpresasPage from '@/app/(app)/empresas/page'
import type { MerchantRecord } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Reply = Response | (() => Response | Promise<Response>)
type Call = { method: string; path: string; search: URLSearchParams; body: unknown }

const record = (id: string, name: string, document: string): MerchantRecord => ({ id, name, document, active: true, createdAt: '2026-10-01T12:00:00Z' })
const alfa = record('m-1', 'Alfa Pagamentos', '12345678000195')
const beta = record('m-2', 'Beta Comércio', '11222333000181')

/** In-memory API: `merchants` answers the top menu; `list` answers `GET /api/merchants` (defaults to the same merchants). */
let server: { merchants: MerchantRecord[]; list: ((search: URLSearchParams) => Reply) | null }
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })
const fail = (status: number, message: string) => json({ error: status === 409 ? 'CONFLICT' : 'ERROR', message }, status)
const page = <T,>(content: T[], totalPages = 1) => ({ content, totalPages, totalElements: content.length })

async function route(method: string, path: string, search: URLSearchParams): Promise<Response> {
  if (path === '/api/me/merchants') return json(page(server.merchants.map(({ id, name, document }) => ({ id, name, document }))))
  if (path === '/api/merchants' && method === 'GET') {
    const reply = server.list?.(search) ?? json(page(server.merchants))
    return typeof reply === 'function' ? reply() : reply
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  calls = []
  server = { merchants: [alfa, beta], list: null }
  vi.stubGlobal('fetch', vi.fn(async (input: string, init: RequestInit = {}) => {
    const url = new URL(input)
    const method = init.method ?? 'GET'
    const body = init.body ? JSON.parse(String(init.body)) : undefined
    calls.push({ method, path: url.pathname, search: url.searchParams, body })
    return route(method, url.pathname, url.searchParams)
  }))
})
afterEach(() => vi.unstubAllGlobals())

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
const menuButton = () => screen.getByRole('button', { expanded: false, name: /Pagamentos|Comércio|Nenhuma empresa/ })

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

  it('documento gravado sem 14 dígitos aparece como está, sem máscara (MLIST-08)', async () => {
    server.merchants = [record('m-9', 'Legado Ltda', 'ABC-123')]
    renderPage()
    const legacy = await screen.findByRole('row', { name: /Legado Ltda/ })
    expect(within(legacy).getByText('ABC-123')).toBeInTheDocument()
  })
})

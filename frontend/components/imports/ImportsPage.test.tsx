import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ImportacoesPage from '@/app/(app)/importacoes/page'
import type { ImportBatch, Merchant } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { MerchantSwitcher, SelectedMerchantProvider } from '@/components/shell/MerchantMenu'

/** `useSearchParams` follows `router.replace`, as the real router does on a same-page navigation. */
const navigation = vi.hoisted(() => {
  const listeners = new Set<() => void>()
  const nav = {
    search: new URLSearchParams(),
    listeners,
    replace: vi.fn((href: string) => {
      nav.search = new URL(href, 'http://localhost').searchParams
      listeners.forEach(listener => listener())
    }),
  }
  return nav
})
vi.mock('next/navigation', async () => {
  const { useSyncExternalStore } = await import('react')
  const subscribe = (listener: () => void) => {
    navigation.listeners.add(listener)
    return () => { navigation.listeners.delete(listener) }
  }
  return {
    useSearchParams: () => useSyncExternalStore(subscribe, () => navigation.search),
    useRouter: () => ({ replace: navigation.replace }),
  }
})
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1', name: 'Ana Souza' }, signOut: vi.fn() }) }))

type Call = { method: string; path: string; search: URLSearchParams }

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11222333000181' }
const batch = (id: string, merchantId: string, fileName: string): ImportBatch => ({ id, merchantId, fileName, totalRows: 3, createdAt: '2026-10-10T12:00:00Z' })

/** In-memory API: the top menu lists `merchants`; `batches` holds each merchant's accepted imports by path; every upload is refused with row errors. */
let server: { merchants: Merchant[]; batches: Record<string, ImportBatch[]> }
let calls: Call[]

const json = (body: unknown, status = 200) => Response.json(body, { status })

function route(method: string, path: string): Response {
  if (path === '/api/me/merchants') return json({ content: server.merchants, totalPages: 1 })
  const list = path.match(/^\/api\/merchants\/([^/]+)\/(transactions|external-settlements|bank-statements)\/imports$/)
  if (list && method === 'GET') return json({ content: server.batches[`${list[1]}/${list[2]}`] ?? [], totalPages: 1 })
  if (/\/import$/.test(path) && method === 'POST') {
    return json({
      status: 400, error: 'VALIDATION_ERROR', message: 'Erro na importação do CSV',
      details: { rowErrors: [{ row: 2, message: 'Referência externa é obrigatória' }] },
    }, 400)
  }
  throw new Error(`rota sem resposta no teste: ${method} ${path}`)
}

beforeEach(() => {
  calls = []
  server = {
    merchants: [alfa, beta],
    batches: {
      'm-1/transactions': [batch('i-1', 'm-1', 'alfa-transacoes.csv')],
      'm-1/external-settlements': [batch('i-2', 'm-1', 'alfa-liquidacao.csv')],
      'm-1/bank-statements': [batch('i-3', 'm-1', 'alfa-extrato.csv')],
      'm-2/transactions': [batch('i-4', 'm-2', 'beta-transacoes.csv')],
    },
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
  navigation.search = new URLSearchParams()
  navigation.replace.mockClear()
})

function renderPage(search = '') {
  navigation.search = new URLSearchParams(search)
  render(
    <QueryClientProvider client={createQueryClient(vi.fn())}>
      <SelectedMerchantProvider>
        <MerchantSwitcher />
        <ImportacoesPage />
      </SelectedMerchantProvider>
    </QueryClientProvider>,
  )
  return userEvent.setup()
}

const tabs = () => within(screen.getByRole('tablist', { name: 'Tipo de arquivo' })).getAllByRole('tab')
const selectedTab = () => tabs().find(tab => tab.getAttribute('aria-selected') === 'true')?.textContent
const batchesTable = () => screen.findByRole('table', { name: 'Lotes importados' })
const importButton = () => screen.getByRole('button', { name: 'Importar' })
/** The top menu trigger is the only button with `aria-expanded`. */
const menuButton = () => screen.getByRole('button', { expanded: false })

async function refuseUpload(user: ReturnType<typeof userEvent.setup>) {
  await user.upload(screen.getByLabelText('Arquivo CSV'), new File(['x'], 'arquivo.csv', { type: 'text/csv' }))
  await user.click(importButton())
  expect(await screen.findByRole('alert')).toHaveTextContent('Referência externa é obrigatória')
}

describe('ImportsPage', () => {
  it('a rota mostra o título, a empresa e as abas Transações, Liquidação e Extrato, com Transações ativa sem ?tipo= (IMP-01)', async () => {
    renderPage()

    expect(await batchesTable()).toHaveTextContent('alfa-transacoes.csv')
    expect(screen.getByRole('heading', { level: 1, name: 'Importações' })).toBeInTheDocument()
    expect(screen.getAllByText('Alfa Pagamentos').length).toBeGreaterThan(0)
    expect(tabs().map(tab => tab.textContent)).toEqual(['Transações', 'Liquidação', 'Extrato'])
    expect(selectedTab()).toBe('Transações')
    expect(screen.getByRole('tabpanel', { name: 'Transações' })).toBeInTheDocument()
    expect(calls.some(call => call.path === '/api/merchants/m-1/transactions/imports')).toBe(true)
  })

  it.each([
    ['tipo=extrato', 'Extrato', '/api/merchants/m-1/bank-statements/imports', 'alfa-extrato.csv'],
    ['tipo=liquidacao', 'Liquidação', '/api/merchants/m-1/external-settlements/imports', 'alfa-liquidacao.csv'],
    ['tipo=xyz', 'Transações', '/api/merchants/m-1/transactions/imports', 'alfa-transacoes.csv'],
  ])('com ?%s abre a aba %s (IMP-01)', async (search, label, path, fileName) => {
    renderPage(search)

    expect(await batchesTable()).toHaveTextContent(fileName)
    expect(selectedTab()).toBe(label)
    expect(calls.filter(call => call.path.endsWith('/imports')).map(call => call.path)).toEqual([path])
  })

  it('trocar de aba atualiza ?tipo= e mostra os lotes do novo tipo (IMP-01)', async () => {
    const user = renderPage()
    await batchesTable()

    await user.click(screen.getByRole('tab', { name: 'Liquidação' }))
    expect(navigation.replace).toHaveBeenLastCalledWith('/importacoes?tipo=liquidacao')
    expect(await screen.findByText('alfa-liquidacao.csv')).toBeInTheDocument()
    expect(selectedTab()).toBe('Liquidação')

    await user.click(screen.getByRole('tab', { name: 'Extrato' }))
    expect(navigation.replace).toHaveBeenLastCalledWith('/importacoes?tipo=extrato')
    await user.click(screen.getByRole('tab', { name: 'Transações' }))
    expect(navigation.replace).toHaveBeenLastCalledWith('/importacoes?tipo=transacoes')
  })

  it('trocar de aba descarta o arquivo escolhido e o resultado do envio (IMP-13)', async () => {
    const user = renderPage()
    await batchesTable()
    await refuseUpload(user)

    await user.click(screen.getByRole('tab', { name: 'Liquidação' }))
    expect(await screen.findByText('alfa-liquidacao.csv')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(importButton()).toBeDisabled()

    await user.click(screen.getByRole('tab', { name: 'Transações' }))
    expect(await screen.findByText('alfa-transacoes.csv')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(importButton()).toBeDisabled()
  })

  it('trocar de empresa descarta arquivo e resultado e lista os lotes da nova empresa (IMP-13)', async () => {
    const user = renderPage()
    await batchesTable()
    await refuseUpload(user)

    await user.click(menuButton())
    await user.click(screen.getByRole('option', { name: /Beta Comércio/ }))

    expect(await screen.findByText('beta-transacoes.csv')).toBeInTheDocument()
    expect(screen.queryByText('alfa-transacoes.csv')).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(importButton()).toBeDisabled()
    expect(selectedTab()).toBe('Transações')
    await waitFor(() => expect(calls.some(call => call.path === '/api/merchants/m-2/transactions/imports')).toBe(true))
  })

  it('sem empresa no acesso mostra o mesmo estado vazio de Regras de taxa, sem abas (IMP-13)', async () => {
    server.merchants = []
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.queryByRole('tablist')).toBeNull()
    expect(calls.some(call => call.path.endsWith('/imports'))).toBe(false)
  })
})

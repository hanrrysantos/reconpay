import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import UsuariosPage from '@/app/(app)/usuarios/page'
import type { Merchant } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { account, fail, installFakeApi, type FakeServer } from './fakeUsersApi'

vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: vi.fn() }) }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-admin', name: 'Ana Admin', email: 'ana@reconpay.dev', role: 'ADMIN' } }) }))

const alfa: Merchant = { id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000195' }
const beta: Merchant = { id: 'm-2', name: 'Beta Comércio', document: '11222333000181' }
const gama: Merchant = { id: 'm-3', name: 'Gama Serviços', document: '98765432000155' }

let server: FakeServer

beforeEach(() => {
  server = installFakeApi({
    users: [account('u-admin', 'Ana Admin', { role: 'ADMIN' }), account('u-bruno', 'Bruno Operador')],
    merchants: [alfa, beta, gama],
    access: { 'u-bruno': ['m-2'] },
  })
})
afterEach(() => vi.unstubAllGlobals())

async function openAccess() {
  render(<QueryClientProvider client={createQueryClient(vi.fn())}><UsuariosPage /></QueryClientProvider>)
  await screen.findByRole('table', { name: 'Usuários' })
  const user = userEvent.setup()
  const row = within(screen.getByRole('table', { name: 'Usuários' })).getAllByRole('row').find(item => item.textContent?.includes('Bruno Operador'))!
  await user.click(within(row).getByRole('button', { name: 'Acessos' }))
  return user
}

const drawer = () => screen.getByRole('dialog', { name: 'Acessos de Bruno Operador' })
const boxes = () => within(drawer()).getAllByRole('checkbox')
const puts = () => server.calls.filter(call => call.method === 'PUT' && call.path === '/api/users/u-bruno/merchants')
const save = (user: ReturnType<typeof userEvent.setup>) => user.click(within(drawer()).getByRole('button', { name: 'Salvar' }))

describe('AccessDrawer', () => {
  it('lista todas as empresas ativas em ordem alfabética, com CNPJ, marcadas as que o operador acessa (USERS-05 AC1)', async () => {
    await openAccess()
    await waitFor(() => expect(boxes()).toHaveLength(3))

    expect(boxes().map(box => box.closest('label')!.textContent)).toEqual([
      'Alfa Pagamentos12.345.678/0001-95', 'Beta Comércio11.222.333/0001-81', 'Gama Serviços98.765.432/0001-55',
    ])
    expect(boxes().map(box => (box as HTMLInputElement).checked)).toEqual([false, true, false])
    const merchantsRequest = server.calls.find(call => call.path === '/api/me/merchants')!
    expect(merchantsRequest.search.get('sort')).toBe('name,asc')
    expect(server.calls.some(call => call.method === 'GET' && call.path === '/api/users/u-bruno/merchants')).toBe(true)
  })

  it('marcar e desmarcar envia exatamente as marcadas e mostra "Acessos atualizados." (USERS-05 AC2)', async () => {
    const user = await openAccess()
    await waitFor(() => expect(boxes()).toHaveLength(3))
    await user.click(within(drawer()).getByRole('checkbox', { name: /Alfa Pagamentos/ }))
    await user.click(within(drawer()).getByRole('checkbox', { name: /Gama Serviços/ }))
    await user.click(within(drawer()).getByRole('checkbox', { name: /Beta Comércio/ }))
    await save(user)

    expect(await screen.findByRole('status')).toHaveTextContent('Acessos atualizados.')
    expect(puts().map(call => call.body)).toEqual([{ merchantIds: ['m-1', 'm-3'] }])
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(server.access['u-bruno']).toEqual(['m-1', 'm-3'])
  })

  it('salvar sem mexer reenvia exatamente os acessos atuais (USERS-05 AC2)', async () => {
    const user = await openAccess()
    await waitFor(() => expect(boxes()).toHaveLength(3))
    await save(user)

    await screen.findByRole('status')
    expect(puts().map(call => call.body)).toEqual([{ merchantIds: ['m-2'] }])
  })

  it('desmarcar tudo envia a lista vazia (USERS-05 AC3)', async () => {
    const user = await openAccess()
    await waitFor(() => expect(boxes()).toHaveLength(3))
    await user.click(within(drawer()).getByRole('checkbox', { name: /Beta Comércio/ }))
    await save(user)

    expect(await screen.findByRole('status')).toHaveTextContent('Acessos atualizados.')
    expect(puts().map(call => call.body)).toEqual([{ merchantIds: [] }])
  })

  it('sem empresa ativa mostra "Nenhuma empresa cadastrada." (USERS-05 AC7)', async () => {
    server.merchants = []
    server.access = {}
    await openAccess()

    expect(await within(drawer()).findByText('Nenhuma empresa cadastrada.')).toBeInTheDocument()
    expect(within(drawer()).queryAllByRole('checkbox')).toHaveLength(0)
  })

  it.each([
    [404, 'Empresa não encontrada'],
    [409, 'Contas de demonstração não recebem acesso a outras empresas.'],
  ])('%i mostra a message e mantém o painel aberto (edge case, USERS-05 AC5)', async (status, message) => {
    server.replaceAccess = () => fail(status, message)
    const user = await openAccess()
    await waitFor(() => expect(boxes()).toHaveLength(3))
    await user.click(within(drawer()).getByRole('checkbox', { name: /Alfa Pagamentos/ }))
    await save(user)

    expect(await within(drawer()).findByRole('alert')).toHaveTextContent(message)
    expect(boxes().map(box => (box as HTMLInputElement).checked)).toEqual([true, true, false])
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})

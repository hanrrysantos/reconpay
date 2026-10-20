import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterAll, afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import UsuariosPage from '@/app/(app)/usuarios/page'
import type { User } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { account, installFakeApi, type FakeServer } from './fakeUsersApi'

// Pinned before the imports run, so the local time differs from UTC (São Paulo is UTC-3, no daylight saving).
const originalTz = vi.hoisted(() => {
  const tz = process.env.TZ
  process.env.TZ = 'America/Sao_Paulo'
  return tz
})
afterAll(() => {
  if (originalTz === undefined) delete process.env.TZ
  else process.env.TZ = originalTz
})

const navigation = vi.hoisted(() => ({ replace: vi.fn() }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: navigation.replace }) }))
const auth = vi.hoisted(() => ({ user: { id: 'u-admin', name: 'Ana Admin', email: 'ana@reconpay.dev', role: 'ADMIN' as 'ADMIN' | 'OPERATOR' } }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: auth.user }) }))

const me = account('u-admin', 'Ana Admin', { role: 'ADMIN' })
const bruno = account('u-bruno', 'Bruno Operador')
const carla = account('u-carla', 'Carla Admin', { role: 'ADMIN' })
const davi = account('u-davi', 'Davi Inativo', { active: false })
const demo = account('u-demo', 'Visitante Demo', { demoExpiresAt: '2099-10-21T12:34:56Z' })
const oldDemo = account('u-demo-off', 'Visitante Expirado', { active: false, demoExpiresAt: '2099-10-21T02:05:00Z' })

let server: FakeServer

beforeEach(() => {
  auth.user = { id: 'u-admin', name: 'Ana Admin', email: 'ana@reconpay.dev', role: 'ADMIN' }
  server = installFakeApi({ users: [me, bruno, carla, davi, demo, oldDemo] })
})
afterEach(() => {
  vi.unstubAllGlobals()
  navigation.replace.mockReset()
})

function renderPage() {
  render(<QueryClientProvider client={createQueryClient(vi.fn())}><UsuariosPage /></QueryClientProvider>)
  return userEvent.setup()
}

const listRequests = () => server.calls.filter(call => call.method === 'GET' && call.path === '/api/users')
const lastList = () => listRequests().at(-1)!.search
const table = () => screen.getByRole('table', { name: 'Usuários' })
/** The name of each row, without the demo badge. */
const rowNames = () => within(table()).getAllByRole('row').slice(1).map(row => within(row).getAllByRole('cell')[0].querySelector('span')!.textContent)
const row = (name: string) => within(table()).getAllByRole('row').find(item => item.textContent?.includes(name))!
const rowActions = (name: string) => within(row(name)).queryAllByRole('button').map(button => button.textContent)
const many = (count: number, extra: Partial<User> = {}) =>
  Array.from({ length: count }, (_, index) => account(`u-${index}`, `Pessoa ${String(index).padStart(2, '0')}`, extra))

describe('UsersPage', () => {
  it('pede a primeira página de ativos sem demos, por nome, e mostra nome, e-mail, papel, status e cadastro (USERS-02 AC8, AC9)', async () => {
    renderPage()
    expect(screen.getByRole('heading', { level: 1, name: 'Usuários' })).toBeInTheDocument()
    await screen.findByRole('table', { name: 'Usuários' })

    expect(lastList().toString()).toBe('status=ACTIVE&demo=false&page=0&size=20&sort=name%2Casc')
    expect(within(table()).getAllByRole('columnheader').map(header => header.textContent)).toEqual(['Nome', 'E-mail', 'Papel', 'Status', 'Cadastro', 'Ações'])
    expect(rowNames()).toEqual(['Ana Admin', 'Bruno Operador', 'Carla Admin'])
    expect(within(row('Bruno Operador')).getAllByRole('cell').slice(0, 5).map(cell => cell.textContent))
      .toEqual(['Bruno Operador', 'u-bruno@reconpay.dev', 'Operador', 'Ativo', '01/10/2026'])
    expect(within(row('Carla Admin')).getByText('Admin')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ativos' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Inativos' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('button', { name: 'Todos' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('checkbox', { name: 'Mostrar demonstrações' })).not.toBeChecked()
  })

  it.each([
    ['Inativos', 'INACTIVE', 'false', ['Davi Inativo']],
    ['Todos', 'ALL', 'false', ['Ana Admin', 'Bruno Operador', 'Carla Admin', 'Davi Inativo']],
  ])('"%s" pede status=%s e mostra só as contas dele (USERS-02 AC9)', async (label, status, demoParam, names) => {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('button', { name: label }))

    await waitFor(() => expect(rowNames()).toEqual(names))
    expect(lastList().get('status')).toBe(status)
    expect(lastList().get('demo')).toBe(demoParam)
    expect(screen.getByRole('button', { name: label })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Ativos' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('"Ativos" depois de outro filtro volta a pedir status=ACTIVE (USERS-02 AC9)', async () => {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('button', { name: 'Inativos' }))
    await waitFor(() => expect(rowNames()).toEqual(['Davi Inativo']))

    await user.click(screen.getByRole('button', { name: 'Ativos' }))
    await waitFor(() => expect(rowNames()).toEqual(['Ana Admin', 'Bruno Operador', 'Carla Admin']))
    expect(lastList().get('status')).toBe('ACTIVE')
  })

  it('"Mostrar demonstrações" pede demo=true e mantém o status escolhido; desligar volta a demo=false (USERS-02 AC9)', async () => {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('checkbox', { name: 'Mostrar demonstrações' }))

    await waitFor(() => expect(rowNames()).toContain('Visitante Demo'))
    expect(lastList().get('status')).toBe('ACTIVE')
    expect(lastList().get('demo')).toBe('true')
    expect(rowNames()).not.toContain('Visitante Expirado')

    await user.click(screen.getByRole('checkbox', { name: 'Mostrar demonstrações' }))
    await waitFor(() => expect(rowNames()).not.toContain('Visitante Demo'))
    expect(lastList().get('demo')).toBe('false')
  })

  it.each([
    ['Inativos', { status: 'INACTIVE', demo: 'false' }],
    ['Todos', { status: 'ALL', demo: 'false' }],
    ['Mostrar demonstrações', { status: 'ACTIVE', demo: 'true' }],
  ])('trocar para "%s" estando na página 2 volta para a página 1 (USERS-02 AC9)', async (label, expected) => {
    server.users = [...many(25), ...many(25, { active: false }).map(item => ({ ...item, id: `${item.id}-off` }))]
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(lastList().get('page')).toBe('1'))
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()

    if (label === 'Mostrar demonstrações') await user.click(screen.getByRole('checkbox', { name: label }))
    else await user.click(screen.getByRole('button', { name: label }))

    await waitFor(() => expect(lastList().get('status')).toBe(expected.status))
    expect(lastList().get('demo')).toBe(expected.demo)
    expect(lastList().get('page')).toBe('0')
    expect(await screen.findByText(/^Página 1 de/)).toBeInTheDocument()
  })

  it.each([
    [demo, '21/10 às 09:34'],
    [oldDemo, '20/10 às 23:05'],
  ])('com demonstrações, a demo leva o selo "Demo · expira em dd/MM às HH:mm" no horário local (%#) (USERS-02 AC10)', async (account, local) => {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('button', { name: 'Todos' }))
    await user.click(screen.getByRole('checkbox', { name: 'Mostrar demonstrações' }))

    await waitFor(() => expect(rowNames()).toContain(account.name))
    expect(within(row(account.name)).getByText(/^Demo · expira em/).textContent).toBe(`Demo · expira em ${local}`)
    expect(within(row('Bruno Operador')).queryByText(/^Demo/)).not.toBeInTheDocument()
  })

  it('sem usuários no filtro mostra "Nenhum usuário encontrado." (USERS-02 AC12)', async () => {
    server.users = [me]
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('button', { name: 'Inativos' }))

    expect(await screen.findByText('Nenhum usuário encontrado.')).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Usuários' })).not.toBeInTheDocument()
  })

  it('OPERATOR é levado para /painel sem chamar /api/users (USERS-02 AC11)', async () => {
    auth.user = { id: 'u-bruno', name: 'Bruno Operador', email: 'bruno@reconpay.dev', role: 'OPERATOR' }
    renderPage()

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith('/painel'))
    expect(listRequests()).toHaveLength(0)
    expect(screen.queryByRole('heading', { name: 'Usuários' })).not.toBeInTheDocument()
  })

  it('ADMIN não é redirecionado', async () => {
    renderPage()
    await screen.findByRole('table', { name: 'Usuários' })

    expect(navigation.replace).not.toHaveBeenCalled()
  })

  describe('ações por linha', () => {
    async function showEverything() {
      const user = renderPage()
      await screen.findByRole('table', { name: 'Usuários' })
      await user.click(screen.getByRole('button', { name: 'Todos' }))
      await user.click(screen.getByRole('checkbox', { name: 'Mostrar demonstrações' }))
      await waitFor(() => expect(rowNames()).toHaveLength(6))
      return user
    }

    it('OPERATOR ativo: Acessos, Renomear e Desativar (USERS-05 AC1)', async () => {
      await showEverything()
      expect(rowActions('Bruno Operador')).toEqual(['Acessos', 'Renomear', 'Desativar'])
    })

    it('outro ADMIN ativo: "Acessa todas as empresas", Renomear e Desativar, sem Acessos (USERS-05 AC4)', async () => {
      await showEverything()
      expect(within(row('Carla Admin')).getByText('Acessa todas as empresas')).toBeInTheDocument()
      expect(rowActions('Carla Admin')).toEqual(['Renomear', 'Desativar'])
    })

    it('o ADMIN logado não tem "Desativar" na própria linha (USERS-04 AC4)', async () => {
      await showEverything()
      expect(within(row('Ana Admin')).getByText('Acessa todas as empresas')).toBeInTheDocument()
      expect(rowActions('Ana Admin')).toEqual(['Renomear'])
    })

    it('demo ativa: só Desativar (USERS-05 AC6)', async () => {
      await showEverything()
      expect(rowActions('Visitante Demo')).toEqual(['Desativar'])
      expect(within(row('Visitante Demo')).queryByText('Acessa todas as empresas')).not.toBeInTheDocument()
    })

    it.each([['Davi Inativo'], ['Visitante Expirado']])('conta inativa (%s): só Reativar, sem Renomear (USERS-06 AC4)', async name => {
      await showEverything()
      expect(rowActions(name)).toEqual(['Reativar'])
      expect(within(row(name)).getByText('Inativo')).toBeInTheDocument()
    })
  })

  it('"Reativar" chama PATCH .../activation sem confirmação, atualiza a lista e mostra "Usuário reativado." (USERS-04 AC6)', async () => {
    const user = renderPage()
    await screen.findByRole('table', { name: 'Usuários' })
    await user.click(screen.getByRole('button', { name: 'Inativos' }))
    await waitFor(() => expect(rowNames()).toEqual(['Davi Inativo']))

    await user.click(within(row('Davi Inativo')).getByRole('button', { name: 'Reativar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Usuário reativado.')
    expect(server.calls.filter(call => call.method === 'PATCH').map(call => call.path)).toEqual(['/api/users/u-davi/activation'])
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(await screen.findByText('Nenhum usuário encontrado.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Fechar aviso' }))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('a última página que fica vazia volta para a anterior (edge case)', async () => {
    server.users = many(21, { active: false })
    const user = renderPage()
    await screen.findByText('Nenhum usuário encontrado.')
    await user.click(screen.getByRole('button', { name: 'Inativos' }))
    await screen.findByText('Página 1 de 2')
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(rowNames()).toEqual(['Pessoa 20']))

    await user.click(within(row('Pessoa 20')).getByRole('button', { name: 'Reativar' }))

    await waitFor(() => expect(lastList().get('page')).toBe('0'))
    await waitFor(() => expect(rowNames()).toHaveLength(20))
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).not.toBeInTheDocument()
  })

  it('erro na lista mostra a mensagem e "Tentar de novo"', async () => {
    server.list = () => Response.json({ error: 'ERROR', message: 'Falha ao listar' }, { status: 400 })
    renderPage()

    expect(await screen.findByText('Falha ao listar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar de novo' })).toBeInTheDocument()
  })
})

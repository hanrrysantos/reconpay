import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import UsuariosPage from '@/app/(app)/usuarios/page'
import { createQueryClient } from '@/lib/queries'
import { account, fail, installFakeApi, type FakeServer } from './fakeUsersApi'

vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: vi.fn() }) }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-admin', name: 'Ana Admin', email: 'ana@reconpay.dev', role: 'ADMIN' } }) }))

let server: FakeServer

beforeEach(() => {
  server = installFakeApi({
    users: [
      account('u-admin', 'Ana Admin', { role: 'ADMIN' }),
      account('u-bruno', 'Bruno Operador'),
      account('u-carla', 'Carla Admin', { role: 'ADMIN' }),
      account('u-demo', 'Visitante Demo', { demoExpiresAt: '2099-10-21T12:34:56Z' }),
    ],
  })
})
afterEach(() => vi.unstubAllGlobals())

async function renderPage({ demos = false } = {}) {
  render(<QueryClientProvider client={createQueryClient(vi.fn())}><UsuariosPage /></QueryClientProvider>)
  await screen.findByRole('table', { name: 'Usuários' })
  const user = userEvent.setup()
  if (demos) {
    await user.click(screen.getByRole('checkbox', { name: 'Mostrar demonstrações' }))
    await waitFor(() => expect(rowNames()).toContain('Visitante Demo'))
  }
  return user
}

const deletes = () => server.calls.filter(call => call.method === 'DELETE')
const table = () => screen.getByRole('table', { name: 'Usuários' })
const rowNames = () => within(table()).getAllByRole('row').slice(1).map(row => row.querySelector('td span')!.textContent)
const row = (name: string) => within(table()).getAllByRole('row').find(item => item.textContent?.includes(name))!

describe('DeactivateUserDialog', () => {
  it('"Desativar" pede confirmação com o aviso e não chama a API antes de confirmar (USERS-04 AC1)', async () => {
    const user = await renderPage()
    await user.click(within(row('Bruno Operador')).getByRole('button', { name: 'Desativar' }))

    const dialog = screen.getByRole('alertdialog', { name: 'Desativar Bruno Operador?' })
    expect(dialog).toHaveAccessibleDescription('Bruno Operador perde o acesso na hora, inclusive a sessão aberta.')
    expect(deletes()).toHaveLength(0)
  })

  it.each([
    ['Bruno Operador', 'u-bruno'],
    ['Carla Admin', 'u-carla'],
    ['Visitante Demo', 'u-demo'],
  ])('confirmar em %s chama DELETE /api/users/%s, fecha, atualiza a lista e mostra "Usuário desativado." (USERS-04 AC2)', async (name, id) => {
    const user = await renderPage({ demos: true })
    await user.click(within(row(name)).getByRole('button', { name: 'Desativar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Desativar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Usuário desativado.')
    expect(deletes().map(call => call.path)).toEqual([`/api/users/${id}`])
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    await waitFor(() => expect(rowNames()).not.toContain(name))
  })

  it('desativar quem outra aba já desativou responde 204 de novo e a conta aparece em "Inativos" (edge case)', async () => {
    const user = await renderPage()
    server.users = server.users.map(account => account.id === 'u-bruno' ? { ...account, active: false } : account)
    await user.click(within(row('Bruno Operador')).getByRole('button', { name: 'Desativar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Desativar' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Usuário desativado.')
    await waitFor(() => expect(rowNames()).not.toContain('Bruno Operador'))
    await user.click(screen.getByRole('button', { name: 'Inativos' }))
    await waitFor(() => expect(rowNames()).toEqual(['Bruno Operador']))
  })

  it('cancelar fecha sem chamar a API (USERS-04 AC1)', async () => {
    const user = await renderPage()
    await user.click(within(row('Bruno Operador')).getByRole('button', { name: 'Desativar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(deletes()).toHaveLength(0)
    expect(rowNames()).toContain('Bruno Operador')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('erro da API aparece no diálogo, que continua aberto', async () => {
    server.deactivate = () => fail(409, 'Você não pode desativar a própria conta.')
    const user = await renderPage()
    await user.click(within(row('Bruno Operador')).getByRole('button', { name: 'Desativar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Desativar' }))

    expect(await within(screen.getByRole('alertdialog')).findByRole('alert')).toHaveTextContent('Você não pode desativar a própria conta.')
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('desativar o único usuário da última página com "Ativos" volta para a página anterior (edge case)', async () => {
    server.users = Array.from({ length: 21 }, (_, index) => account(`u-${index}`, `Pessoa ${String(index).padStart(2, '0')}`))
    const user = await renderPage()
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(rowNames()).toEqual(['Pessoa 20']))

    await user.click(within(row('Pessoa 20')).getByRole('button', { name: 'Desativar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Desativar' }))

    await waitFor(() => expect(rowNames()).toHaveLength(20))
    expect(rowNames()[0]).toBe('Pessoa 00')
  })
})

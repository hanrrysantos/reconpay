import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccountMenu } from './AccountMenu'

const signOut = vi.hoisted(() => vi.fn())
const auth = vi.hoisted(() => ({ user: null as { id: string; name: string; email: string; role: 'ADMIN' | 'OPERATOR' } | null }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ user: auth.user, signOut }) }))

const admin = { id: 'u-1', name: 'Ana Souza', email: 'ana@reconpay.dev', role: 'ADMIN' as const }
const operator = { id: 'u-2', name: 'Bruno Lima', email: 'bruno@reconpay.dev', role: 'OPERATOR' as const }

beforeEach(() => {
  auth.user = admin
  signOut.mockReset()
})

function renderMenu() {
  render(<><AccountMenu /><button type="button">Fora</button></>)
  return userEvent.setup()
}

describe('AccountMenu', () => {
  it('o avatar mostra as iniciais e o menu começa fechado', () => {
    renderMenu()

    const avatar = screen.getByRole('button', { name: 'Ana Souza' })
    expect(avatar).toHaveTextContent('AS')
    expect(avatar).toHaveAttribute('aria-haspopup', 'menu')
    expect(avatar).toHaveAttribute('aria-expanded', 'false')
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it.each([
    [admin, 'Admin'],
    [operator, 'Operador'],
  ])('o clique abre o menu com nome, e-mail, selo do papel e "Sair" (%#) (USERS-01 AC1)', async (account, badge) => {
    auth.user = account
    const user = renderMenu()
    await user.click(screen.getByRole('button', { name: account.name }))

    const menu = screen.getByRole('menu')
    expect(screen.getByRole('button', { name: account.name })).toHaveAttribute('aria-expanded', 'true')
    expect(within(menu).getByText(account.name)).toBeInTheDocument()
    expect(within(menu).getByText(account.email)).toBeInTheDocument()
    expect(within(menu).getByText(badge)).toBeInTheDocument()
    expect(within(menu).getByRole('menuitem', { name: 'Sair' })).toBeInTheDocument()
  })

  it('para ADMIN mostra "Usuários", que leva a /usuarios (USERS-01 AC2)', async () => {
    const user = renderMenu()
    await user.click(screen.getByRole('button', { name: 'Ana Souza' }))

    expect(within(screen.getByRole('menu')).getByRole('menuitem', { name: 'Usuários' })).toHaveAttribute('href', '/usuarios')
  })

  it('para OPERATOR não mostra "Usuários" (USERS-01 AC3)', async () => {
    auth.user = operator
    const user = renderMenu()
    await user.click(screen.getByRole('button', { name: 'Bruno Lima' }))

    const menu = screen.getByRole('menu')
    expect(within(menu).queryByRole('menuitem', { name: 'Usuários' })).not.toBeInTheDocument()
    expect(within(menu).getAllByRole('menuitem').map(item => item.textContent)).toEqual(['Sair'])
  })

  it('"Sair" no menu encerra a sessão (USERS-01 AC4)', async () => {
    const user = renderMenu()
    await user.click(screen.getByRole('button', { name: 'Ana Souza' }))
    await user.click(screen.getByRole('menuitem', { name: 'Sair' }))

    expect(signOut).toHaveBeenCalledOnce()
  })

  it('Esc fecha o menu (USERS-01 AC6)', async () => {
    const user = renderMenu()
    await user.click(screen.getByRole('button', { name: 'Ana Souza' }))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ana Souza' })).toHaveAttribute('aria-expanded', 'false')
  })

  it('clique fora fecha o menu, e clique dentro não (USERS-01 AC6)', async () => {
    const user = renderMenu()
    await user.click(screen.getByRole('button', { name: 'Ana Souza' }))
    await user.click(within(screen.getByRole('menu')).getByText('ana@reconpay.dev'))
    expect(screen.getByRole('menu')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Fora' }))
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })
})

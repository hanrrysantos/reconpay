import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TopNav } from './TopNav'

const navigation = vi.hoisted(() => ({ pathname: '/painel' }))
vi.mock('next/navigation', () => ({ usePathname: () => navigation.pathname }))
const signOut = vi.hoisted(() => vi.fn())
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ user: { name: 'Ana Souza' }, signOut }) }))

const ITEMS: Array<[string, string]> = [
  ['Visão geral', '/painel'],
  ['Conciliações', '/conciliacoes'],
  ['Divergências', '/divergencias'],
  ['Transações', '/transacoes'],
  ['Importações', '/importacoes'],
  ['Fechamento', '/fechamento'],
]

beforeEach(() => { navigation.pathname = '/painel' })

describe('TopNav', () => {
  it('renderiza os seis itens do produto na ordem, com seus links', () => {
    render(<TopNav />)

    const links = within(screen.getByRole('navigation', { name: 'Principal' })).getAllByRole('link')
    expect(links.map(link => [link.textContent, link.getAttribute('href')])).toEqual(ITEMS)
  })

  it.each(ITEMS)('marca "%s" como ativo na rota %s e só ele', (label, href) => {
    navigation.pathname = href
    render(<TopNav />)

    const nav = screen.getByRole('navigation', { name: 'Principal' })
    expect(within(nav).getByRole('link', { name: label })).toHaveAttribute('aria-current', 'page')
    const active = within(nav).getAllByRole('link').filter(link => link.getAttribute('aria-current') === 'page')
    expect(active).toHaveLength(1)
  })

  it('a marca leva ao painel (LAND-01 AC3)', () => {
    render(<TopNav />)

    expect(screen.getByRole('link', { name: 'ReconPay' })).toHaveAttribute('href', '/painel')
  })

  it('a marca é a de fundo claro, toda em preto e sem a frase (BRD-02 AC2, AC4)', () => {
    render(<TopNav />)

    const brand = screen.getByRole('link', { name: 'ReconPay' })
    expect(brand.querySelector('img')).toHaveAttribute('src', '/brand/reconpay-mark-on-light.svg')
    expect(brand.querySelector('img')).toHaveAttribute('alt', '')
    expect(within(brand).getByText('Pay').parentElement).toHaveClass('text-foreground')
    expect(within(brand).getByText('Pay')).not.toHaveClass('text-primary')
    expect(screen.queryByText('Conciliação financeira com clareza')).not.toBeInTheDocument()
  })

  it('marca o item da seção numa sub-rota', () => {
    navigation.pathname = '/importacoes/novo'
    render(<TopNav />)

    expect(screen.getByRole('link', { name: 'Importações' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Visão geral' })).not.toHaveAttribute('aria-current')
  })

  it('não renderiza o sino de notificações e mostra o avatar com as iniciais do usuário', () => {
    render(<TopNav />)

    expect(screen.queryByRole('button', { name: /notifica/i })).not.toBeInTheDocument()
    expect(screen.getByLabelText('Ana Souza')).toHaveTextContent('AS')
  })

  it('renderiza o seletor de empresa recebido à direita', () => {
    render(<TopNav merchantSwitcher={<button>Empresa X</button>} />)

    expect(screen.getByRole('button', { name: 'Empresa X' })).toBeInTheDocument()
  })

  it('"Sair" encerra a sessão (migrado do dashboard antigo)', async () => {
    render(<TopNav />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Sair' }))
    expect(signOut).toHaveBeenCalledOnce()
  })
})

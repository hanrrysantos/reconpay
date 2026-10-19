import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LandingPage } from './LandingPage'

const GITHUB = 'https://github.com/hanrrysantos/reconpay'
const API_DOCS = 'https://reconpay.hanrry.top/swagger-ui/index.html'

const hrefOf = (link: HTMLElement) => link.getAttribute('href')

describe('LandingPage', () => {
  it('menu tem a marca, as três âncoras, "Entrar" e "Criar conta" (LAND-02 AC2, AC3)', () => {
    render(<LandingPage />)

    const nav = screen.getByRole('navigation', { name: 'Principal' })
    expect(within(nav).getAllByRole('link').map(link => [link.textContent, hrefOf(link)])).toEqual([
      ['reconpay', '/'],
      ['Como funciona', '#como-funciona'],
      ['Divergências', '#divergencias'],
      ['Fechamento', '#fechamento'],
      ['Entrar', '/login'],
      ['Criar conta', '/register'],
    ])
  })

  it('abertura com o selo, o único h1, o texto e os dois botões (LAND-02 AC1, AC2; LAND-05 AC1)', () => {
    render(<LandingPage />)

    expect(screen.getByText('Conciliação financeira para fintechs, gateways e marketplaces')).toBeInTheDocument()
    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('O que você vendeu, o que caiu na conta e onde não bate.')
    expect(screen.getByText('O ReconPay cruza suas vendas com a liquidação da adquirente e o extrato do banco, aponta cada divergência e fecha o período com rastro para auditoria.')).toBeInTheDocument()

    const hero = headings[0].closest('section')!
    expect(within(hero).getByRole('link', { name: 'Criar conta' })).toHaveAttribute('href', '/register')
    expect(within(hero).getByRole('link', { name: 'Entrar na minha conta' })).toHaveAttribute('href', '/login')
    expect(screen.queryByText(/Ver demonstração/)).not.toBeInTheDocument()
  })

  it('prévia do painel é uma imagem com nome, alvo de "Fechamento" (LAND-02 AC3; LAND-05 AC1)', () => {
    render(<LandingPage />)

    const preview = screen.getByRole('img', { name: 'Prévia do painel de fechamento do ReconPay' })
    expect(preview).toHaveAttribute('id', 'fechamento')
    expect(preview).toHaveTextContent('Faltam 12 divergências para fechar setembro')
    expect(preview).toHaveTextContent('Fechamento · Setembro de 2026')
    expect(preview).toHaveTextContent('Taxa de match')
    expect(preview).toHaveTextContent('94,2%')
    expect(preview).toHaveTextContent('1.209 de 1.284 itens')
    for (const value of ['R$ 10.389,38', 'R$ 121.212,50', 'R$ 3.787,50']) expect(preview).toHaveTextContent(value)
    expect(preview).toHaveTextContent('Divergências abertas')
    expect(preview).toHaveTextContent('12 abertas')
    for (const row of ['PED-10021', 'Liquidação ausente', 'R$ 145,22', 'PED-10022', 'Taxa divergente', '−R$ 990,10', 'PED-10047', 'Status inconsistente', 'R$ 0,00', 'PED-10063', 'Parcelas divergentes', 'R$ 12,40']) {
      expect(preview).toHaveTextContent(row)
    }
  })

  it('rodapé com a marca, GitHub, Documentação da API, Entrar e o crédito (LAND-02 AC2, AC4)', () => {
    render(<LandingPage />)

    const footer = screen.getByRole('contentinfo')
    const links = within(footer).getAllByRole('link')
    expect(links.map(link => [link.textContent, hrefOf(link)])).toEqual([
      ['reconpay', '/'],
      ['GitHub', GITHUB],
      ['Documentação da API', API_DOCS],
      ['Entrar', '/login'],
    ])
    for (const external of links.slice(1, 3)) {
      expect(external).toHaveAttribute('target', '_blank')
      expect(external).toHaveAttribute('rel', 'noopener noreferrer')
    }
    expect(footer).toHaveTextContent('Feito por Hanrry Santos · 2026')
  })

  it('tem as regiões nav, main e footer (LAND-05 AC1)', () => {
    render(<LandingPage />)

    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })
})

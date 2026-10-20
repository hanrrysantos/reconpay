import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AuthPanel } from './AuthPanel'

describe('AuthPanel', () => {
  it('a marca leva de volta à landing', () => {
    render(<AuthPanel title="Título" lead="Texto do painel" />)

    expect(screen.getByRole('link', { name: 'ReconPay, voltar para o início' })).toHaveAttribute('href', '/')
  })

  it('mostra a marca, o título e o texto recebidos e o rodapé fixo', () => {
    render(<AuthPanel title="Seu primeiro fechamento começa aqui." lead="Crie a conta e importe os arquivos do mês." />)

    const panel = screen.getByRole('complementary')
    expect(screen.getByRole('heading', { level: 2, name: 'Seu primeiro fechamento começa aqui.' })).toBeInTheDocument()
    expect(screen.getByText('Crie a conta e importe os arquivos do mês.')).toBeInTheDocument()
    expect(screen.getByText('Conciliação financeira para fintechs, gateways e marketplaces')).toBeInTheDocument()
  })

  it('mostra a marca para fundo escuro, com "Pay" em verde-limão e a frase (BRD-02 AC1)', () => {
    render(<AuthPanel title="Título" lead="Texto do painel" />)

    const panel = screen.getByRole('complementary')
    expect(panel.querySelector('img')).toHaveAttribute('src', '/brand/reconpay-mark-on-dark.svg')
    expect(screen.getByText('Pay').parentElement).toHaveTextContent(/^ReconPay$/)
    expect(screen.getByText('Pay').parentElement).toHaveClass('text-background')
    expect(screen.getByText('Pay')).toHaveClass('text-primary')
    expect(screen.getByText('Conciliação financeira com clareza')).toBeInTheDocument()
    expect(panel).not.toHaveTextContent('reconpay')
  })

  it('esconde o texto e o rodapé abaixo de 1024 px e mantém marca e título', () => {
    render(<AuthPanel title="Título" lead="Texto do painel" />)

    expect(screen.getByText('Texto do painel')).toHaveClass('hidden', 'lg:block')
    expect(screen.getByText('Conciliação financeira para fintechs, gateways e marketplaces').parentElement).toHaveClass('hidden', 'lg:flex')
    expect(screen.getByText('Pay').parentElement).not.toHaveClass('hidden')
    expect(screen.getByText('Conciliação financeira com clareza')).not.toHaveClass('hidden')
    expect(screen.getByRole('heading', { name: 'Título' })).not.toHaveClass('hidden')
  })
})

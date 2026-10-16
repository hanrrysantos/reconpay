import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { makeRun } from './fixtures'
import { BlockError, DashboardSkeleton, EmptyOnboarding, Forbidden, NetworkBanner, NoMerchant, RunFailedCard } from './states'

describe('DashboardSkeleton', () => {
  it('mostra a grade bento em carregamento sem nenhum número (DASH-06 AC1, AC7)', () => {
    const { container } = render(<DashboardSkeleton />)
    const status = screen.getByRole('status', { name: 'Carregando painel' })
    expect(status).toHaveAttribute('aria-busy', 'true')
    expect(container.querySelectorAll('[data-skeleton-block]')).toHaveLength(8)
    expect(status.textContent).not.toMatch(/\d/)
  })
})

describe('NoMerchant', () => {
  it('mostra "Nenhuma empresa no seu acesso" com "Cadastrar empresa" (DASH-06 AC2)', () => {
    render(<NoMerchant />)
    expect(screen.getByRole('heading', { name: 'Nenhuma empresa no seu acesso' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Cadastrar empresa' })).toHaveAttribute('href', '/empresas?cadastrar=1')
  })
})

describe('EmptyOnboarding', () => {
  const steps = () => within(screen.getByRole('list', { name: 'Primeiros passos' })).getAllByRole('listitem')

  it('lista liquidação, extrato e conciliar (DASH-06 AC3)', () => {
    render(<EmptyOnboarding imports={undefined} onRun={vi.fn()} />)
    expect(steps().map(step => step.textContent)).toEqual([
      expect.stringContaining('Importar liquidação'),
      expect.stringContaining('Importar extrato'),
      expect.stringContaining('Conciliar período'),
    ])
  })

  it.each([
    [{ settlementImportedAt: null, bankStatementImportedAt: null }, ['current', 'blocked', 'blocked']],
    [{ settlementImportedAt: '2026-10-05T12:00:00Z', bankStatementImportedAt: null }, ['done', 'current', 'blocked']],
    [{ settlementImportedAt: '2026-10-05T12:00:00Z', bankStatementImportedAt: '2026-10-05T12:30:00Z' }, ['done', 'done', 'current']],
  ])('estado de cada passo vem dos imports (DASH-06 AC3) %#', (imports, expected) => {
    render(<EmptyOnboarding imports={imports} onRun={vi.fn()} />)
    expect(steps().map(step => step.getAttribute('data-state'))).toEqual(expected)
  })

  it('passo de import atual leva a /importacoes', () => {
    render(<EmptyOnboarding imports={{ settlementImportedAt: '2026-10-05T12:00:00Z', bankStatementImportedAt: null }} onRun={vi.fn()} />)
    expect(within(steps()[1]).getByRole('link', { name: 'Importar' })).toHaveAttribute('href', '/importacoes')
  })

  it('com os dois imports, "Conciliar período" dispara o run', async () => {
    const onRun = vi.fn()
    render(<EmptyOnboarding imports={{ settlementImportedAt: 'a', bankStatementImportedAt: 'b' }} onRun={onRun} />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Conciliar período' }))
    expect(onRun).toHaveBeenCalledOnce()
  })
})

describe('NetworkBanner', () => {
  it('mostra "Não foi possível conectar à API" e "Tentar de novo" refaz (DASH-06 AC4)', async () => {
    const onRetry = vi.fn()
    render(<NetworkBanner onRetry={onRetry} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Não foi possível conectar à API')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Tentar de novo' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})

describe('Forbidden', () => {
  it('mostra "Você não tem acesso a esta empresa" e oferece o menu de empresa (DASH-06 AC6)', () => {
    render(<Forbidden merchantMenu={<button type="button">Trocar empresa</button>} />)
    expect(screen.getByRole('heading', { name: 'Você não tem acesso a esta empresa' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Trocar empresa' })).toBeInTheDocument()
  })
})

describe('RunFailedCard', () => {
  const run = makeRun({ id: 'run-42', status: 'FAILED', errorMessage: 'Falha ao processar o run.' })

  it('mostra a mensagem pública e o id do run (DASH-05 AC5)', () => {
    render(<RunFailedCard run={run} onRetry={vi.fn()} />)
    expect(screen.getByText('Falhou')).toBeInTheDocument()
    expect(screen.getByText('Falha ao processar o run.')).toBeInTheDocument()
    expect(screen.getByText('run-42')).toBeInTheDocument()
  })

  it('"Copiar" copia o id do run (DASH-05 AC5)', async () => {
    const user = userEvent.setup()
    render(<RunFailedCard run={run} onRetry={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: 'Copiar' }))
    expect(await navigator.clipboard.readText()).toBe('run-42')
    expect(screen.getByRole('button', { name: 'Copiado' })).toBeInTheDocument()
  })

  it('"Rodar de novo" dispara o run (DASH-05 AC5)', async () => {
    const onRetry = vi.fn()
    render(<RunFailedCard run={run} onRetry={onRetry} />)
    await userEvent.setup().click(screen.getByRole('button', { name: 'Rodar de novo' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})

describe('BlockError', () => {
  it('mostra o erro do bloco e a ação recarrega só ele (DASH-03 AC9)', async () => {
    const onRetry = vi.fn()
    render(<BlockError message="O gráfico não carregou" action="Recarregar gráfico" onRetry={onRetry} />)
    expect(screen.getByRole('alert')).toHaveTextContent('O gráfico não carregou')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Recarregar gráfico' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })
})

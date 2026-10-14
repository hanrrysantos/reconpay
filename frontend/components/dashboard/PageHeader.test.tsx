import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { makePeriod, makeRun, makeSummary, september } from './fixtures'
import { PageHeader } from './PageHeader'
import type { DashboardView } from './view'

function renderHeader(view: DashboardView, window = september) {
  const onRun = vi.fn()
  const onExport = vi.fn()
  render(<PageHeader view={view} window={window} periodMenu={<span>menu de período</span>} onRun={onRun} onExport={onExport} />)
  return { user: userEvent.setup(), onRun, onExport }
}

describe('PageHeader', () => {
  it('período aberto: manchete com N da soma de openByType (DASH-02 AC6)', async () => {
    const summary = makeSummary({ openByType: { MISSING_SETTLEMENT: 7, FEE_DIVERGENCE: 4, ORPHAN_BANK_CREDIT: 1 } })
    const { user, onRun } = renderHeader({ kind: 'open', data: { period: makePeriod(), summary } })

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Faltam 12 divergências para fechar setembro')
    expect(screen.getByText('menu de período')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Conciliar período' }))
    expect(onRun).toHaveBeenCalledTimes(1)
  })

  it('uma divergência aberta usa o singular', () => {
    renderHeader({ kind: 'open', data: { period: makePeriod(), summary: makeSummary({ openByType: { FEE_DIVERGENCE: 1 } }) } })
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Falta 1 divergência para fechar setembro')
  })

  it('sem resumo carregado não mostra contagem inventada (DASH-06 AC7)', () => {
    renderHeader({ kind: 'open', data: { period: makePeriod(), summary: undefined } })
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Fechamento de setembro')
    expect(screen.getByRole('heading', { level: 1 }).textContent).not.toMatch(/\d/)
  })

  it('travado: "<Mês> fechado em X%" e "Exportar relatório" como ação principal (DASH-07 AC5)', async () => {
    const period = makePeriod({ locked: true, matchRate: '0.9420', openAmount: '0.00' })
    const { user, onExport, onRun } = renderHeader({ kind: 'locked', data: { period, summary: makeSummary({ openByType: {} }) } })

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Setembro fechado em 94,2%')
    expect(screen.getByRole('button', { name: 'Conciliar período' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Exportar relatório' }))
    expect(onExport).toHaveBeenCalledTimes(1)
    expect(onRun).not.toHaveBeenCalled()
  })

  it('rodando: ação desabilitada "Conciliando…" (DASH-05 AC2)', () => {
    renderHeader({ kind: 'running', run: makeRun({ status: 'RUNNING' }) })

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Conciliando setembro…')
    expect(screen.getByRole('button', { name: 'Conciliando…' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Conciliar período' })).not.toBeInTheDocument()
  })

  it('vazio: "<Mês> ainda não foi conciliado"', () => {
    const october = { fromDate: '2026-10-01', toDate: '2026-10-31' }
    renderHeader({ kind: 'empty', window: october }, october)

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Outubro ainda não foi conciliado')
  })

  it('janela personalizada usa o intervalo de datas na manchete', () => {
    const custom = { fromDate: '2026-09-10', toDate: '2026-09-20' }
    renderHeader({ kind: 'empty', window: custom }, custom)

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('10/09/2026 – 20/09/2026 ainda não foi conciliado')
  })
})

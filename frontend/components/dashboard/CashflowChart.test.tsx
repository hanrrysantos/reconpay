import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { PeriodSummaryDay } from '@/lib/api'
import { CashflowChart } from './CashflowChart'

function septemberDays(bank: (index: number) => string | null = () => '10000.00'): PeriodSummaryDay[] {
  return Array.from({ length: 30 }, (_, index) => ({
    date: `2026-09-${String(index + 1).padStart(2, '0')}`,
    expectedNetAmount: index === 26 ? '15060.00' : '10000.00',
    settledNetAmount: '10000.00',
    bankReceivedAmount: index === 26 ? (bank(index) === null ? null : '13582.00') : bank(index),
    itemCount: 40,
    matchedCount: 38,
  }))
}

const points = (d: string | null | undefined) => (d ?? '').match(/[ML]/g)?.length ?? 0
const series = (container: HTMLElement, name: string) => container.querySelector(`[data-series="${name}"]`)

describe('CashflowChart', () => {
  it('desenha a linha esperada, a área recebida e a faixa de falta com um ponto por dia (DASH-03 AC4)', () => {
    const { container } = render(<CashflowChart days={septemberDays()} bankReceivedAmount="303582.00" expectedNetAmount="305060.00" />)

    expect(points(series(container, 'expected')?.getAttribute('d'))).toBe(30)
    expect(points(series(container, 'received')?.getAttribute('d'))).toBe(30)
    expect(series(container, 'received-area')).toBeInTheDocument()
    expect(points(series(container, 'gap')?.getAttribute('d'))).toBe(60)
    expect(screen.getByText('R$ 303.582,00')).toBeInTheDocument()
    expect(screen.getByText('de R$ 305.060,00 esperado')).toBeInTheDocument()
  })

  it('eixo X marca o primeiro e o último dia da janela', () => {
    render(<CashflowChart days={septemberDays()} bankReceivedAmount="303582.00" expectedNetAmount="305060.00" />)
    expect(screen.getByText('01 set')).toBeInTheDocument()
    expect(screen.getByText('30 set')).toBeInTheDocument()
  })

  it('hover num dia mostra o esperado e o que faltou nesse dia (DASH-03 AC5)', () => {
    const { container } = render(<CashflowChart days={septemberDays()} bankReceivedAmount="303582.00" expectedNetAmount="305060.00" />)

    fireEvent.mouseEnter(container.querySelector('[data-day="2026-09-27"]')!)

    const tooltip = screen.getByRole('tooltip')
    expect(tooltip).toHaveTextContent('27 set · esperado R$ 15.060,00')
    expect(tooltip).toHaveTextContent('Faltaram R$ 1.478,00')
  })

  it('dia sem falta não mostra valor faltando', () => {
    const { container } = render(<CashflowChart days={septemberDays()} bankReceivedAmount="303582.00" expectedNetAmount="305060.00" />)

    fireEvent.mouseEnter(container.querySelector('[data-day="2026-09-02"]')!)

    expect(screen.getByRole('tooltip')).toHaveTextContent('02 set · esperado R$ 10.000,00')
    expect(screen.getByRole('tooltip')).toHaveTextContent('Nada faltou')
  })

  it('banco nulo: sem área nem faixa e aviso "Extrato não disponível para este run"', () => {
    const { container } = render(<CashflowChart days={septemberDays(() => null)} bankReceivedAmount={null} expectedNetAmount="305060.00" />)

    expect(series(container, 'expected')).toBeInTheDocument()
    expect(series(container, 'received-area')).not.toBeInTheDocument()
    expect(series(container, 'gap')).not.toBeInTheDocument()
    expect(screen.getByText('Extrato não disponível para este run')).toBeInTheDocument()

    fireEvent.mouseEnter(container.querySelector('[data-day="2026-09-27"]')!)
    expect(screen.getByRole('tooltip')).not.toHaveTextContent('Faltaram')
  })

  it('janela sem vendas mostra "Sem vendas nesta janela" e nenhuma série', () => {
    const days = septemberDays().map(day => ({ ...day, expectedNetAmount: '0.00', settledNetAmount: '0.00', bankReceivedAmount: '0.00', itemCount: 0, matchedCount: 0 }))
    const { container } = render(<CashflowChart days={days} bankReceivedAmount="0.00" expectedNetAmount="0.00" />)

    expect(screen.getByText('Sem vendas nesta janela')).toBeInTheDocument()
    expect(series(container, 'expected')).not.toBeInTheDocument()
  })
})

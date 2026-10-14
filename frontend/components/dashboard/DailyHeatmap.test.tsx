import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { PeriodSummaryDay } from '@/lib/api'
import { DailyHeatmap } from './DailyHeatmap'

const day = (date: string, itemCount: number, matchedCount: number): PeriodSummaryDay => ({
  date, expectedNetAmount: '0.00', settledNetAmount: '0.00', bankReceivedAmount: null, itemCount, matchedCount,
})

const september = { fromDate: '2026-09-01', toDate: '2026-09-06' }
const days = [
  day('2026-09-01', 100, 100),
  day('2026-09-02', 100, 97),
  day('2026-09-03', 100, 95),
  day('2026-09-04', 100, 91),
  day('2026-09-05', 100, 85),
  day('2026-09-06', 0, 0),
]

const grid = () => screen.getByRole('grid', { name: 'Conciliação por dia' })
const cell = (name: RegExp) => within(grid()).getByRole('gridcell', { name })

describe('DailyHeatmap', () => {
  it('colore cada dia pela faixa do percentual conciliado (DASH-03 AC7)', () => {
    render(<DailyHeatmap days={days} window={september} />)

    expect(cell(/^01\/09: 100%$/)).toHaveAttribute('data-band', 'full')
    expect(cell(/^02\/09: 97%$/)).toHaveAttribute('data-band', 'soft')
    expect(cell(/^03\/09: 95%$/)).toHaveAttribute('data-band', 'neutral')
    expect(cell(/^04\/09: 91%$/)).toHaveAttribute('data-band', 'warn')
    expect(cell(/^05\/09: 85%$/)).toHaveAttribute('data-band', 'bad')
    expect(cell(/^05\/09/)).toHaveClass('bg-danger', 'text-white')
    expect(cell(/^01\/09/)).toHaveClass('bg-primary')
  })

  it('dia sem item fica neutro e sem percentual', () => {
    render(<DailyHeatmap days={days} window={september} />)

    const empty = cell(/^06\/09/)
    expect(empty).toHaveAttribute('data-band', 'empty')
    expect(empty).toHaveAccessibleName('06/09: sem itens')
    expect(empty).toHaveClass('bg-white')
    expect(empty.textContent).toBe('6')
  })

  it('grade começa na segunda e alinha o primeiro dia da janela ao seu dia da semana', () => {
    render(<DailyHeatmap days={days} window={september} />)

    expect(within(grid()).getAllByRole('columnheader').map(header => header.textContent)).toEqual(['S', 'T', 'Q', 'Q', 'S', 'S', 'D'])
    const cells = within(grid()).getAllByRole('gridcell')
    // 1º de setembro de 2026 é terça: uma célula vazia antes dele.
    expect(cells[0]).not.toHaveAttribute('data-band')
    expect(cells[1]).toHaveAccessibleName('01/09: 100%')
  })

  it('mostra a legenda de "< 90%" a "100%"', () => {
    render(<DailyHeatmap days={days} window={september} />)
    expect(screen.getByText('< 90%')).toBeInTheDocument()
    expect(screen.getByText('100%')).toBeInTheDocument()
  })
})

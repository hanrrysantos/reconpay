import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { makePeriod, makeRun, makeSummary } from './fixtures'
import { MatchRingCard } from './MatchRingCard'

const arc = (container: HTMLElement) => container.querySelector('[data-arc]')!
const count = (label: string) => screen.getByText(label).previousElementSibling

describe('MatchRingCard', () => {
  it('desenha o arco proporcional a matchRate e mostra as contagens do período e do resumo (DASH-03 AC1)', () => {
    const { container } = render(<MatchRingCard period={makePeriod()} summary={makeSummary()} run={makeRun()} />)

    expect(arc(container)).toHaveAttribute('stroke-dasharray', '414.3 439.8')
    expect(screen.getByTestId('ring-value')).toHaveTextContent('94,2%')
    expect(count('bateram')).toHaveTextContent('1.209')
    expect(count('divergentes')).toHaveTextContent('75')
    expect(count('abertas')).toHaveTextContent('12')
    expect(screen.getByText('1.209 de 1.284 itens conciliados')).toBeInTheDocument()
  })

  it('sem taxa do run anterior não mostra comparativo (DASH-03 AC8)', () => {
    render(<MatchRingCard period={makePeriod()} summary={makeSummary({ previousRunMatchRate: null })} run={makeRun()} />)
    expect(screen.queryByText(/vs run anterior/)).not.toBeInTheDocument()
  })

  it('com taxa do run anterior mostra a diferença em pontos percentuais (DASH-03 AC8)', () => {
    render(<MatchRingCard period={makePeriod()} summary={makeSummary({ previousRunMatchRate: '0.9240' })} run={makeRun()} />)
    expect(screen.getByText('+1,8 pp vs run anterior')).toBeInTheDocument()
  })

  it('janela com zero items mostra 0% e "0 de 0"', () => {
    const period = makePeriod({ totalItems: 0, matchedCount: 0, divergentCount: 0, matchRate: null, openAmount: '0.00' })
    const { container } = render(<MatchRingCard period={period} summary={makeSummary({ openByType: {} })} run={makeRun()} />)

    expect(arc(container)).toHaveAttribute('stroke-dasharray', '0 439.8')
    expect(screen.getByTestId('ring-value')).toHaveTextContent('0%')
    expect(screen.getByText('0 de 0 itens conciliados')).toBeInTheDocument()
  })

  it('sem resumo não inventa a contagem de abertas (DASH-06 AC7)', () => {
    render(<MatchRingCard period={makePeriod()} summary={undefined} run={makeRun()} />)
    expect(count('abertas')).toHaveTextContent('—')
  })

  it('run em andamento com progresso mostra o percentual processado (DASH-05 AC3)', () => {
    const run = makeRun({ status: 'RUNNING', processedItems: 500, totalItems: 1250 })
    const { container } = render(<MatchRingCard period={makePeriod()} summary={makeSummary()} run={run} />)

    expect(screen.getByText('RUNNING')).toBeInTheDocument()
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '40')
    expect(screen.getByTestId('ring-value')).toHaveTextContent('40%')
    expect(screen.getByText('500 de 1.250 itens processados')).toBeInTheDocument()
    expect(arc(container)).toHaveAttribute('stroke-dasharray', '175.9 439.8')
    expect(screen.getByText('Os blocos abaixo mostram o run anterior')).toBeInTheDocument()
  })

  it('run em andamento sem progresso mostra anel indeterminado sem número (DASH-05 AC3)', () => {
    const run = makeRun({ status: 'PENDING', processedItems: null, totalItems: null })
    render(<MatchRingCard period={undefined} summary={undefined} run={run} />)

    const progress = screen.getByRole('progressbar')
    expect(progress).not.toHaveAttribute('aria-valuenow')
    expect(screen.queryByTestId('ring-value')).not.toBeInTheDocument()
    expect(within(progress.closest('section')!).queryByText(/\d/)).not.toBeInTheDocument()
    expect(screen.getByText('PENDING')).toBeInTheDocument()
  })

  it('período travado mostra o selo "Travado"', () => {
    render(<MatchRingCard period={makePeriod({ locked: true })} summary={makeSummary({ openByType: {} })} run={makeRun()} />)

    expect(screen.getByText('Travado')).toBeInTheDocument()
    expect(count('abertas')).toHaveTextContent('0')
  })
})

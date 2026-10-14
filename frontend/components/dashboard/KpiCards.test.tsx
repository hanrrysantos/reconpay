import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { PeriodSummaryDay } from '@/lib/api'
import { makePeriod, makeSummary } from './fixtures'
import { KpiCards } from './KpiCards'

const card = (label: string) => screen.getByRole('heading', { name: label }).closest('article')!

const day = (date: string, itemCount: number, matchedCount: number): PeriodSummaryDay => ({
  date, expectedNetAmount: '1000.00', settledNetAmount: '1000.00', bankReceivedAmount: '1000.00', bankShortfallAmount: '0.00',
  itemCount, matchedCount,
})

describe('KpiCards', () => {
  it('"Valor em aberto" é o openAmount formatado (DASH-03 AC2)', () => {
    render(<KpiCards period={makePeriod({ openAmount: '10389.38' })} summary={makeSummary()} />)
    expect(card('Valor em aberto')).toHaveTextContent('R$ 10.389,38')
    expect(card('Valor em aberto')).toHaveTextContent('12 divergências')
  })

  it('valor com mais de 13 dígitos não passa por float', () => {
    render(<KpiCards period={makePeriod({ openAmount: '12345678901234567.89' })} summary={makeSummary()} />)
    expect(card('Valor em aberto')).toHaveTextContent('R$ 12.345.678.901.234.567,89')
  })

  it('vendido bruto, taxas com percentual do bruto e excesso sobre contrato vêm do resumo (DASH-03 AC3)', () => {
    render(<KpiCards period={makePeriod()} summary={makeSummary()} />)

    expect(card('Vendido bruto')).toHaveTextContent('R$ 125.000,00')
    expect(card('Vendido bruto')).toHaveTextContent('1.284 itens na janela')
    expect(card('Taxas cobradas')).toHaveTextContent('R$ 3.787,50')
    expect(card('Taxas cobradas')).toHaveTextContent('3,03% do bruto')
    expect(card('Taxas cobradas')).toHaveTextContent('+R$ 35,50 acima do contrato')
  })

  it('o percentual do bruto é o feePercent do servidor, sem conta local (AD-010)', () => {
    render(<KpiCards period={makePeriod()} summary={makeSummary({ feePercent: '9.99' })} />)
    expect(card('Taxas cobradas')).toHaveTextContent('9,99% do bruto')
  })

  it('sem feePercent (bruto zero) não mostra percentual do bruto', () => {
    render(<KpiCards period={makePeriod()} summary={makeSummary({ grossAmount: '0.00', feeAmount: '0.00', feePercent: null })} />)
    expect(card('Taxas cobradas')).not.toHaveTextContent('do bruto')
  })

  it('sem variação do servidor não mostra comparativo (DASH-03 AC8)', () => {
    render(<KpiCards period={makePeriod()} summary={makeSummary({ previousWindowGrossAmount: '0.00', previousWindowGrossChangePercent: null })} />)
    expect(card('Vendido bruto')).not.toHaveTextContent(/vs /)
  })

  it('com variação do servidor compara com o mês anterior (DASH-03 AC8)', () => {
    render(<KpiCards period={makePeriod()} summary={makeSummary({ previousWindowGrossAmount: '117700.00', previousWindowGrossChangePercent: '6.2' })} />)
    expect(card('Vendido bruto')).toHaveTextContent('+6,2% vs ago')
  })

  it('janela personalizada compara com a janela anterior', () => {
    const custom = { fromDate: '2026-09-10', toDate: '2026-09-20' }
    render(<KpiCards period={makePeriod(custom)} summary={makeSummary({ ...custom, previousWindowGrossAmount: '130000.00', previousWindowGrossChangePercent: '-3.8' })} />)
    expect(card('Vendido bruto')).toHaveTextContent('−3,8% vs janela anterior')
  })

  it('sem resumo mostra só os números do período (DASH-06 AC7)', () => {
    render(<KpiCards period={makePeriod()} summary={undefined} />)

    expect(card('Valor em aberto')).toHaveTextContent('R$ 10.389,38')
    expect(card('Vendido bruto')).not.toHaveTextContent(/R\$/)
    expect(card('Taxas cobradas')).not.toHaveTextContent(/\d/)
    expect(card('Valor em aberto')).not.toHaveTextContent('divergências')
  })

  it('sparkline usa os últimos 7 dias da série', () => {
    const days = Array.from({ length: 10 }, (_, index) => day(`2026-09-${String(index + 1).padStart(2, '0')}`, 10, index))
    render(<KpiCards period={makePeriod()} summary={makeSummary({ days })} />)

    const path = card('Valor em aberto').querySelector('svg[viewBox="0 0 100 28"] path')!.getAttribute('d')!
    expect(path.match(/[ML]/g)).toHaveLength(7)
  })
})

import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { DiscrepancyType } from '@/lib/api'
import { TypeDonut } from './TypeDonut'

const legend = () => within(screen.getByRole('list', { name: 'Tipos de divergência' })).queryAllByRole('listitem')

describe('TypeDonut', () => {
  it('desenha um segmento por tipo e a legenda com a contagem de abertas de cada tipo (DASH-03 AC6)', () => {
    const { container } = render(<TypeDonut openByType={{ ORPHAN_BANK_CREDIT: 1, MISSING_SETTLEMENT: 7, FEE_DIVERGENCE: 4 }} />)

    expect(screen.getByTestId('donut-total')).toHaveTextContent('12 abertas')
    expect(legend().map(item => item.textContent)).toEqual(['Liquidação ausente7', 'Taxa divergente4', 'Crédito sem venda1'])
    expect(container.querySelectorAll('[data-segment]')).toHaveLength(3)
  })

  it('zero abertas mostra anel vazio e "0 abertas"', () => {
    const { container } = render(<TypeDonut openByType={{}} />)

    expect(screen.getByTestId('donut-total')).toHaveTextContent('0 abertas')
    expect(container.querySelectorAll('[data-segment]')).toHaveLength(0)
    expect(legend()).toHaveLength(0)
  })

  it('usa os rótulos em português dos 11 tipos', () => {
    const types: DiscrepancyType[] = [
      'MISSING_SETTLEMENT', 'ORPHAN_SETTLEMENT', 'INCORRECT_AMOUNT', 'FEE_DIVERGENCE', 'STATUS_MISMATCH', 'PAYMENT_METHOD_MISMATCH',
      'INSTALLMENTS_MISMATCH', 'BANK_AMOUNT_MISMATCH', 'MISSING_BANK_CREDIT', 'ORPHAN_BANK_CREDIT', 'AMBIGUOUS_BANK_MATCH',
    ]
    render(<TypeDonut openByType={Object.fromEntries(types.map(type => [type, 1]))} />)

    expect(legend().map(item => item.textContent?.replace(/\d+$/, ''))).toEqual([
      'Liquidação ausente', 'Liquidação órfã', 'Valor bruto incorreto', 'Taxa divergente', 'Status inconsistente', 'Método divergente',
      'Parcelas divergentes', 'Valor no banco divergente', 'Sem linha no extrato', 'Crédito sem venda', 'Extrato ambíguo',
    ])
  })
})

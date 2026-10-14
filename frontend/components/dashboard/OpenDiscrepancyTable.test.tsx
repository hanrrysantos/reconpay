import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ReconciliationItem } from '@/lib/api'
import { makeItem } from './fixtures'
import { OpenDiscrepancyTable, type OpenDiscrepancyTableProps } from './OpenDiscrepancyTable'

const items: ReconciliationItem[] = [
  makeItem(),
  makeItem({
    id: 'item-2', externalReference: 'PED-10022', paymentMethod: 'PIX', installments: 1, transactionDate: '2026-09-24',
    expectedNetAmount: '2287.13', settlementNetAmount: '1297.03',
    discrepancies: [{ id: 'd-2', status: 'OPEN', type: 'FEE_DIVERGENCE', expectedValue: '2287.13', actualValue: '1297.03' }],
  }),
  makeItem({ id: 'item-3', externalReference: 'PED-10023', expectedNetAmount: '100.00', settlementNetAmount: '90.00',
    discrepancies: [{ id: 'd-3', status: 'OPEN', type: 'INCORRECT_AMOUNT', expectedValue: null, actualValue: null }] }),
  makeItem({ id: 'item-4', externalReference: 'PED-10024', expectedNetAmount: '50.00', settlementNetAmount: '45.00',
    discrepancies: [{ id: 'd-4', status: 'OPEN', type: 'STATUS_MISMATCH', expectedValue: null, actualValue: null }] }),
  makeItem({ id: 'item-5', externalReference: 'PED-10025' }),
]

function renderTable(overrides: Partial<OpenDiscrepancyTableProps> = {}) {
  const props: OpenDiscrepancyTableProps = {
    items, total: 12, types: ['MISSING_SETTLEMENT', 'FEE_DIVERGENCE'], typeFilter: null,
    onFilter: vi.fn(), onResolve: vi.fn(), locked: false, ...overrides,
  }
  render(<OpenDiscrepancyTable {...props} />)
  return { user: userEvent.setup(), ...props }
}

const bodyRows = () => within(screen.getAllByRole('rowgroup')[1]).getAllByRole('row')

describe('OpenDiscrepancyTable', () => {
  it('lista no máximo 4 divergências, na ordem recebida (maior diferença primeiro) (DASH-04 AC1)', () => {
    renderTable()
    expect(bodyRows().map(row => within(row).getAllByRole('cell')[0].textContent))
      .toEqual(['CRPED-10021Crédito 3x · 26 set', 'PIXPED-10022Pix · 24 set', 'CRPED-10023Crédito 3x · 26 set', 'CRPED-10024Crédito 3x · 26 set'])
  })

  it('mostra referência, método, data, tipo, esperado, diferença e status (DASH-04 AC1)', () => {
    renderTable()
    const cells = within(bodyRows()[1]).getAllByRole('cell').map(cell => cell.textContent)
    expect(cells.slice(0, 5)).toEqual(['PIXPED-10022Pix · 24 set', 'Taxa divergente', '2.287,13', '−990,10', 'Aberta'])
  })

  it('sem liquidação, a diferença é o esperado negativo', () => {
    renderTable()
    expect(within(bodyRows()[0]).getAllByRole('cell')[3]).toHaveTextContent('−4.870,00')
  })

  it('rodapé "Mostrando N de M" e link para /divergencias', () => {
    renderTable()
    expect(screen.getByText('Mostrando 4 de 12 · maior diferença primeiro')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Ver todas as 12/ })).toHaveAttribute('href', '/divergencias')
  })

  it('filtro por tipo pede só aquele tipo e marca o ativo (DASH-04 AC2)', async () => {
    const { user, onFilter } = renderTable()
    expect(screen.getByRole('button', { name: 'Todas' })).toHaveAttribute('aria-pressed', 'true')
    await user.click(screen.getByRole('button', { name: 'Taxa divergente' }))
    expect(onFilter).toHaveBeenCalledWith('FEE_DIVERGENCE')
  })

  it('com filtro ativo, a linha mostra a divergência aberta daquele tipo (DASH-04 AC2)', () => {
    const item = makeItem({ discrepancies: [
      { id: 'd-a', status: 'OPEN', type: 'MISSING_SETTLEMENT', expectedValue: null, actualValue: null },
      { id: 'd-b', status: 'OPEN', type: 'FEE_DIVERGENCE', expectedValue: null, actualValue: null },
    ] })
    renderTable({ items: [item], total: 1, typeFilter: 'FEE_DIVERGENCE' })
    expect(screen.getByRole('button', { name: 'Taxa divergente' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(bodyRows()[0]).getAllByRole('cell')[1]).toHaveTextContent('Taxa divergente')
  })

  it('"Resolver" entrega o item e a divergência da linha', async () => {
    const { user, onResolve } = renderTable()
    await user.click(screen.getByRole('button', { name: 'Resolver PED-10022' }))
    expect(onResolve).toHaveBeenCalledWith(items[1], items[1].discrepancies[0])
  })

  it('status exibidos só como Aberta, Aceita, Ajustada ou Baixada', () => {
    const statuses = ['OPEN', 'ACCEPTED', 'ADJUSTED', 'WRITTEN_OFF'] as const
    renderTable({ items: statuses.map((status, index) => makeItem({
      id: `s-${index}`, externalReference: `S-${index}`,
      discrepancies: [{ id: `d-${index}`, status, type: 'FEE_DIVERGENCE', expectedValue: null, actualValue: null }],
    })) })
    expect(bodyRows().map(row => within(row).getAllByRole('cell')[4].textContent)).toEqual(['Aberta', 'Aceita', 'Ajustada', 'Baixada'])
    expect(screen.queryByText('Em análise')).not.toBeInTheDocument()
  })

  it('período travado: somente leitura, a ação vira "Detalhes"', () => {
    renderTable({ locked: true })
    expect(screen.queryByRole('button', { name: /^Resolver/ })).not.toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /^Detalhes/ })).toHaveLength(4)
  })

  it('sem divergências abertas mostra o estado vazio sem rodapé', () => {
    renderTable({ items: [], total: 0 })
    expect(screen.getByText('Nenhuma divergência aberta')).toBeInTheDocument()
    expect(screen.queryByText(/Mostrando/)).not.toBeInTheDocument()
  })
})

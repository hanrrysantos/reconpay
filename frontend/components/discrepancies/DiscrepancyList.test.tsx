import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Discrepancy, DiscrepancyType, ReconciliationItem } from '@/lib/api'
import type { StatusFilter } from '@/lib/discrepancies'
import { createQueryClient } from '@/lib/queries'
import { DiscrepancyList } from './DiscrepancyList'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), getRunItems: vi.fn() }))

const discrepancy = (id: string, type: DiscrepancyType, status: Discrepancy['status']): Discrepancy =>
  ({ id, type, status, expectedValue: null, actualValue: null })
const item = (id: string, discrepancies: Discrepancy[], fields: Partial<ReconciliationItem> = {}): ReconciliationItem => ({
  id, reconciliationRunId: 'r-1', externalReference: `REF-${id}`, result: 'DIVERGENT', internalTransactionId: 't-1', externalSettlementId: 's-1',
  transactionAmount: '300.00', expectedNetAmount: '290.73', settlementAmount: '300.00', settlementNetAmount: '280.00', netDifference: '-10.73',
  paymentMethod: 'CREDIT_CARD', installments: 3, transactionStatus: 'APPROVED', settlementStatus: 'APPROVED', transactionDate: '2026-10-03',
  settlementDate: '2026-10-10', discrepancies, createdAt: '2026-10-15T12:05:00Z', ...fields,
})
const twoOpen = item('1', [discrepancy('d-1', 'FEE_DIVERGENCE', 'OPEN'), discrepancy('d-2', 'STATUS_MISMATCH', 'OPEN')])
const orphan = item('2', [discrepancy('d-3', 'ORPHAN_SETTLEMENT', 'OPEN')], {
  internalTransactionId: null, transactionAmount: null, expectedNetAmount: null, paymentMethod: null, installments: null,
  transactionStatus: null, transactionDate: null, netDifference: '48.50',
})

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getRunItems).mockReset()
})
afterEach(() => client.clear())

function show(props: { status?: StatusFilter; type?: DiscrepancyType | null; page?: number } = {}) {
  const onPageChange = vi.fn()
  const onOpen = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <DiscrepancyList merchantId="m-1" runId="r-1" status={props.status ?? 'OPEN'} type={props.type ?? null} page={props.page ?? 0} onPageChange={onPageChange} onOpen={onOpen} />
    </QueryClientProvider>,
  )
  return { onPageChange, onOpen, user: userEvent.setup() }
}

const rowsText = (table: HTMLElement) => within(table).getAllByRole('row').slice(1)
  .map(row => within(row).getAllByRole('cell').slice(0, 5).map(cell => cell.textContent))

describe('DiscrepancyList', () => {
  it('mostra uma linha por divergência com Transação, Tipo, Esperado, Diferença e Status (DIV-01 AC2, edge de duas divergências)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [twoOpen, orphan], totalPages: 1 })
    show()

    const table = await screen.findByRole('table', { name: 'Divergências' })
    expect(within(table).getAllByRole('columnheader').map(header => header.textContent)).toEqual(['Transação', 'Tipo', 'Esperado', 'Diferença', 'Status', 'Ação'])
    expect(rowsText(table)).toEqual([
      ['REF-1Crédito 3x · 03/10/2026', 'Taxa divergente', 'R$ 290,73', '−R$ 10,73', 'Aberta'],
      ['REF-1Crédito 3x · 03/10/2026', 'Status inconsistente', 'R$ 290,73', '−R$ 10,73', 'Aberta'],
      ['REF-2— · —', 'Liquidação órfã', '—', '+R$ 48,50', 'Aberta'],
    ])
    expect(within(table).getAllByRole('button', { name: /^Abrir/ })).toHaveLength(3)
  })

  it.each<[StatusFilter, DiscrepancyType | null, object]>([
    ['OPEN', null, { discrepancyStatus: 'OPEN' }],
    ['ACCEPTED', 'FEE_DIVERGENCE', { discrepancyStatus: 'ACCEPTED', discrepancyType: 'FEE_DIVERGENCE' }],
    ['ALL', null, { result: 'DIVERGENT' }],
  ])('consulta a página com os filtros recebidos: status %s, tipo %s (DIV-02)', async (status, type, filters) => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [], totalPages: 0 })
    show({ status, type, page: 0 })
    await screen.findByText('Nenhuma divergência para os filtros escolhidos.')
    expect(api.getRunItems).toHaveBeenCalledWith('jwt', 'm-1', 'r-1', filters, 0)
  })

  it('mostra só as divergências do status escolhido, mesmo que o item tenha outras (DIV-01 AC5)', async () => {
    const mixed = item('3', [discrepancy('d-4', 'FEE_DIVERGENCE', 'ACCEPTED'), discrepancy('d-5', 'MISSING_SETTLEMENT', 'OPEN')])
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [mixed], totalPages: 1 })
    show({ status: 'ACCEPTED' })

    const table = await screen.findByRole('table', { name: 'Divergências' })
    expect(rowsText(table).map(row => [row[1], row[4]])).toEqual([['Taxa divergente', 'Aceita']])
  })

  it('"Abrir" entrega as linhas da página e a posição da linha clicada (DIV-06 AC1)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [twoOpen, orphan], totalPages: 1 })
    const { onOpen, user } = show()

    await user.click(await screen.findByRole('button', { name: 'Abrir REF-1 · Status inconsistente' }))

    expect(onOpen).toHaveBeenCalledTimes(1)
    const [rows, index] = onOpen.mock.calls[0]
    expect(rows.map((row: { item: ReconciliationItem; discrepancyId: string }) => row.discrepancyId)).toEqual(['d-1', 'd-2', 'd-3'])
    expect(index).toBe(1)
  })

  it('pagina com "Página X de Y", Anterior e Próxima (DIV-03 AC7)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [orphan], totalPages: 3 })
    const { onPageChange, user } = show({ page: 1 })

    const nav = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(within(nav).getByText('Página 2 de 3')).toBeInTheDocument()
    expect(api.getRunItems).toHaveBeenCalledWith('jwt', 'm-1', 'r-1', { discrepancyStatus: 'OPEN' }, 1)
    await user.click(within(nav).getByRole('button', { name: 'Anterior' }))
    expect(onPageChange).toHaveBeenLastCalledWith(0)
    await user.click(within(nav).getByRole('button', { name: 'Próxima' }))
    expect(onPageChange).toHaveBeenLastCalledWith(2)
  })

  it('sem divergências mostra o estado vazio (DIV-03 AC8)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [], totalPages: 0 })
    show()
    expect(await screen.findByText('Nenhuma divergência para os filtros escolhidos.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('erro na busca mostra a mensagem e "Tentar novamente" refaz a busca (DIV-03 AC9)', async () => {
    vi.mocked(api.getRunItems)
      .mockRejectedValueOnce(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
      .mockResolvedValueOnce({ content: [orphan], totalPages: 1 })
    const { user } = show()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Você não tem acesso a este comerciante')
    await user.click(within(alert).getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('table', { name: 'Divergências' })).toHaveTextContent('REF-2')
  })
})

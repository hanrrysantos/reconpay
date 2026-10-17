import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { TYPE_LABELS } from '@/components/dashboard/format'
import * as api from '@/lib/api'
import type { ReconciliationItem } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { RunItems } from './RunItems'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({ ...await importOriginal<typeof import('@/lib/api')>(), getRunItems: vi.fn() }))

const item = (id: string, fields: Partial<ReconciliationItem> = {}): ReconciliationItem => ({
  id, reconciliationRunId: 'r-1', externalReference: `REF-${id}`, result: 'MATCHED', internalTransactionId: 't-1', externalSettlementId: 's-1',
  transactionAmount: '100.00', expectedNetAmount: '95.00', settlementAmount: '100.00', settlementNetAmount: '95.00', netDifference: '0.00',
  paymentMethod: 'CREDIT_CARD', installments: 1, transactionStatus: 'APPROVED', settlementStatus: 'APPROVED', transactionDate: '2026-10-03',
  settlementDate: '2026-10-05', discrepancies: [], createdAt: '2026-10-15T12:05:00Z', ...fields,
})

const matched = item('1')
const missingSettlement = item('2', {
  result: 'DIVERGENT', externalSettlementId: null, settlementAmount: null, settlementNetAmount: null, settlementStatus: null, settlementDate: null,
  transactionAmount: '5000.00', expectedNetAmount: '4870.00', netDifference: '-4870.00', paymentMethod: 'PIX', transactionDate: '2026-10-07',
  discrepancies: [{ id: 'd-1', status: 'OPEN', type: 'MISSING_SETTLEMENT', expectedValue: null, actualValue: null }],
})
const orphan = item('3', {
  result: 'DIVERGENT', internalTransactionId: null, transactionAmount: null, expectedNetAmount: null, transactionStatus: null, transactionDate: null,
  paymentMethod: null, settlementNetAmount: '48.50', netDifference: '48.50',
  discrepancies: [{ id: 'd-2', status: 'OPEN', type: 'ORPHAN_SETTLEMENT', expectedValue: null, actualValue: null }],
})
const twoDiscrepancies = item('4', {
  result: 'DIVERGENT', settlementNetAmount: '93.10', netDifference: '-1.90', paymentMethod: 'DEBIT_CARD',
  discrepancies: [
    { id: 'd-3', status: 'OPEN', type: 'FEE_DIVERGENCE', expectedValue: '5.00', actualValue: '6.90' },
    { id: 'd-4', status: 'ACCEPTED', type: 'STATUS_MISMATCH', expectedValue: 'APPROVED', actualValue: 'CANCELED' },
  ],
})

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  vi.mocked(api.getRunItems).mockReset()
})
afterEach(() => {
  client.clear()
  vi.useRealTimers()
})

function show({ polling = false }: { polling?: boolean } = {}) {
  render(
    <QueryClientProvider client={client}>
      <RunItems merchantId="m-1" runId="r-1" polling={polling} />
    </QueryClientProvider>,
  )
}

const rowsOf = (table: HTMLElement) => within(table).getAllByRole('row').slice(1)
const cellsOf = (row: HTMLElement) => within(row).getAllByRole('cell').map(cell => cell.textContent)
const findTable = () => screen.findByRole('table', { name: 'Itens' })
const optionsOf = (select: HTMLElement) => within(select).getAllByRole('option').map(option => option.textContent)

describe('RunItems', () => {
  it('mostra as 8 colunas, os dois lados, a Diferença do servidor e o resultado; sem divergência a coluna fica vazia (REC-08 AC2)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [matched, missingSettlement], totalPages: 1 })
    show()

    const table = await findTable()
    expect(within(table).getAllByRole('columnheader').map(th => th.textContent)).toEqual([
      'Referência', 'Data da venda', 'Método', 'Líquido esperado', 'Líquido liquidado', 'Diferença', 'Resultado', 'Divergências',
    ])
    const [matchedRow, missingRow] = rowsOf(table)
    expect(cellsOf(matchedRow)).toEqual(['REF-1', '03/10/2026', 'Crédito', 'R$ 95,00', 'R$ 95,00', 'R$ 0,00', 'Bateu', ''])
    expect(cellsOf(missingRow)).toEqual([
      'REF-2', '07/10/2026', 'Pix', 'R$ 4.870,00', '—', '−R$ 4.870,00', 'Divergente', 'Liquidação ausente · Aberta',
    ])
    expect(api.getRunItems).toHaveBeenCalledWith('jwt', 'm-1', 'r-1', {}, 0)
    expect(screen.queryByRole('navigation', { name: 'Paginação' })).toBeNull()
  })

  it('liquidação órfã mostra — no lado da venda (REC-08 AC2)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [orphan], totalPages: 1 })
    show()

    const [row] = rowsOf(await findTable())
    expect(cellsOf(row)).toEqual(['REF-3', '—', '—', '—', 'R$ 48,50', '+R$ 48,50', 'Divergente', 'Liquidação órfã · Aberta'])
  })

  it('duas divergências no mesmo item aparecem como dois selos com tipo e status (REC-08 AC3)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [twoDiscrepancies], totalPages: 1 })
    show()

    const [row] = rowsOf(await findTable())
    const badges = within(within(row).getAllByRole('cell')[7]).getAllByRole('listitem').map(badge => badge.textContent)
    expect(badges).toEqual(['Taxa divergente · Aberta', 'Status inconsistente · Aceita'])
  })

  it('Resultado envia result=MATCHED/DIVERGENT e volta para a página 1; Todos remove o parâmetro (REC-09 AC4)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [matched], totalPages: 3 })
    show()

    const resultFilter = await screen.findByRole('combobox', { name: 'Resultado' })
    expect(optionsOf(resultFilter)).toEqual(['Todos', 'Bateram', 'Divergentes'])
    await userEvent.click(await screen.findByRole('button', { name: 'Próxima' }))
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', {}, 1)

    await userEvent.selectOptions(resultFilter, 'Divergentes')
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', { result: 'DIVERGENT' }, 0)
    expect(await screen.findByText('Página 1 de 3')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Próxima' }))
    await userEvent.selectOptions(resultFilter, 'Bateram')
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', { result: 'MATCHED' }, 0)

    await userEvent.click(await screen.findByRole('button', { name: 'Próxima' }))
    await userEvent.selectOptions(resultFilter, 'Todos')
    const [, , , filters, page] = vi.mocked(api.getRunItems).mock.lastCall!
    expect(filters).toStrictEqual({})
    expect(page).toBe(0)
  })

  it('Tipo de divergência envia discrepancyType e volta para a página 1; Todos remove o parâmetro (REC-09 AC5)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [matched], totalPages: 3 })
    show()

    const typeFilter = await screen.findByRole('combobox', { name: 'Tipo de divergência' })
    expect(optionsOf(typeFilter)).toEqual(['Todos', ...Object.values(TYPE_LABELS)])
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Resultado' }), 'Divergentes')
    await userEvent.click(await screen.findByRole('button', { name: 'Próxima' }))
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', { result: 'DIVERGENT' }, 1)

    await userEvent.selectOptions(typeFilter, 'Taxa divergente')
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', { result: 'DIVERGENT', discrepancyType: 'FEE_DIVERGENCE' }, 0)

    await userEvent.click(await screen.findByRole('button', { name: 'Próxima' }))
    await userEvent.selectOptions(typeFilter, 'Todos')
    const [, , , filters, page] = vi.mocked(api.getRunItems).mock.lastCall!
    expect(filters).toStrictEqual({ result: 'DIVERGENT' })
    expect(page).toBe(0)
  })

  it('pagina como a lista de runs, desabilitando Anterior na primeira e Próxima na última (REC-09 AC6)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [matched], totalPages: 2 })
    show()

    const nav = await screen.findByRole('navigation', { name: 'Paginação' })
    expect(within(nav).getByText('Página 1 de 2')).toBeInTheDocument()
    expect(within(nav).getByRole('button', { name: 'Anterior' })).toBeDisabled()
    await userEvent.click(within(nav).getByRole('button', { name: 'Próxima' }))
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', {}, 1)
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()
    const last = screen.getByRole('navigation', { name: 'Paginação' })
    expect(within(last).getByRole('button', { name: 'Próxima' })).toBeDisabled()
    await userEvent.click(within(last).getByRole('button', { name: 'Anterior' }))
    expect(api.getRunItems).toHaveBeenLastCalledWith('jwt', 'm-1', 'r-1', {}, 0)
  })

  it('sem itens para os filtros mostra a mensagem de vazio e mantém os filtros (REC-09 AC7)', async () => {
    vi.mocked(api.getRunItems).mockResolvedValue({ content: [], totalPages: 0 })
    show()

    expect(await screen.findByText('Nenhum item encontrado para os filtros escolhidos.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
    expect(screen.getByRole('combobox', { name: 'Resultado' })).toBeInTheDocument()
  })

  it('mostra o erro da busca e Tentar novamente refaz a consulta (REC-09 AC10)', async () => {
    vi.mocked(api.getRunItems)
      .mockRejectedValueOnce(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
      .mockResolvedValueOnce({ content: [matched], totalPages: 1 })
    show()

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Você não tem acesso a este comerciante')
    await userEvent.click(within(alert).getByRole('button', { name: 'Tentar novamente' }))

    expect(await findTable()).toHaveTextContent('REF-1')
    expect(api.getRunItems).toHaveBeenCalledTimes(2)
  })

  describe('atualização (REC-09 AC8)', () => {
    const tick = (ms: number) => act(async () => {
      await vi.advanceTimersByTimeAsync(ms - 2)
      await vi.advanceTimersByTimeAsync(1)
      await vi.advanceTimersByTimeAsync(1)
    })

    it('com polling recarrega os itens a cada 3 s', async () => {
      vi.useFakeTimers()
      vi.mocked(api.getRunItems)
        .mockResolvedValueOnce({ content: [], totalPages: 0 })
        .mockResolvedValue({ content: [matched], totalPages: 1 })
      show({ polling: true })

      await tick(2)
      expect(screen.getByText('Nenhum item encontrado para os filtros escolhidos.')).toBeInTheDocument()
      await tick(2996)
      expect(api.getRunItems).toHaveBeenCalledTimes(1)
      await tick(4)
      expect(api.getRunItems).toHaveBeenCalledTimes(2)
      expect(rowsOf(screen.getByRole('table', { name: 'Itens' }))).toHaveLength(1)
      await tick(3000)
      expect(api.getRunItems).toHaveBeenCalledTimes(3)
    })

    it('sem polling não recarrega', async () => {
      vi.useFakeTimers()
      vi.mocked(api.getRunItems).mockResolvedValue({ content: [matched], totalPages: 1 })
      show({ polling: false })

      await tick(2)
      await tick(15000)
      expect(api.getRunItems).toHaveBeenCalledTimes(1)
    })
  })
})

import { describe, expect, it } from 'vitest'
import type { Discrepancy, ReconciliationItem } from './api'
import { STATUS_FILTERS, itemFilters, rowsOf } from './discrepancies'

const discrepancy = (id: string, type: Discrepancy['type'], status: Discrepancy['status']): Discrepancy =>
  ({ id, type, status, expectedValue: null, actualValue: null })
const item = (id: string, discrepancies: Discrepancy[]): ReconciliationItem => ({
  id, reconciliationRunId: 'r-1', externalReference: `REF-${id}`, result: 'DIVERGENT', internalTransactionId: 't-1', externalSettlementId: 's-1',
  transactionAmount: '100.00', expectedNetAmount: '95.00', settlementAmount: '100.00', settlementNetAmount: '90.00', netDifference: '-5.00',
  paymentMethod: 'CREDIT_CARD', installments: 1, transactionStatus: 'APPROVED', settlementStatus: 'APPROVED', transactionDate: '2026-10-03',
  settlementDate: '2026-10-05', discrepancies, createdAt: '2026-10-15T12:05:00Z',
})

const twoOpen = item('1', [discrepancy('d-1', 'FEE_DIVERGENCE', 'OPEN'), discrepancy('d-2', 'STATUS_MISMATCH', 'OPEN')])
const mixed = item('2', [discrepancy('d-3', 'FEE_DIVERGENCE', 'ACCEPTED'), discrepancy('d-4', 'MISSING_SETTLEMENT', 'OPEN')])
const ids = (rows: ReturnType<typeof rowsOf>) => rows.map(row => `${row.item.id}:${row.discrepancyId}`)

describe('STATUS_FILTERS', () => {
  it('lista Abertas, Aceitas, Ajustadas, Baixadas e Todas, nessa ordem (DIV-02)', () => {
    expect(STATUS_FILTERS).toEqual([
      { value: 'OPEN', label: 'Abertas' },
      { value: 'ACCEPTED', label: 'Aceitas' },
      { value: 'ADJUSTED', label: 'Ajustadas' },
      { value: 'WRITTEN_OFF', label: 'Baixadas' },
      { value: 'ALL', label: 'Todas' },
    ])
  })
})

describe('itemFilters', () => {
  it('Todas pede os itens divergentes, sem status (DIV-01 AC3)', () => {
    expect(itemFilters('ALL', null)).toEqual({ result: 'DIVERGENT' })
  })

  it('um status vira discrepancyStatus e um tipo vira discrepancyType (DIV-02)', () => {
    expect(itemFilters('ACCEPTED', 'FEE_DIVERGENCE')).toEqual({ discrepancyStatus: 'ACCEPTED', discrepancyType: 'FEE_DIVERGENCE' })
    expect(itemFilters('OPEN', null)).toEqual({ discrepancyStatus: 'OPEN' })
    expect(itemFilters('ALL', 'MISSING_SETTLEMENT')).toEqual({ result: 'DIVERGENT', discrepancyType: 'MISSING_SETTLEMENT' })
  })
})

describe('rowsOf', () => {
  it('um item com duas divergências abertas vira duas linhas, na ordem (DIV-02 edge)', () => {
    expect(ids(rowsOf([twoOpen, mixed], 'OPEN', null))).toEqual(['1:d-1', '1:d-2', '2:d-4'])
  })

  it('com Aceitas, mostra só a divergência aceita de um item que também tem uma aberta (DIV-02 edge)', () => {
    expect(ids(rowsOf([mixed], 'ACCEPTED', null))).toEqual(['2:d-3'])
  })

  it('o tipo restringe as linhas ao mesmo tipo (DIV-01 AC5)', () => {
    expect(ids(rowsOf([twoOpen, mixed], 'OPEN', 'FEE_DIVERGENCE'))).toEqual(['1:d-1'])
    expect(ids(rowsOf([twoOpen, mixed], 'ALL', 'FEE_DIVERGENCE'))).toEqual(['1:d-1', '2:d-3'])
  })

  it('Todas mostra todas as divergências dos itens (DIV-01 AC3)', () => {
    expect(ids(rowsOf([twoOpen, mixed], 'ALL', null))).toEqual(['1:d-1', '1:d-2', '2:d-3', '2:d-4'])
  })

  it('cada linha leva o item inteiro, para a gaveta mostrar os dois lados (DIV-06)', () => {
    expect(rowsOf([mixed], 'OPEN', null)[0].item).toBe(mixed)
  })
})

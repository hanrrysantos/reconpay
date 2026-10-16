import { describe, expect, it } from 'vitest'
import { ApiError } from './api'
import {
  FILE_TOO_LARGE_MESSAGE, IMPORT_KINDS, MAX_CSV_BYTES, formatImportedAt, importFailure, importedRowsMessage, isTooLarge, kindFromSlug,
} from './imports'

describe('IMPORT_KINDS', () => {
  it('lista Transações, Liquidação e Extrato nessa ordem, com os slugs de ?tipo= (IMP-01)', () => {
    expect(IMPORT_KINDS.map(({ kind, slug, label }) => ({ kind, slug, label }))).toEqual([
      { kind: 'transactions', slug: 'transacoes', label: 'Transações' },
      { kind: 'settlements', slug: 'liquidacao', label: 'Liquidação' },
      { kind: 'bankStatements', slug: 'extrato', label: 'Extrato' },
    ])
  })

  it('traz as colunas exigidas pela API, na ordem do cabeçalho (IMP-02)', () => {
    expect(IMPORT_KINDS.map(({ columns }) => columns)).toEqual([
      ['externalReference', 'amount', 'paymentMethod', 'installments', 'status', 'transactionDate'],
      ['externalReference', 'amount', 'netAmount', 'paymentMethod', 'installments', 'status', 'settlementDate'],
      ['lineReference', 'externalReference', 'amount', 'movementDate'],
    ])
  })

  it('nomeia cada tipo no estado vazio da lista (IMP-11)', () => {
    expect(IMPORT_KINDS.map(({ noun }) => noun)).toEqual(['transações', 'liquidação', 'extrato'])
  })
})

describe('kindFromSlug', () => {
  it.each([
    ['transacoes', 'transactions'],
    ['liquidacao', 'settlements'],
    ['extrato', 'bankStatements'],
  ])('abre a aba de ?tipo=%s (IMP-01)', (slug, kind) => {
    expect(kindFromSlug(slug)).toBe(kind)
  })

  it.each([null, '', 'xyz', 'EXTRATO'])('abre Transações com ?tipo=%s ausente ou desconhecido (IMP-01)', slug => {
    expect(kindFromSlug(slug)).toBe('transactions')
  })
})

describe('isTooLarge', () => {
  it('aceita exatamente 5 MB e recusa um byte a mais (IMP-05)', () => {
    expect(MAX_CSV_BYTES).toBe(5_242_880)
    expect(isTooLarge({ size: 5_242_880 })).toBe(false)
    expect(isTooLarge({ size: 5_242_881 })).toBe(true)
    expect(isTooLarge({ size: 0 })).toBe(false)
  })

  it('usa o mesmo texto da API para arquivo grande demais (IMP-05)', () => {
    expect(FILE_TOO_LARGE_MESSAGE).toBe('Arquivo CSV excede o tamanho máximo permitido de 5MB')
  })
})

describe('importFailure', () => {
  it('lê todos os erros por linha, na ordem da resposta (IMP-06)', () => {
    const error = new ApiError('Erro na importação do CSV', 400, 'VALIDATION_ERROR', {
      rowErrors: [{ row: 7, message: 'Valor inválido' }, { row: 2, message: 'Referência externa é obrigatória' }],
    })
    expect(importFailure(error)).toEqual({
      message: 'Erro na importação do CSV',
      rowErrors: [{ row: 7, message: 'Valor inválido' }, { row: 2, message: 'Referência externa é obrigatória' }],
      conflictingReferences: [],
    })
  })

  it('lê todas as referências em conflito (IMP-07)', () => {
    const error = new ApiError('Referência externa já importada: TX-1', 409, 'CONFLICT', { conflictingReferences: ['TX-1', 'TX-2'] })
    expect(importFailure(error)).toEqual({ message: 'Referência externa já importada: TX-1', rowErrors: [], conflictingReferences: ['TX-1', 'TX-2'] })
  })

  it('devolve só a mensagem de um erro sem details (IMP-08)', () => {
    expect(importFailure(new ApiError('Janela travada de 2026-10-01 a 2026-10-31', 409, 'CONFLICT')))
      .toEqual({ message: 'Janela travada de 2026-10-01 a 2026-10-31', rowErrors: [], conflictingReferences: [] })
  })

  it('devolve só a mensagem de um Error comum, como a falha de rede (IMP-08)', () => {
    expect(importFailure(new Error('Não foi possível conectar à API. Tente novamente.')))
      .toEqual({ message: 'Não foi possível conectar à API. Tente novamente.', rowErrors: [], conflictingReferences: [] })
  })

  it.each([
    ['rowErrors não é lista', { rowErrors: 'linha 2' }],
    ['item sem row numérico', { rowErrors: [{ row: '2', message: 'x' }] }],
    ['item sem message em texto', { rowErrors: [{ row: 2, message: 3 }] }],
    ['referência que não é texto', { conflictingReferences: ['TX-1', 2] }],
    ['conflictingReferences não é lista', { conflictingReferences: 'TX-1' }],
  ])('ignora details malformado: %s', (_case, details) => {
    expect(importFailure(new ApiError('Falhou', 400, 'VALIDATION_ERROR', details)))
      .toEqual({ message: 'Falhou', rowErrors: [], conflictingReferences: [] })
  })
})

describe('importedRowsMessage', () => {
  it('cita o arquivo e o total de linhas do lote aceito (IMP-04)', () => {
    expect(importedRowsMessage({ id: 'i-1', merchantId: 'm-1', fileName: 'extrato.csv', totalRows: 3, createdAt: '2026-10-16T13:05:00Z' }))
      .toBe('extrato.csv importado: 3 linha(s)')
  })
})

describe('formatImportedAt', () => {
  it('formata como dd/mm/aaaa hh:mm no fuso indicado (IMP-10)', () => {
    expect(formatImportedAt('2026-10-16T13:05:00Z', 'America/Sao_Paulo')).toBe('16/10/2026 10:05')
    expect(formatImportedAt('2026-01-02T03:04:00Z', 'UTC')).toBe('02/01/2026 03:04')
  })

  it('usa o dia do fuso, não o do UTC, perto da meia-noite (IMP-10)', () => {
    expect(formatImportedAt('2026-10-17T01:30:00Z', 'America/Sao_Paulo')).toBe('16/10/2026 22:30')
  })
})

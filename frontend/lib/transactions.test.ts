import { describe, expect, it } from 'vitest'
import { ApiError } from './api'
import {
  AMOUNT_MESSAGE, DATE_REQUIRED_MESSAGE, FINAL_STATUSES, FUTURE_DATE_MESSAGE, INVALID_WINDOW_MESSAGE, MISSING_FEE_RULE_MESSAGE,
  REFERENCE_REQUIRED_MESSAGE, REFERENCE_TOO_LONG_MESSAGE, TRANSACTION_STATUS_LABELS, dateError, formatTransactionDate,
  invalidWindowError, isMissingFeeRule, parseAmount, parseReference, recentMonths, todayIso,
} from './transactions'

describe('mensagens', () => {
  it('usam os textos da spec', () => {
    expect(REFERENCE_REQUIRED_MESSAGE).toBe('Informe a referência externa')
    expect(REFERENCE_TOO_LONG_MESSAGE).toBe('Use até 100 caracteres')
    expect(AMOUNT_MESSAGE).toBe('Informe um valor maior que zero com até 2 casas decimais')
    expect(DATE_REQUIRED_MESSAGE).toBe('Informe a data da transação')
    expect(FUTURE_DATE_MESSAGE).toBe('Data da transação não pode ser futura')
    expect(INVALID_WINDOW_MESSAGE).toBe('A data inicial deve ser igual ou anterior à final')
  })
})

describe('parseAmount (TXN-06)', () => {
  it('converte a vírgula em ponto e mantém as casas digitadas', () => {
    expect(parseAmount('1234,5')).toBe('1234.5')
    expect(parseAmount('1234.50')).toBe('1234.50')
    expect(parseAmount('0,10')).toBe('0.10')
    expect(parseAmount('100')).toBe('100')
  })

  it('aceita 17 dígitos inteiros e 2 casas', () => {
    expect(parseAmount('12345678901234567,89')).toBe('12345678901234567.89')
  })

  it('recusa 18 dígitos inteiros ou 3 casas', () => {
    expect(parseAmount('123456789012345678')).toBeNull()
    expect(parseAmount('123456789012345678,00')).toBeNull()
    expect(parseAmount('1,234')).toBeNull()
  })

  it('recusa zero, vazio, negativo e texto', () => {
    expect(parseAmount('0')).toBeNull()
    expect(parseAmount('0,00')).toBeNull()
    expect(parseAmount('')).toBeNull()
    expect(parseAmount('   ')).toBeNull()
    expect(parseAmount('-10')).toBeNull()
    expect(parseAmount('dez')).toBeNull()
  })
})

describe('parseReference (TXN-06)', () => {
  it('remove os espaços das extremidades', () => {
    expect(parseReference('  AVULSA-1 ')).toEqual({ value: 'AVULSA-1' })
  })

  it('vazio ou só espaços pede a referência', () => {
    expect(parseReference('')).toEqual({ error: 'Informe a referência externa' })
    expect(parseReference('    ')).toEqual({ error: 'Informe a referência externa' })
  })

  it('aceita 100 caracteres depois de remover os espaços e recusa 101', () => {
    expect(parseReference(` ${'a'.repeat(100)} `)).toEqual({ value: 'a'.repeat(100) })
    expect(parseReference('a'.repeat(101))).toEqual({ error: 'Use até 100 caracteres' })
  })
})

describe('dateError (TXN-06)', () => {
  const today = '2026-10-16'

  it('aceita hoje e o passado', () => {
    expect(dateError('2026-10-16', today)).toBeNull()
    expect(dateError('2025-12-31', today)).toBeNull()
  })

  it('recusa amanhã como data futura', () => {
    expect(dateError('2026-10-17', today)).toBe('Data da transação não pode ser futura')
  })

  it('vazio pede a data', () => {
    expect(dateError('', today)).toBe('Informe a data da transação')
  })
})

describe('todayIso', () => {
  it('devolve a data local aaaa-mm-dd', () => {
    expect(todayIso(new Date(2026, 9, 3, 23, 59))).toBe('2026-10-03')
    expect(todayIso(new Date(2027, 0, 1, 0, 0))).toBe('2027-01-01')
  })
})

describe('recentMonths (TXN-02)', () => {
  it('devolve do mês atual até 11 meses atrás, o mais recente primeiro', () => {
    const months = recentMonths(new Date(2026, 9, 16))
    expect(months).toHaveLength(12)
    expect(months[0]).toEqual({ fromDate: '2026-10-01', toDate: '2026-10-31' })
    expect(months[1]).toEqual({ fromDate: '2026-09-01', toDate: '2026-09-30' })
    expect(months[11]).toEqual({ fromDate: '2025-11-01', toDate: '2025-11-30' })
  })
})

describe('invalidWindowError (TXN-02 AC4)', () => {
  it('aceita intervalo de um dia ou crescente', () => {
    expect(invalidWindowError({ fromDate: '2026-10-10', toDate: '2026-10-10' })).toBeNull()
    expect(invalidWindowError({ fromDate: '2026-10-10', toDate: '2026-10-11' })).toBeNull()
  })

  it('recusa intervalo invertido', () => {
    expect(invalidWindowError({ fromDate: '2026-10-11', toDate: '2026-10-10' })).toBe('A data inicial deve ser igual ou anterior à final')
  })
})

describe('isMissingFeeRule (TXN-10)', () => {
  it('reconhece a mensagem exata da API', () => {
    expect(MISSING_FEE_RULE_MESSAGE).toBe('Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas')
    expect(isMissingFeeRule(new ApiError('Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas', 409, 'CONFLICT'))).toBe(true)
  })

  it('é falso para outro erro ou para um Error com o mesmo texto', () => {
    expect(isMissingFeeRule(new ApiError('Referência externa já cadastrada para este comerciante', 409, 'CONFLICT'))).toBe(false)
    expect(isMissingFeeRule(new Error('Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas'))).toBe(false)
    expect(isMissingFeeRule(null)).toBe(false)
  })
})

describe('rótulos e formatos (TXN-01 AC2)', () => {
  it('rotula os quatro status', () => {
    expect(TRANSACTION_STATUS_LABELS).toEqual({ APPROVED: 'Aprovada', CANCELLED: 'Cancelada', REFUNDED: 'Estornada', CHARGEBACK: 'Chargeback' })
  })

  it('os status finais são Cancelada, Estornada e Chargeback, nessa ordem (TXN-08 AC2)', () => {
    expect(FINAL_STATUSES).toEqual(['CANCELLED', 'REFUNDED', 'CHARGEBACK'])
  })

  it('formata a data como dd/mm/aaaa sem fuso', () => {
    expect(formatTransactionDate('2026-10-03')).toBe('03/10/2026')
  })
})

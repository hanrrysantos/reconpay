import { describe, expect, it } from 'vitest'
import { formatWindow, monthWindow, monthsBack } from './period'

describe('monthWindow', () => {
  it('vai do dia 1 ao último dia do mês civil', () => {
    expect(monthWindow(new Date(2026, 8, 17))).toEqual({ fromDate: '2026-09-01', toDate: '2026-09-30' })
    expect(monthWindow('2026-10-13')).toEqual({ fromDate: '2026-10-01', toDate: '2026-10-31' })
    expect(monthWindow('2026-12-31')).toEqual({ fromDate: '2026-12-01', toDate: '2026-12-31' })
  })

  it('termina fevereiro no dia 29 em ano bissexto e no 28 nos demais', () => {
    expect(monthWindow('2028-02-10')).toEqual({ fromDate: '2028-02-01', toDate: '2028-02-29' })
    expect(monthWindow('2026-02-10')).toEqual({ fromDate: '2026-02-01', toDate: '2026-02-28' })
    expect(monthWindow('2100-02-10')).toEqual({ fromDate: '2100-02-01', toDate: '2100-02-28' })
  })
})

describe('monthsBack', () => {
  it('lista do mês atual ao mês do run mais antigo, mais recente primeiro, sem repetir', () => {
    expect(monthsBack('2026-10-13', '2026-07-20')).toEqual([
      { fromDate: '2026-10-01', toDate: '2026-10-31' },
      { fromDate: '2026-09-01', toDate: '2026-09-30' },
      { fromDate: '2026-08-01', toDate: '2026-08-31' },
      { fromDate: '2026-07-01', toDate: '2026-07-31' },
    ])
  })

  it('atravessa a virada do ano', () => {
    expect(monthsBack('2027-01-05', '2026-11-30').map(window => window.fromDate)).toEqual(['2027-01-01', '2026-12-01', '2026-11-01'])
  })

  it('devolve só o mês atual quando não há run ou o run mais antigo é do mesmo mês', () => {
    expect(monthsBack('2026-10-13', null)).toEqual([{ fromDate: '2026-10-01', toDate: '2026-10-31' }])
    expect(monthsBack('2026-10-13', '2026-10-01')).toEqual([{ fromDate: '2026-10-01', toDate: '2026-10-31' }])
    expect(monthsBack('2026-10-13', '2026-12-01')).toEqual([{ fromDate: '2026-10-01', toDate: '2026-10-31' }])
  })
})

describe('formatWindow', () => {
  it('rotula um mês civil inteiro pelo nome do mês em português', () => {
    expect(formatWindow({ fromDate: '2026-09-01', toDate: '2026-09-30' })).toBe('Setembro de 2026')
    expect(formatWindow({ fromDate: '2028-02-01', toDate: '2028-02-29' })).toBe('Fevereiro de 2028')
  })

  it('rotula uma janela personalizada com as duas datas', () => {
    expect(formatWindow({ fromDate: '2026-09-01', toDate: '2026-09-15' })).toBe('01/09/2026 – 15/09/2026')
    expect(formatWindow({ fromDate: '2026-08-25', toDate: '2026-09-30' })).toBe('25/08/2026 – 30/09/2026')
  })
})

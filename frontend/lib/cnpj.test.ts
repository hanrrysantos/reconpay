import { describe, expect, it } from 'vitest'
import { cnpjDigits, formatCnpj, isValidCnpj, maskCnpj } from './cnpj'

describe('cnpjDigits', () => {
  it('tira a pontuação do CNPJ', () => {
    expect(cnpjDigits('12.345.678/0001-95')).toBe('12345678000195')
  })
})

describe('maskCnpj', () => {
  it.each([
    ['', ''],
    ['12', '12'],
    ['123', '12.3'],
    ['12345', '12.345'],
    ['123456', '12.345.6'],
    ['12345678', '12.345.678'],
    ['123456780', '12.345.678/0'],
    ['123456780001', '12.345.678/0001'],
    ['1234567800019', '12.345.678/0001-9'],
    ['12345678000195', '12.345.678/0001-95'],
  ])('mascara %s progressivamente como %s', (input, expected) => {
    expect(maskCnpj(input)).toBe(expected)
  })

  it('corta em 14 dígitos o que passar disso', () => {
    expect(maskCnpj('123456780001951')).toBe('12.345.678/0001-95')
  })

  it('reformata um CNPJ colado já formatado', () => {
    expect(maskCnpj('12.345.678/0001-95')).toBe('12.345.678/0001-95')
    expect(maskCnpj(' 12 345 678 0001 95 ')).toBe('12.345.678/0001-95')
  })
})

describe('formatCnpj', () => {
  it('formata um documento de 14 dígitos', () => {
    expect(formatCnpj('12345678000195')).toBe('12.345.678/0001-95')
  })

  it.each(['1234567800019', '123456780001951', 'ABC-123'])('deixa %s como está', document => {
    expect(formatCnpj(document)).toBe(document)
  })
})

describe('isValidCnpj', () => {
  it.each(['12345678000195', '12.345.678/0001-95', '11111111000191'])('aceita %s', value => {
    expect(isValidCnpj(value)).toBe(true)
  })

  it.each([
    ['13 dígitos', '1234567800019'],
    ['15 dígitos', '123456780001950'],
    ['primeiro verificador errado', '12345678000185'],
    ['segundo verificador errado', '12345678000194'],
    ['todos iguais (1)', '11111111111111'],
    ['todos iguais (0)', '00000000000000'],
    ['letra no meio', '12A45678000195'],
    ['vazio', ''],
  ])('recusa %s', (_case, value) => {
    expect(isValidCnpj(value)).toBe(false)
  })
})

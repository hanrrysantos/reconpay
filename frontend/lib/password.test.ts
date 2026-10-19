import { describe, expect, it } from 'vitest'
import { PASSWORD_PATTERN, PASSWORD_RULES } from './password'

const rule = (label: string) => {
  const found = PASSWORD_RULES.find(item => item.label === label)
  if (!found) throw new Error(`regra ausente: ${label}`)
  return found
}

describe('regras da senha', () => {
  it('lista as três regras da API na ordem do desenho', () => {
    expect(PASSWORD_RULES.map(item => item.label)).toEqual(['8 caracteres', 'Uma letra maiúscula', 'Um número'])
  })

  it('exige 8 caracteres ou mais', () => {
    expect(rule('8 caracteres').test('Recon20')).toBe(false)
    expect(rule('8 caracteres').test('Recon202')).toBe(true)
  })

  it('exige uma letra maiúscula de A a Z', () => {
    expect(rule('Uma letra maiúscula').test('reconpay20')).toBe(false)
    expect(rule('Uma letra maiúscula').test('reconPay20')).toBe(true)
  })

  it('exige um número de 0 a 9', () => {
    expect(rule('Um número').test('Reconpay')).toBe(false)
    expect(rule('Um número').test('Reconpay7')).toBe(true)
  })

  it('o padrão do campo aceita só a senha que cumpre as três regras', () => {
    const matches = (value: string) => new RegExp(`^(?:${PASSWORD_PATTERN})$`, 'v').test(value)
    expect(matches('Reconpay20')).toBe(true)
    expect(matches('reconpay20')).toBe(false)
    expect(matches('Reconpay')).toBe(false)
    expect(matches('Recon20')).toBe(false)
  })
})

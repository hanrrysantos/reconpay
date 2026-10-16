import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ImportResult } from './ImportResult'

const failure = (message: string, rowErrors: { row: number; message: string }[] = [], conflictingReferences: string[] = []) =>
  ({ failure: { message, rowErrors, conflictingReferences } })

describe('ImportResult', () => {
  it('confirma o lote aceito com o arquivo e o total de linhas (IMP-04)', () => {
    render(<ImportResult outcome={{ batch: { id: 'i-1', merchantId: 'm-1', fileName: 'transacoes-out.csv', totalRows: 42, createdAt: '2026-10-16T13:05:00Z' } }} />)

    expect(screen.getByRole('status')).toHaveTextContent('transacoes-out.csv importado: 42 linha(s)')
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('mostra a mensagem e todos os erros por linha, na ordem recebida (IMP-06)', () => {
    render(<ImportResult outcome={failure('Erro na importação do CSV', [
      { row: 9, message: 'Regra de taxa ativa não encontrada para CREDIT_CARD em 3x' },
      { row: 2, message: 'Referência externa é obrigatória' },
      { row: 4, message: 'Valor inválido' },
    ])} />)

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Erro na importação do CSV')
    const table = within(alert).getByRole('table', { name: 'Erros por linha' })
    expect(within(table).getAllByRole('columnheader').map(th => th.textContent)).toEqual(['Linha', 'Erro'])
    expect(within(table).getAllByRole('row').slice(1).map(row => within(row).getAllByRole('cell').map(cell => cell.textContent))).toEqual([
      ['9', 'Regra de taxa ativa não encontrada para CREDIT_CARD em 3x'],
      ['2', 'Referência externa é obrigatória'],
      ['4', 'Valor inválido'],
    ])
    expect(within(alert).queryByRole('list')).toBeNull()
  })

  it('mostra a mensagem e todas as referências em conflito (IMP-07)', () => {
    render(<ImportResult outcome={failure('Referências externas já cadastradas para este comerciante: PED-1001', [], ['PED-1001', 'PED-1002', 'PED-1003'])} />)

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Referências externas já cadastradas para este comerciante: PED-1001')
    const list = within(alert).getByRole('list', { name: 'Referências já importadas' })
    expect(within(list).getAllByRole('listitem').map(item => item.textContent)).toEqual(['PED-1001', 'PED-1002', 'PED-1003'])
    expect(within(alert).queryByRole('table')).toBeNull()
  })

  it('sem listas, mostra só a mensagem da API (IMP-08)', () => {
    render(<ImportResult outcome={failure('Janela travada de 2026-10-01 a 2026-10-31')} />)

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Janela travada de 2026-10-01 a 2026-10-31')
    expect(within(alert).queryByRole('table')).toBeNull()
    expect(within(alert).queryByRole('list')).toBeNull()
    expect(screen.queryByRole('status')).toBeNull()
  })
})

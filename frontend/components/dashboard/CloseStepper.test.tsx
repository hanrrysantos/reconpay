import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Run } from '@/lib/api'
import { CloseStepper, type CloseStepperProps } from './CloseStepper'
import { makeRun } from './fixtures'

process.env.TZ = 'America/Sao_Paulo'

const imports = { settlementImportedAt: '2026-10-05T12:12:00Z', bankStatementImportedAt: '2026-10-05T12:30:00Z' }

function renderStepper(overrides: Partial<CloseStepperProps> = {}) {
  const props: CloseStepperProps = {
    imports, run: makeRun(), openCount: 12, locked: false, onLock: vi.fn(), onUnlock: vi.fn(), ...overrides,
  }
  render(<CloseStepper {...props} />)
  return { user: userEvent.setup(), ...props }
}

const states = () => screen.getAllByRole('listitem').map(step => step.getAttribute('data-state'))
const step = (name: string) => screen.getAllByRole('listitem').find(item => item.textContent?.includes(name))!

describe('CloseStepper', () => {
  it('mostra os quatro passos na ordem (DASH-02 AC7)', () => {
    renderStepper()
    expect(screen.getAllByRole('listitem').map(item => within(item).getByRole('heading').textContent))
      .toEqual(['Importado', 'Conciliado', 'Zerar divergências', 'Travar período'])
  })

  it('run concluído com abertas: "Zerar divergências" é o passo atual', () => {
    renderStepper()
    expect(states()).toEqual(['done', 'done', 'current', 'pending'])
    expect(step('Zerar divergências')).toHaveAttribute('aria-current', 'step')
    expect(step('Zerar divergências')).toHaveTextContent('12 abertas')
  })

  it('run em andamento: "Conciliado" é o passo atual', () => {
    renderStepper({ run: makeRun({ status: 'RUNNING' }), openCount: null })
    expect(states()).toEqual(['done', 'current', 'pending', 'pending'])
  })

  it('sem run: "Conciliado" é o passo atual', () => {
    renderStepper({ run: null, openCount: null })
    expect(states()).toEqual(['done', 'current', 'pending', 'pending'])
  })

  it('sem extrato importado: "Importado" é o passo atual', () => {
    renderStepper({ imports: { ...imports, bankStatementImportedAt: null }, run: null, openCount: null })
    expect(states()).toEqual(['current', 'pending', 'pending', 'pending'])
    expect(step('Importado')).toHaveTextContent('Extrato sem import')
  })

  it('mostra no passo Importado o horário do último import de liquidação e de extrato (DASH-02 AC8)', () => {
    renderStepper()
    expect(step('Importado')).toHaveTextContent('Liquidação 05/10, 09:12')
    expect(step('Importado')).toHaveTextContent('Extrato 05/10, 09:30')
  })

  it('travado: todos os passos concluídos (DASH-02 AC7)', () => {
    renderStepper({ openCount: 0, locked: true })
    expect(states()).toEqual(['done', 'done', 'done', 'done'])
    expect(step('Travar período')).toHaveTextContent('Travado')
  })

  it.each<[string, Partial<CloseStepperProps>]>([
    ['com divergências abertas', { openCount: 12 }],
    ['com run em andamento', { run: makeRun({ status: 'RUNNING' }) as Run, openCount: 0 }],
    ['com run que falhou', { run: makeRun({ status: 'FAILED' }), openCount: 0 }],
    ['sem run', { run: null, openCount: 0 }],
  ])('"Travar período" desabilitado %s (DASH-07 AC1)', (_, overrides) => {
    renderStepper(overrides)
    expect(screen.getByRole('button', { name: 'Travar período' })).toBeDisabled()
  })

  it('run concluído, zero abertas e destravado: "Travar período" habilitado e trava após confirmar (DASH-07 AC1, AC2)', async () => {
    const { user, onLock } = renderStepper({ openCount: 0 })
    expect(states()).toEqual(['done', 'done', 'done', 'current'])

    await user.click(screen.getByRole('button', { name: 'Travar período' }))
    expect(onLock).not.toHaveBeenCalled()
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))
    expect(onLock).toHaveBeenCalledTimes(1)
  })

  it('"Destravar" pede confirmação antes de chamar onUnlock (DASH-07 AC3)', async () => {
    const { user, onUnlock } = renderStepper({ openCount: 0, locked: true })

    await user.click(screen.getByRole('button', { name: 'Destravar' }))
    expect(onUnlock).not.toHaveBeenCalled()
    const dialog = screen.getByRole('alertdialog')
    expect(dialog).toHaveTextContent('Destravar o período?')

    await user.click(within(dialog).getByRole('button', { name: 'Destravar' }))
    expect(onUnlock).toHaveBeenCalledTimes(1)
  })

  it('cancelar a confirmação não destrava', async () => {
    const { user, onUnlock } = renderStepper({ openCount: 0, locked: true })

    await user.click(screen.getByRole('button', { name: 'Destravar' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(onUnlock).not.toHaveBeenCalled()
  })
})

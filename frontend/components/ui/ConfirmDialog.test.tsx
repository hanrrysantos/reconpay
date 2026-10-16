import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog'

function renderDialog({ pending = false, error = null as string | null } = {}) {
  const onClose = vi.fn()
  const onConfirm = vi.fn()
  render(
    <ConfirmDialog title="Desativar a regra Pix 1x?" warning="Novas transações serão recusadas." confirmLabel="Desativar" pending={pending} error={error} onConfirm={onConfirm} onClose={onClose} />,
  )
  return { onClose, onConfirm, user: userEvent.setup() }
}

describe('ConfirmDialog', () => {
  it('é um alertdialog com título e aviso, e Cancelar recebe o foco', () => {
    renderDialog()
    const dialog = screen.getByRole('alertdialog', { name: 'Desativar a regra Pix 1x?' })
    expect(dialog).toHaveAccessibleDescription('Novas transações serão recusadas.')
    expect(screen.getByRole('button', { name: 'Cancelar' })).toHaveFocus()
  })

  it('Esc fecha sem confirmar (FDES-04)', async () => {
    const { onClose, onConfirm, user } = renderDialog()
    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('Cancelar fecha sem confirmar (FDES-04)', async () => {
    const { onClose, onConfirm, user } = renderDialog()
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('o botão de confirmação chama onConfirm', async () => {
    const { onConfirm, onClose, user } = renderDialog()
    await user.click(screen.getByRole('button', { name: 'Desativar' }))
    expect(onConfirm).toHaveBeenCalledTimes(1)
    expect(onClose).not.toHaveBeenCalled()
  })

  it('o botão de confirmação fica desabilitado em pending', async () => {
    const { onConfirm, user } = renderDialog({ pending: true })
    const button = screen.getByRole('button', { name: 'Desativar' })
    expect(button).toBeDisabled()
    await user.click(button)
    expect(onConfirm).not.toHaveBeenCalled()
  })

  it('mostra o erro com role="alert"', () => {
    renderDialog({ error: 'Regra de taxa não encontrada' })
    expect(screen.getByRole('alert')).toHaveTextContent('Regra de taxa não encontrada')
  })
})

describe('ConfirmDialog com conteúdo extra (TXN-08)', () => {
  function renderWithOptions({ confirmDisabled = false, error = null as string | null } = {}) {
    const onConfirm = vi.fn()
    render(
      <ConfirmDialog title="Alterar status" warning="A mudança não pode ser desfeita." confirmLabel="Confirmar" pending={false} error={error} confirmDisabled={confirmDisabled} onConfirm={onConfirm} onClose={vi.fn()}>
        <div role="radiogroup" aria-label="Novo status" />
      </ConfirmDialog>,
    )
    return { onConfirm, user: userEvent.setup() }
  }

  it('renderiza children entre o aviso e o erro', () => {
    renderWithOptions({ error: 'Transição de status inválida' })
    const warning = screen.getByText('A mudança não pode ser desfeita.')
    const options = screen.getByRole('radiogroup', { name: 'Novo status' })
    const error = screen.getByRole('alert')
    expect(warning.compareDocumentPosition(options) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(options.compareDocumentPosition(error) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('com confirmDisabled, o botão de confirmar fica desabilitado e o clique não chama onConfirm', async () => {
    const { onConfirm, user } = renderWithOptions({ confirmDisabled: true })
    const button = screen.getByRole('button', { name: 'Confirmar' })
    expect(button).toBeDisabled()
    await user.click(button)
    expect(onConfirm).not.toHaveBeenCalled()
  })
})

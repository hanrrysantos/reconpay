import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SideDrawer } from './SideDrawer'

function renderDrawer({ pending = false, error = null as string | null } = {}) {
  const onClose = vi.fn()
  const onSubmit = vi.fn((event: { preventDefault: () => void }) => event.preventDefault())
  render(
    <SideDrawer title="Cadastrar regra de taxa" description="Uma regra por envio." formId="test-form" submitLabel="Cadastrar" pending={pending} error={error} onSubmit={onSubmit} onClose={onClose}>
      <label htmlFor="first">Primeiro</label>
      <input id="first" />
    </SideDrawer>,
  )
  return { onClose, onSubmit, user: userEvent.setup() }
}

describe('SideDrawer', () => {
  it('é um dialog nomeado pelo título, com descrição, Cancelar e o botão de envio', () => {
    renderDrawer()
    const dialog = screen.getByRole('dialog', { name: 'Cadastrar regra de taxa' })
    expect(dialog).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByRole('heading', { level: 2, name: 'Cadastrar regra de taxa' })).toBeInTheDocument()
    expect(screen.getByText('Uma regra por envio.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cadastrar' })).toBeEnabled()
  })

  it('Esc fecha a gaveta sem enviar (FCAD-12)', async () => {
    const { onClose, onSubmit, user } = renderDrawer()
    await user.click(screen.getByLabelText('Primeiro'))
    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('Cancelar fecha a gaveta sem enviar (FCAD-12)', async () => {
    const { onClose, onSubmit, user } = renderDrawer()
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('clique fora do painel fecha; clique dentro não', async () => {
    const { onClose, user } = renderDrawer()
    await user.click(screen.getByLabelText('Primeiro'))
    expect(onClose).not.toHaveBeenCalled()
    await user.click(screen.getByRole('dialog').parentElement!)
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('Tab fica preso dentro da gaveta', async () => {
    const { user } = renderDrawer()
    const close = screen.getByRole('button', { name: 'Fechar' })
    const submit = screen.getByRole('button', { name: 'Cadastrar' })
    submit.focus()
    await user.tab()
    expect(close).toHaveFocus()
    await user.tab({ shift: true })
    expect(submit).toHaveFocus()
  })

  it('o botão de envio dispara o formulário', async () => {
    const { onSubmit, user } = renderDrawer()
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))
    expect(onSubmit).toHaveBeenCalledTimes(1)
  })

  it('o botão de envio fica desabilitado em pending', async () => {
    const { onSubmit, user } = renderDrawer({ pending: true })
    const button = screen.getByRole('button', { name: 'Cadastrar' })
    expect(button).toBeDisabled()
    await user.click(button)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('mostra o erro com role="alert"', () => {
    renderDrawer({ error: 'Regra de taxa já existe' })
    expect(screen.getByRole('alert')).toHaveTextContent('Regra de taxa já existe')
  })
})

describe('SideDrawer com ação no erro (TXN-10)', () => {
  function renderWithAction(error: string | null) {
    render(
      <SideDrawer title="Nova transação" description="Venda avulsa." formId="action-form" submitLabel="Cadastrar" pending={false} error={error} errorAction={<a href="/taxas?cadastrar=1">Cadastrar regra de taxa</a>} onSubmit={vi.fn()} onClose={vi.fn()}>
        <input aria-label="Campo" />
      </SideDrawer>,
    )
  }

  it('renderiza errorAction dentro da caixa de erro', () => {
    renderWithAction('Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas')
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas')
    expect(within(alert).getByRole('link', { name: 'Cadastrar regra de taxa' })).toHaveAttribute('href', '/taxas?cadastrar=1')
  })

  it('sem erro, errorAction não aparece', () => {
    renderWithAction(null)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Cadastrar regra de taxa' })).not.toBeInTheDocument()
  })
})

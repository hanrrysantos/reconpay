import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError, type Run } from '@/lib/api'
import { PeriodMenu, windowKey } from './PeriodMenu'

const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }
const august = { fromDate: '2026-08-01', toDate: '2026-08-31' }

function run(window: { fromDate: string; toDate: string }, id: string): Run {
  return {
    id, merchantId: 'm-1', ...window, status: 'COMPLETED', totalItems: 10, matchedCount: 9, divergentCount: 1,
    createdAt: '2026-10-01T09:00:00Z', startedAt: null, finishedAt: null, errorMessage: null, supersededAt: null, processedItems: 10,
  }
}

function renderMenu(onApply: (window: { fromDate: string; toDate: string }) => void | Promise<void> = vi.fn()) {
  render(
    <PeriodMenu
      window={september}
      runs={[run(september, 'r-9'), run(august, 'r-8')]}
      locks={{ [windowKey(august)]: true }}
      today={new Date(2026, 9, 14)}
      settlementImportedAt="2026-10-05T12:00:00Z"
      onApply={onApply}
    />,
  )
  return { user: userEvent.setup(), onApply }
}

describe('PeriodMenu', () => {
  it('mostra a janela atual no botão', () => {
    renderMenu()
    expect(screen.getByRole('button', { name: /Setembro de 2026/ })).toBeInTheDocument()
  })

  it('lista o mês atual e os anteriores até o run mais antigo com o estado de cada um (DASH-02 AC3)', async () => {
    const { user } = renderMenu()
    await user.click(screen.getByRole('button', { name: /Setembro de 2026/ }))

    const months = within(screen.getByRole('list', { name: 'Meses' })).getAllByRole('button')
    expect(months.map(month => month.textContent)).toEqual([
      'Outubro de 2026Sem run',
      'Setembro de 2026Aberto',
      'Agosto de 2026',
    ])
    expect(within(months[2]).getByLabelText('Travado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Personalizado…' })).toBeInTheDocument()
  })

  it('escolher um mês aplica a janela do mês inteiro', async () => {
    const { user, onApply } = renderMenu()
    await user.click(screen.getByRole('button', { name: /Setembro de 2026/ }))
    await user.click(screen.getByRole('button', { name: /Agosto de 2026/ }))

    expect(onApply).toHaveBeenCalledWith(august)
  })

  it('intervalo personalizado aplica exatamente o fromDate e o toDate digitados (DASH-02 AC4)', async () => {
    const { user, onApply } = renderMenu()
    await user.click(screen.getByRole('button', { name: /Setembro de 2026/ }))
    await user.click(screen.getByRole('button', { name: 'Personalizado…' }))

    const from = screen.getByLabelText('De')
    const to = screen.getByLabelText('Até')
    await user.clear(from)
    await user.type(from, '2026-09-10')
    await user.clear(to)
    await user.type(to, '2026-09-20')
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    expect(onApply).toHaveBeenCalledWith({ fromDate: '2026-09-10', toDate: '2026-09-20' })
  })

  it('400 da API mantém o menu aberto e mostra a mensagem sob os campos (DASH-02 AC5)', async () => {
    const onApply = vi.fn().mockRejectedValue(new ApiError('A janela não pode passar de 366 dias.', 400, 'VALIDATION_ERROR'))
    const { user } = renderMenu(onApply)
    await user.click(screen.getByRole('button', { name: /Setembro de 2026/ }))
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('A janela não pode passar de 366 dias.')
    expect(screen.getByLabelText('De')).toBeInTheDocument()
    expect(screen.getByRole('button', { expanded: true })).toHaveTextContent('Setembro de 2026')
  })

  it('mostra a nota de atraso da liquidação com a data do último import', async () => {
    const { user } = renderMenu()
    await user.click(screen.getByRole('button', { name: /Setembro de 2026/ }))

    expect(screen.getByText(/Liquidações lidas até 05\/10\/2026/)).toBeInTheDocument()
    expect(screen.getByText(/A janela usa a data da venda/)).toBeInTheDocument()
  })
})

describe('PeriodMenu sem runs (TXN-02)', () => {
  const october = { fromDate: '2026-10-01', toDate: '2026-10-31' }
  const november2025 = { fromDate: '2025-11-01', toDate: '2025-11-30' }

  function renderPlain(onApply: (window: { fromDate: string; toDate: string }) => void | Promise<void> = vi.fn()) {
    render(<PeriodMenu window={october} months={[october, september, november2025]} today={new Date(2026, 9, 16)} onApply={onApply} />)
    return { user: userEvent.setup(), onApply }
  }

  it('abre o painel alinhado à direita do botão por padrão e à esquerda com align="start"', async () => {
    const { user } = renderPlain()
    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    expect(screen.getByRole('dialog', { name: 'Período' })).toHaveClass('right-0')
    expect(screen.getByRole('dialog', { name: 'Período' })).not.toHaveClass('left-0')
    cleanup()

    render(<PeriodMenu window={october} months={[october]} onApply={vi.fn()} align="start" />)
    await userEvent.setup().click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    expect(screen.getByRole('dialog', { name: 'Período' })).toHaveClass('left-0')
    expect(screen.getByRole('dialog', { name: 'Período' })).not.toHaveClass('right-0')
  })

  it('com months, lista exatamente esses meses na ordem recebida', async () => {
    const { user } = renderPlain()
    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))

    const months = within(screen.getByRole('list', { name: 'Meses' })).getAllByRole('button')
    expect(months.map(month => month.textContent)).toEqual(['Outubro de 2026', 'Setembro de 2026', 'Novembro de 2025'])
  })

  it('sem runs, nenhum mês mostra marca de run ou travamento e a nota da liquidação some', async () => {
    const { user } = renderPlain()
    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))

    const menu = screen.getByRole('dialog', { name: 'Período' })
    expect(within(menu).queryByText('Aberto')).not.toBeInTheDocument()
    expect(within(menu).queryByText('Sem run')).not.toBeInTheDocument()
    expect(within(menu).queryByLabelText('Travado')).not.toBeInTheDocument()
    expect(within(menu).queryByText(/Liquidações lidas até/)).not.toBeInTheDocument()
    expect(within(menu).queryByText(/O lado da liquidação/)).not.toBeInTheDocument()
  })

  it('escolher um mês da lista recebida aplica a janela desse mês', async () => {
    const { user, onApply } = renderPlain()
    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    await user.click(screen.getByRole('button', { name: 'Novembro de 2025' }))

    expect(onApply).toHaveBeenCalledWith(november2025)
  })

  it('onApply rejeitado continua mostrando a mensagem no menu', async () => {
    const onApply = vi.fn().mockRejectedValue(new Error('A data inicial deve ser igual ou anterior à final'))
    const { user } = renderPlain(onApply)
    await user.click(screen.getByRole('button', { name: /Outubro de 2026/ }))
    const from = screen.getByLabelText('De')
    await user.clear(from)
    await user.type(from, '2026-10-20')
    const to = screen.getByLabelText('Até')
    await user.clear(to)
    await user.type(to, '2026-10-10')
    await user.click(screen.getByRole('button', { name: 'Aplicar' }))

    expect(onApply).toHaveBeenCalledWith({ fromDate: '2026-10-20', toDate: '2026-10-10' })
    expect(await screen.findByRole('alert')).toHaveTextContent('A data inicial deve ser igual ou anterior à final')
    expect(screen.getByRole('dialog', { name: 'Período' })).toBeInTheDocument()
  })
})

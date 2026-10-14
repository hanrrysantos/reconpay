import { render, screen, within } from '@testing-library/react'
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

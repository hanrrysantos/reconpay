import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { DiscrepancyDetail } from '@/lib/api'
import { createQueryClient } from '@/lib/queries'
import { makeDetail, makeItem, september } from './fixtures'
import { ResolveDrawer, type DrawerEntry, type ResolveDrawerProps } from './ResolveDrawer'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', signOut: vi.fn() }) }))

const feeItem = makeItem({
  id: 'item-2', externalReference: 'PED-10022', paymentMethod: 'PIX', installments: 1, transactionDate: '2026-09-24',
  transactionAmount: '2400.00', settlementAmount: '2400.00', expectedNetAmount: '2287.13', settlementNetAmount: '1297.03',
  netDifference: '-990.10',
  settlementDate: '2026-09-25', settlementStatus: 'APPROVED',
  discrepancies: [{ id: 'd-2', status: 'OPEN', type: 'FEE_DIVERGENCE', expectedValue: '2287.13', actualValue: '1297.03' }],
})
const missingItem = makeItem()
const list: DrawerEntry[] = [
  { item: feeItem, discrepancyId: 'd-2' },
  { item: missingItem, discrepancyId: 'd-1' },
]

type Call = { url: string; method: string; body: unknown }
let calls: Call[]
let details: Record<string, DiscrepancyDetail>
let patchResponse: () => Response

beforeEach(() => {
  calls = []
  details = {
    'd-2': makeDetail({ id: 'd-2', type: 'FEE_DIVERGENCE', expectedValue: '2287.13', actualValue: '1297.03' }),
    'd-1': makeDetail(),
  }
  patchResponse = () => Response.json(details['d-2'])
  vi.stubGlobal('fetch', vi.fn(async (url: string, init: RequestInit = {}) => {
    const method = init.method ?? 'GET'
    calls.push({ url, method, body: init.body ? JSON.parse(String(init.body)) : undefined })
    const id = url.split('/discrepancies/')[1]
    if (method === 'PATCH') return patchResponse()
    return Response.json(details[id])
  }))
})
afterEach(() => vi.unstubAllGlobals())

function renderDrawer(overrides: Partial<ResolveDrawerProps> = {}) {
  const props: ResolveDrawerProps = {
    merchantId: 'm-1', window: september, runId: 'run-9', list, index: 0, total: 12, locked: false,
    onClose: vi.fn(), onNavigate: vi.fn(), ...overrides,
  }
  const client = createQueryClient(vi.fn())
  const view = render(<QueryClientProvider client={client}><ResolveDrawer {...props} /></QueryClientProvider>)
  return { user: userEvent.setup(), view, client, ...props }
}

const patches = () => calls.filter(call => call.method === 'PATCH')
const detailGets = () => calls.filter(call => call.method === 'GET' && call.url.includes('/discrepancies/'))

describe('ResolveDrawer', () => {
  it('mostra os dois lados do snapshot com o campo divergente destacado (DASH-04 AC3)', async () => {
    renderDrawer()
    const table = screen.getByRole('table', { name: 'Comparação' })
    const net = within(table).getByRole('row', { name: /Líquido/ })
    expect(within(net).getAllByRole('cell').map(cell => cell.textContent)).toEqual(['R$ 2.287,13', 'R$ 1.297,03'])
    expect(net).toHaveAttribute('data-divergent', 'true')
    expect(within(table).getByRole('row', { name: /Valor bruto/ })).not.toHaveAttribute('data-divergent')
    expect(screen.getByRole('heading', { name: 'PED-10022' })).toBeInTheDocument()
  })

  it('histórico do mais recente para o mais antigo (DASH-04 AC3)', async () => {
    details['d-2'] = makeDetail({
      id: 'd-2', type: 'FEE_DIVERGENCE', status: 'OPEN',
      adjustments: [{ amount: '-1000.00', voided: true }],
      transitions: [
        { actorUserId: 'u', fromStatus: 'OPEN', toStatus: 'ADJUSTED', note: 'primeiro', createdAt: '2026-10-01T10:00:00Z' },
        { actorUserId: 'u', fromStatus: 'ADJUSTED', toStatus: 'OPEN', note: 'reaberto', createdAt: '2026-10-02T10:00:00Z' },
      ],
    })
    renderDrawer()
    const history = await screen.findByRole('list', { name: 'Histórico' })
    await waitFor(() => expect(within(history).getAllByRole('listitem')).toHaveLength(3))
    const entries = within(history).getAllByRole('listitem').map(item => item.textContent)
    expect(entries[0]).toContain('Ajustada → Aberta')
    expect(entries[1]).toContain('Aberta → Ajustada')
    expect(entries[2]).toBe('lançamento de −1.000,00 anulado')
  })

  it.each([['Aceitar', 'ACCEPTED'], ['Baixar', 'WRITTEN_OFF']] as const)(
    '%s envia PATCH com %s, a nota e sem correctionAmount (DASH-04 AC4)', async (label, status) => {
      const { user } = renderDrawer()
      await user.click(screen.getByRole('radio', { name: new RegExp(`^${label}`) }))
      await user.type(screen.getByLabelText('Nota (opcional)'), 'conferido')
      await user.click(screen.getByRole('button', { name: new RegExp(`^${label}`) }))
      await waitFor(() => expect(patches()).toHaveLength(1))
      expect(patches()[0].url).toContain('/api/merchants/m-1/reconciliations/run-9/discrepancies/d-2')
      expect(patches()[0].body).toEqual({ status, note: 'conferido' })
    },
  )

  it('nota em branco é enviada como ausente (DASH-04 AC4)', async () => {
    const { user } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Aceitar/ }))
    await user.type(screen.getByLabelText('Nota (opcional)'), '   ')
    await user.click(screen.getByRole('button', { name: /^Aceitar/ }))
    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(patches()[0].body).toEqual({ status: 'ACCEPTED' })
  })

  it('Ajustar envia ADJUSTED com correctionAmount decimal com sinal (DASH-04 AC5)', async () => {
    const { user } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Ajustar/ }))
    await user.type(screen.getByLabelText('Valor da correção'), '−1.000,00')
    await user.click(screen.getByRole('button', { name: /Salvar ajuste/ }))
    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(patches()[0].body).toEqual({ status: 'ADJUSTED', correctionAmount: -1000.00 })
    const raw = vi.mocked(fetch).mock.calls.find(([, init]) => init?.method === 'PATCH')![1]!.body
    expect(raw).toBe('{"status":"ADJUSTED","correctionAmount":-1000.00}')
  })

  it('"Usar diferença" preenche a netDifference do servidor', async () => {
    const { user } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Ajustar/ }))
    await user.click(screen.getByRole('button', { name: 'Usar diferença' }))
    expect(screen.getByLabelText('Valor da correção')).toHaveValue('-990,10')
  })

  it('"Usar diferença" sem liquidação preenche −esperado', async () => {
    const { user } = renderDrawer({ index: 1 })
    await user.click(screen.getByRole('radio', { name: /^Ajustar/ }))
    await user.click(screen.getByRole('button', { name: 'Usar diferença' }))
    expect(screen.getByLabelText('Valor da correção')).toHaveValue('-4870,00')
  })

  it.each(['', '0,00'])('Ajustar com valor "%s" bloqueia o envio (DASH-04 AC6)', async value => {
    const { user } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Ajustar/ }))
    if (value) await user.type(screen.getByLabelText('Valor da correção'), value)
    await user.click(screen.getByRole('button', { name: /Salvar ajuste/ }))
    expect(await screen.findByText('Informe um valor diferente de zero')).toBeInTheDocument()
    expect(patches()).toHaveLength(0)
  })

  it('nota acima de 500 caracteres bloqueia o envio e pinta o contador (DASH-04 AC7)', async () => {
    const { user } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Aceitar/ }))
    const note = screen.getByLabelText('Nota (opcional)')
    await user.click(note)
    await user.paste('x'.repeat(501))
    expect(screen.getByTestId('note-counter')).toHaveTextContent('501/500')
    expect(screen.getByTestId('note-counter')).toHaveClass('text-danger')
    expect(screen.getByRole('button', { name: /^Aceitar/ })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: /^Aceitar/ }))
    expect(patches()).toHaveLength(0)
  })

  it('500 caracteres ainda podem ser enviados', async () => {
    const { user } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Aceitar/ }))
    await user.click(screen.getByLabelText('Nota (opcional)'))
    await user.paste('x'.repeat(500))
    expect(screen.getByTestId('note-counter')).not.toHaveClass('text-danger')
    await user.click(screen.getByRole('button', { name: /^Aceitar/ }))
    await waitFor(() => expect(patches()).toHaveLength(1))
  })

  it('"Reabrir" numa divergência resolvida envia PATCH com OPEN (DASH-04 AC8)', async () => {
    details['d-2'] = makeDetail({ id: 'd-2', type: 'FEE_DIVERGENCE', status: 'ADJUSTED' })
    const { user } = renderDrawer()
    await user.click(await screen.findByRole('button', { name: 'Reabrir' }))
    await waitFor(() => expect(patches()).toHaveLength(1))
    expect(patches()[0].body).toEqual({ status: 'OPEN' })
    expect(screen.queryByRole('radio')).not.toBeInTheDocument()
  })

  it('PATCH com sucesso recarrega a divergência e mostra o novo status (DASH-04 AC9)', async () => {
    const { user } = renderDrawer()
    await waitFor(() => expect(detailGets()).toHaveLength(1))
    await user.click(screen.getByRole('radio', { name: /^Aceitar/ }))
    details['d-2'] = makeDetail({ id: 'd-2', type: 'FEE_DIVERGENCE', status: 'ACCEPTED' })
    await user.click(screen.getByRole('button', { name: /^Aceitar/ }))
    await waitFor(() => expect(screen.getByTestId('drawer-status')).toHaveTextContent('Aceita'))
    expect(detailGets().length).toBeGreaterThanOrEqual(2)
    expect(screen.getByRole('button', { name: 'Reabrir' })).toBeInTheDocument()
  })

  it('PATCH 409 mostra a mensagem e recarrega a divergência (DASH-04 AC10)', async () => {
    patchResponse = () => Response.json({ error: 'CONFLICT', message: 'Status mudou' }, { status: 409 })
    const { user } = renderDrawer()
    await waitFor(() => expect(detailGets()).toHaveLength(1))
    details['d-2'] = makeDetail({ id: 'd-2', type: 'FEE_DIVERGENCE', status: 'WRITTEN_OFF' })
    await user.click(screen.getByRole('radio', { name: /^Aceitar/ }))
    await user.click(screen.getByRole('button', { name: /^Aceitar/ }))
    expect(await screen.findByText('Esta divergência mudou. Recarregamos o estado atual.')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByTestId('drawer-status')).toHaveTextContent('Baixada'))
    expect(detailGets()).toHaveLength(2)
  })

  it('período travado: sem formulário de desfecho nem "Reabrir" (DASH-04 AC11)', async () => {
    renderDrawer({ locked: true })
    expect(screen.getByText('Período travado · somente leitura')).toBeInTheDocument()
    expect(screen.queryByRole('radio')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Salvar|Reabrir/ })).not.toBeInTheDocument()
  })

  it('J vai para a próxima e K para a anterior da lista carregada (DASH-04 AC12)', async () => {
    const first = renderDrawer()
    await first.user.keyboard('j')
    expect(first.onNavigate).toHaveBeenCalledWith(1)
    await first.user.keyboard('k')
    expect(first.onNavigate).toHaveBeenCalledTimes(1)
    first.view.unmount()

    const second = renderDrawer({ index: 1 })
    await second.user.keyboard('k')
    expect(second.onNavigate).toHaveBeenCalledWith(0)
    await second.user.keyboard('j')
    expect(second.onNavigate).toHaveBeenCalledTimes(1)
  })

  it('J/K não navegam enquanto se digita na nota', async () => {
    const { user, onNavigate } = renderDrawer()
    await user.click(screen.getByRole('radio', { name: /^Aceitar/ }))
    await user.type(screen.getByLabelText('Nota (opcional)'), 'jk')
    expect(onNavigate).not.toHaveBeenCalled()
  })

  it('rodapé mostra a posição na lista de abertas', () => {
    renderDrawer({ index: 1 })
    expect(screen.getByText('2 de 12 abertas · J / K navega')).toBeInTheDocument()
  })

  it('com footerNoun nulo, o rodapé mostra só a posição e o total (DIV-07)', () => {
    renderDrawer({ index: 1, total: 3, footerNoun: null })
    expect(screen.getByText('2 de 3 · J / K navega')).toBeInTheDocument()
    expect(screen.queryByText(/abertas/)).toBeNull()
  })

  it('Esc fecha a gaveta', async () => {
    const { user, onClose } = renderDrawer()
    await user.keyboard('{Escape}')
    expect(onClose).toHaveBeenCalled()
  })

  it('foco fica preso na gaveta', async () => {
    const outside = document.body.appendChild(document.createElement('button'))
    outside.textContent = 'fora'
    const { user } = renderDrawer()
    const dialog = screen.getByRole('dialog', { name: 'Resolver divergência' })
    expect(dialog).toHaveFocus()
    for (let step = 0; step < 12; step++) {
      await user.tab()
      expect(dialog.contains(document.activeElement)).toBe(true)
    }
    await user.tab({ shift: true })
    expect(dialog.contains(document.activeElement)).toBe(true)
    expect(outside).not.toHaveFocus()
    outside.remove()
  })
})

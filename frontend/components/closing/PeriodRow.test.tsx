import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { Period } from '@/lib/api'
import type { Window } from '@/lib/period'
import { createQueryClient, queryKeys } from '@/lib/queries'
import { PeriodRow } from './PeriodRow'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({
  ...await importOriginal<typeof import('@/lib/api')>(),
  getPeriod: vi.fn(), lockPeriod: vi.fn(), unlockPeriod: vi.fn(), exportRunCsv: vi.fn(),
}))

const october: Window = { fromDate: '2026-10-01', toDate: '2026-10-31' }
const period = (fields: Partial<Period> = {}): Period => ({
  runId: 'r-oct', fromDate: '2026-10-01', toDate: '2026-10-31', totalItems: 4, matchedCount: 3, divergentCount: 1, matchRate: '0.9420',
  openAmount: '10.50', locked: false, lockedAt: null, closeDurationSeconds: null, ...fields,
})
const locked = (fields: Partial<Period> = {}) =>
  period({ locked: true, openAmount: '0.00', matchRate: '1.0000', lockedAt: '2026-11-02T08:07:30', closeDurationSeconds: 7500, ...fields })
const conflict = (message: string) => new api.ApiError(message, 409, 'CONFLICT')

/** A promise the test settles by hand, to hold a request in flight. */
function later<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(done => { resolve = done })
  return { promise, resolve }
}

let client: QueryClient
beforeEach(() => {
  client = createQueryClient(vi.fn())
  for (const fn of [api.getPeriod, api.lockPeriod, api.unlockPeriod, api.exportRunCsv]) vi.mocked(fn).mockReset()
})
afterEach(() => { client.clear(); vi.restoreAllMocks(); vi.unstubAllGlobals() })

function show(window: Window = october) {
  const onError = vi.fn()
  render(
    <QueryClientProvider client={client}>
      <table><tbody><PeriodRow merchantId="m-1" window={window} onError={onError} /></tbody></table>
    </QueryClientProvider>,
  )
  return { onError, user: userEvent.setup() }
}

// The period's own row; an open confirmation adds a second row below it.
const row = () => screen.getAllByRole('row')[0]
const cells = () => within(row()).getAllByRole('cell').slice(0, 6).map(cell => cell.textContent)
const buttons = () => within(row()).queryAllByRole('button').map(button => button.textContent)

describe('PeriodRow', () => {
  it('período aberto mostra as colunas, "Travar" e "Exportar CSV" (FEC-02 AC4, FEC-04 AC1, FEC-07 AC1)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    show()

    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', 'Aberto', '94,2%', 'R$ 10,50', '—', '—']))
    expect(api.getPeriod).toHaveBeenCalledWith('jwt', 'm-1', october)
    expect(buttons()).toEqual(['Travar', 'Exportar CSV'])
    expect(within(row()).getByRole('button', { name: 'Travar Outubro de 2026' })).toBeEnabled()
    expect(within(row()).getByRole('button', { name: 'Exportar CSV de Outubro de 2026' })).toBeEnabled()
  })

  it('período travado mostra data, duração, "Destravar" e "Exportar CSV" (FEC-02 AC4, FEC-04 AC1, FEC-07 AC1)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(locked())
    show()

    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', 'Travado', '100%', 'R$ 0,00', '02/11/2026 08:07', '2 h 5 min']))
    expect(buttons()).toEqual(['Destravar', 'Exportar CSV'])
  })

  it('taxa de match nula mostra "0%" (edge de matchRate nulo)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period({ matchRate: null }))
    show()
    await waitFor(() => expect(cells()[2]).toBe('0%'))
  })

  it('janela que não é um mês inteiro mostra as duas datas (edge de janela parcial)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period({ toDate: '2026-10-15' }))
    show({ fromDate: '2026-10-01', toDate: '2026-10-15' })
    await waitFor(() => expect(cells()[0]).toBe('01/10/2026 – 15/10/2026'))
  })

  it.each([
    [409, 'Janela possui conciliação em andamento', 'Em andamento'],
    [404, 'Período não encontrado', 'Sem conciliação concluída'],
  ])('leitura %i mostra "%s" com "—" nos valores e sem ações (FEC-02 AC5, AC6)', async (status, message, label) => {
    vi.mocked(api.getPeriod).mockRejectedValue(new api.ApiError(message, status))
    show()

    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', label, '—', '—', '—', '—']))
    expect(buttons()).toEqual([])
    expect(api.getPeriod).toHaveBeenCalledTimes(1)
  })

  it('outro erro mostra "Não carregou" e "Tentar novamente" relê a janela (FEC-03 AC5)', async () => {
    vi.mocked(api.getPeriod)
      .mockRejectedValueOnce(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
      .mockResolvedValueOnce(period())
    const { user } = show()

    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', 'Não carregou', '—', '—', '—', '—']))
    expect(buttons()).toEqual(['Tentar novamente'])
    await user.click(within(row()).getByRole('button', { name: 'Tentar novamente Outubro de 2026' }))

    await waitFor(() => expect(cells()[1]).toBe('Aberto'))
    expect(api.getPeriod).toHaveBeenCalledTimes(2)
    expect(vi.mocked(api.getPeriod).mock.calls[1]).toEqual(['jwt', 'm-1', october])
  })

  it('"Travar" pede confirmação e só trava depois de confirmar; a linha mostra a resposta (FEC-04 AC2, FEC-05 AC3)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    vi.mocked(api.lockPeriod).mockResolvedValue(locked({ closeDurationSeconds: 29 }))
    const { user, onError } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Travar Outubro de 2026' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Travar o período?' })
    expect(api.lockPeriod).not.toHaveBeenCalled()
    await user.click(within(dialog).getByRole('button', { name: 'Travar' }))

    expect(api.lockPeriod).toHaveBeenCalledWith('jwt', 'm-1', october)
    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', 'Travado', '100%', 'R$ 0,00', '02/11/2026 08:07', '29 s']))
    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(onError).toHaveBeenCalledWith(null)
    expect(onError).not.toHaveBeenCalledWith(expect.any(String))
  })

  it('a confirmação abre numa linha própria abaixo do período, alinhada à direita, com Cancelar vermelho e Travar preto', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Travar Outubro de 2026' }))

    const [periodRow, confirmRow] = screen.getAllByRole('row')
    const dialog = within(confirmRow).getByRole('alertdialog', { name: 'Travar o período?' })
    expect(within(periodRow).queryByRole('alertdialog')).toBeNull()
    expect(confirmRow.querySelector('td')).toHaveAttribute('colspan', '7')
    expect(dialog).not.toHaveClass('absolute')
    expect(dialog.parentElement).toHaveClass('justify-end')
    expect(within(dialog).getByRole('button', { name: 'Cancelar' })).toHaveClass('bg-danger')
    expect(within(dialog).getByRole('button', { name: 'Travar' })).toHaveClass('bg-foreground')
    expect(within(periodRow).getByRole('button', { name: 'Travar Outubro de 2026' })).toBeDisabled()
  })

  it('"Destravar" pede confirmação e só destrava depois de confirmar; a linha volta para "Aberto" (FEC-04 AC1, FEC-05 AC4)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(locked())
    vi.mocked(api.unlockPeriod).mockResolvedValue(period({ openAmount: '0.00', matchRate: '1.0000' }))
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Destravar Outubro de 2026' }))
    const dialog = screen.getByRole('alertdialog', { name: 'Destravar o período?' })
    expect(api.unlockPeriod).not.toHaveBeenCalled()
    await user.click(within(dialog).getByRole('button', { name: 'Destravar' }))

    expect(api.unlockPeriod).toHaveBeenCalledWith('jwt', 'm-1', october)
    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', 'Aberto', '100%', 'R$ 0,00', '—', '—']))
    expect(api.lockPeriod).not.toHaveBeenCalled()
  })

  it.each([
    ['Travar', 'Travar o período?', api.lockPeriod, period],
    ['Destravar', 'Destravar o período?', api.unlockPeriod, locked],
  ] as const)('"Cancelar" em "%s" fecha a confirmação sem chamar a API (FEC-04 AC5)', async (action, title, mutation, answer) => {
    vi.mocked(api.getPeriod).mockResolvedValue(answer())
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: `${action} Outubro de 2026` }))
    await user.click(within(screen.getByRole('alertdialog', { name: title })).getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(mutation).not.toHaveBeenCalled()
  })

  it('trava ou reabertura bem-sucedida grava a resposta e invalida a leitura do período no cache (FEC-05 AC6)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    const answer = locked()
    vi.mocked(api.lockPeriod).mockResolvedValue(answer)
    const invalidate = vi.spyOn(client, 'invalidateQueries')
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Travar Outubro de 2026' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))

    await waitFor(() => expect(client.getQueryData(queryKeys.period('m-1', october))).toEqual(answer))
    expect(invalidate).toHaveBeenCalledWith({ queryKey: queryKeys.period('m-1', october) })
  })

  it('trava recusada com 409 avisa a mensagem e relê o período da linha (FEC-06 AC1, AC2)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValueOnce(period()).mockResolvedValueOnce(period({ openAmount: '20.00' }))
    vi.mocked(api.lockPeriod).mockRejectedValue(conflict('Janela possui divergência em aberto'))
    const { user, onError } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Travar Outubro de 2026' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))

    await waitFor(() => expect(onError).toHaveBeenLastCalledWith('Janela possui divergência em aberto'))
    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', 'Aberto', '94,2%', 'R$ 20,00', '—', '—']))
    expect(api.getPeriod).toHaveBeenCalledTimes(2)
  })

  it('reabertura recusada com 409 avisa a mensagem e relê o período da linha (FEC-06 AC1, AC2)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValueOnce(locked()).mockResolvedValueOnce(period())
    vi.mocked(api.unlockPeriod).mockRejectedValue(conflict('Janela não está travada de 2026-10-01 a 2026-10-31'))
    const { user, onError } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Destravar Outubro de 2026' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Destravar' }))

    await waitFor(() => expect(onError).toHaveBeenLastCalledWith('Janela não está travada de 2026-10-01 a 2026-10-31'))
    await waitFor(() => expect(cells()[1]).toBe('Aberto'))
    expect(api.getPeriod).toHaveBeenCalledTimes(2)
  })

  it.each([
    [409, 'Em andamento', []],
    [404, 'Sem conciliação concluída', []],
    [403, 'Não carregou', ['Tentar novamente']],
  ])('trava recusada cuja releitura responde %i esconde os valores e as ações antigas (FEC-02 AC5, AC6, FEC-03 AC5, FEC-06 AC2)', async (status, label, actions) => {
    vi.mocked(api.getPeriod).mockResolvedValueOnce(period()).mockRejectedValue(new api.ApiError('Falha na releitura', status))
    vi.mocked(api.lockPeriod).mockRejectedValue(conflict('Janela possui conciliação em andamento'))
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Travar Outubro de 2026' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))

    await waitFor(() => expect(cells()).toEqual(['Outubro de 2026', label, '—', '—', '—', '—']))
    expect(buttons()).toEqual(actions)
  })

  it('trava com erro que não é 409 avisa a mensagem sem reler o período (FEC-06 AC1, AC2)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    vi.mocked(api.lockPeriod).mockRejectedValue(new api.ApiError('Você não tem acesso a este comerciante', 403, 'FORBIDDEN'))
    const { user, onError } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Travar Outubro de 2026' }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Travar' }))

    await waitFor(() => expect(onError).toHaveBeenLastCalledWith('Você não tem acesso a este comerciante'))
    expect(api.getPeriod).toHaveBeenCalledTimes(1)
    expect(cells()[1]).toBe('Aberto')
  })

  it('"Exportar CSV" baixa o CSV do run da linha como reconpay-<from>-<to>.csv (FEC-07 AC2)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(locked({ runId: 'r-9' }))
    vi.mocked(api.exportRunCsv).mockResolvedValue(new Blob(['externalReference;result\n']))
    const createObjectURL = vi.fn(() => 'blob:csv')
    vi.stubGlobal('URL', Object.assign(globalThis.URL, { createObjectURL, revokeObjectURL: vi.fn() }))
    const downloads: Array<{ name: string; href: string }> = []
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function (this: HTMLAnchorElement) { downloads.push({ name: this.download, href: this.href }) })
    const { user, onError } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Exportar CSV de Outubro de 2026' }))

    await waitFor(() => expect(downloads).toEqual([{ name: 'reconpay-2026-10-01-2026-10-31.csv', href: 'blob:csv' }]))
    expect(api.exportRunCsv).toHaveBeenCalledWith('jwt', 'm-1', 'r-9')
    expect(await (createObjectURL.mock.calls[0] as unknown as [Blob])[0].text()).toBe('externalReference;result\n')
    expect(onError).toHaveBeenCalledWith(null)
  })

  it('export com erro avisa a mensagem e não baixa nada (FEC-06 AC1)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    vi.mocked(api.exportRunCsv).mockRejectedValue(new api.ApiError('Conciliação não encontrada', 404, 'NOT_FOUND'))
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const { user, onError } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Exportar CSV de Outubro de 2026' }))

    await waitFor(() => expect(onError).toHaveBeenLastCalledWith('Conciliação não encontrada'))
    expect(click).not.toHaveBeenCalled()
  })

  it.each([
    ['trava', 'Travar', period, api.lockPeriod, locked],
    ['reabertura', 'Destravar', locked, api.unlockPeriod, period],
  ] as const)('com a %s em andamento, os botões da linha ficam desabilitados (FEC-06 AC3)', async (_case, action, start, mutation, answer) => {
    vi.mocked(api.getPeriod).mockResolvedValue(start())
    const pending = later<Period>()
    vi.mocked(mutation).mockReturnValue(pending.promise)
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: `${action} Outubro de 2026` }))
    await user.click(within(screen.getByRole('alertdialog')).getByRole('button', { name: action }))

    await waitFor(() => expect(within(row()).getByRole('button', { name: `${action} Outubro de 2026` })).toBeDisabled())
    expect(within(row()).getByRole('button', { name: 'Exportar CSV de Outubro de 2026' })).toBeDisabled()
    pending.resolve(answer())
    await waitFor(() => expect(within(row()).getByRole('button', { name: 'Exportar CSV de Outubro de 2026' })).toBeEnabled())
  })

  it('com o export em andamento, os botões da linha ficam desabilitados (FEC-06 AC3)', async () => {
    vi.mocked(api.getPeriod).mockResolvedValue(period())
    const pending = later<Blob>()
    vi.mocked(api.exportRunCsv).mockReturnValue(pending.promise)
    vi.stubGlobal('URL', Object.assign(globalThis.URL, { createObjectURL: vi.fn(() => 'blob:csv'), revokeObjectURL: vi.fn() }))
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const { user } = show()

    await user.click(await within(row()).findByRole('button', { name: 'Exportar CSV de Outubro de 2026' }))

    await waitFor(() => expect(within(row()).getByRole('button', { name: 'Exportar CSV de Outubro de 2026' })).toBeDisabled())
    expect(within(row()).getByRole('button', { name: 'Travar Outubro de 2026' })).toBeDisabled()
    pending.resolve(new Blob(['x']))
    await waitFor(() => expect(within(row()).getByRole('button', { name: 'Travar Outubro de 2026' })).toBeEnabled())
  })
})

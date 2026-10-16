import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClientProvider, type QueryClient } from '@tanstack/react-query'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '@/lib/api'
import type { ImportBatch } from '@/lib/api'
import { IMPORT_KINDS } from '@/lib/imports'
import { createQueryClient } from '@/lib/queries'
import { ImportPanel } from './ImportPanel'

vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: 'jwt', user: { id: 'u-1' }, signOut: vi.fn() }) }))
vi.mock('@/lib/api', async importOriginal => ({
  ...await importOriginal<typeof import('@/lib/api')>(),
  importCsv: vi.fn(), getImportBatches: vi.fn(), getLatestSettlementImport: vi.fn(), getLatestBankStatementImportTime: vi.fn(),
}))

const [transactions, settlements, bankStatements] = IMPORT_KINDS
const batch = (id: string, fileName: string, totalRows: number, createdAt: string): ImportBatch => ({ id, merchantId: 'm-1', fileName, totalRows, createdAt })
const old = batch('i-1', 'antigo.csv', 4, '2026-09-01T12:00:00Z')
const accepted = batch('i-9', 'transacoes-out.csv', 42, '2026-10-16T13:05:00Z')

const csv = (name = 'transacoes-out.csv', size?: number) => {
  const file = new File(['externalReference,amount\n'], name, { type: 'text/csv' })
  if (size !== undefined) Object.defineProperty(file, 'size', { value: size })
  return file
}

/** A promise the test settles by hand, to look at the panel while the upload is in flight. */
function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (error: unknown) => void
  const promise = new Promise<T>((res, rej) => { resolve = res; reject = rej })
  return { promise, resolve, reject }
}

let client: QueryClient
let stored: ImportBatch[]
beforeEach(() => {
  client = createQueryClient(vi.fn())
  stored = [old]
  vi.mocked(api.importCsv).mockReset()
  // Two pages: the first holds the stored batches, the second only the oldest one.
  vi.mocked(api.getImportBatches).mockReset().mockImplementation(async (_token, _merchantId, _kind, page) =>
    ({ content: page === 0 ? [...stored] : [old], totalPages: 2 }))
  vi.mocked(api.getLatestSettlementImport).mockResolvedValue(null)
  vi.mocked(api.getLatestBankStatementImportTime).mockResolvedValue(null)
})
afterEach(() => client.clear())

function show(info = transactions) {
  render(<QueryClientProvider client={client}><ImportPanel merchantId="m-1" info={info} /></QueryClientProvider>)
}

const importButton = () => screen.getByRole('button', { name: /^Importar|Importando…$/ })
const choose = (file: File) => userEvent.upload(screen.getByLabelText('Arquivo CSV'), file)

describe('ImportPanel', () => {
  it.each([
    [transactions, 'externalReference,amount,paymentMethod,installments,status,transactionDate'],
    [settlements, 'externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate'],
    [bankStatements, 'lineReference,externalReference,amount,movementDate'],
  ])('mostra as colunas esperadas de $label (IMP-02)', async (info, columns) => {
    show(info)
    expect(screen.getByText(columns)).toBeInTheDocument()
    await screen.findByRole('table', { name: 'Lotes importados' })
  })

  it('mantém Importar desabilitado até escolher um arquivo (IMP-03)', async () => {
    show()
    expect(importButton()).toBeDisabled()
    expect(importButton()).toHaveTextContent('Importar')
    await choose(csv())
    expect(importButton()).toBeEnabled()
  })

  it.each([
    [transactions, 'transactions'],
    [settlements, 'settlements'],
    [bankStatements, 'bankStatements'],
  ] as const)('envia o arquivo escolhido para o import de $label da empresa (IMP-03)', async (info, kind) => {
    vi.mocked(api.importCsv).mockResolvedValue(accepted)
    show(info)
    const file = csv()
    await choose(file)
    await userEvent.click(importButton())

    await screen.findByRole('status')
    expect(api.importCsv).toHaveBeenCalledTimes(1)
    expect(api.importCsv).toHaveBeenCalledWith('jwt', 'm-1', kind, file)
  })

  it('desabilita o botão com Importando… enquanto o envio não responde (IMP-03)', async () => {
    const pending = deferred<ImportBatch>()
    vi.mocked(api.importCsv).mockReturnValue(pending.promise)
    show()
    await choose(csv())
    await userEvent.click(importButton())

    expect(importButton()).toBeDisabled()
    expect(importButton()).toHaveTextContent('Importando…')
    await userEvent.click(importButton())
    expect(api.importCsv).toHaveBeenCalledTimes(1)

    pending.resolve(accepted)
    await screen.findByRole('status')
  })

  it('no sucesso confirma o lote, limpa o arquivo e volta a lista à primeira página com o lote no topo (IMP-04, IMP-12)', async () => {
    show()
    await userEvent.click(await screen.findByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(api.getImportBatches).toHaveBeenLastCalledWith('jwt', 'm-1', 'transactions', 1))
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()

    vi.mocked(api.importCsv).mockImplementation(async () => {
      stored = [accepted, old]
      return accepted
    })
    await choose(csv())
    await userEvent.click(importButton())

    expect(await screen.findByRole('status')).toHaveTextContent('transacoes-out.csv importado: 42 linha(s)')
    expect(importButton()).toBeDisabled()
    expect((screen.getByLabelText('Arquivo CSV') as HTMLInputElement).value).toBe('')
    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    const table = screen.getByRole('table', { name: 'Lotes importados' })
    expect(within(table).getAllByRole('row')[1]).toHaveTextContent('transacoes-out.csv')
  })

  it('recusa arquivo acima de 5 MB sem enviar (IMP-05)', async () => {
    show()
    await choose(csv('grande.csv', 5_242_881))
    await userEvent.click(importButton())

    expect(screen.getByRole('alert')).toHaveTextContent('Arquivo CSV excede o tamanho máximo permitido de 5MB')
    expect(api.importCsv).not.toHaveBeenCalled()
  })

  it('envia arquivo de exatamente 5 MB (IMP-05)', async () => {
    vi.mocked(api.importCsv).mockResolvedValue(accepted)
    show()
    const file = csv('limite.csv', 5_242_880)
    await choose(file)
    await userEvent.click(importButton())

    await screen.findByRole('status')
    expect(api.importCsv).toHaveBeenCalledWith('jwt', 'm-1', 'transactions', file)
  })

  it('na recusa mostra os erros por linha e mantém o arquivo para reenviar (IMP-06, IMP-09)', async () => {
    vi.mocked(api.importCsv).mockRejectedValue(new api.ApiError('Erro na importação do CSV', 400, 'VALIDATION_ERROR', {
      rowErrors: [{ row: 3, message: 'Regra de taxa ativa não encontrada para CREDIT_CARD em 3x' }],
    }))
    show()
    const file = csv()
    await choose(file)
    await userEvent.click(importButton())

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Erro na importação do CSV')
    expect(within(alert).getByRole('table', { name: 'Erros por linha' })).toHaveTextContent('Regra de taxa ativa não encontrada para CREDIT_CARD em 3x')
    expect(importButton()).toBeEnabled()

    await userEvent.click(importButton())
    await waitFor(() => expect(api.importCsv).toHaveBeenCalledTimes(2))
    expect(api.importCsv).toHaveBeenLastCalledWith('jwt', 'm-1', 'transactions', file)
  })

  it('na recusa por referências repetidas lista todas elas (IMP-07)', async () => {
    vi.mocked(api.importCsv).mockRejectedValue(new api.ApiError('Referência externa já importada: TX-1', 409, 'CONFLICT', {
      conflictingReferences: ['TX-1', 'TX-2'],
    }))
    show(settlements)
    await choose(csv('liquidacao.csv'))
    await userEvent.click(importButton())

    const list = await screen.findByRole('list', { name: 'Referências já importadas' })
    expect(within(list).getAllByRole('listitem').map(item => item.textContent)).toEqual(['TX-1', 'TX-2'])
  })

  it('na recusa por período travado mostra a mensagem da API (IMP-08)', async () => {
    vi.mocked(api.importCsv).mockRejectedValue(new api.ApiError('Janela travada de 2026-10-01 a 2026-10-31', 409, 'CONFLICT'))
    show(bankStatements)
    await choose(csv('extrato.csv'))
    await userEvent.click(importButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Janela travada de 2026-10-01 a 2026-10-31')
  })

  it('na falha de rede mostra a mensagem de conexão (IMP-08)', async () => {
    vi.mocked(api.importCsv).mockRejectedValue(new api.ApiError('Não foi possível conectar à API. Tente novamente.'))
    show()
    await choose(csv())
    await userEvent.click(importButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar à API. Tente novamente.')
  })

  it('um novo envio remove o resultado anterior antes da resposta (IMP-09)', async () => {
    vi.mocked(api.importCsv).mockRejectedValueOnce(new api.ApiError('Erro na importação do CSV', 400, 'VALIDATION_ERROR'))
    show()
    await choose(csv())
    await userEvent.click(importButton())
    await screen.findByRole('alert')

    const pending = deferred<ImportBatch>()
    vi.mocked(api.importCsv).mockReturnValueOnce(pending.promise)
    await userEvent.click(importButton())

    expect(importButton()).toHaveTextContent('Importando…')
    expect(screen.queryByRole('alert')).toBeNull()
    pending.resolve(accepted)
    await screen.findByRole('status')
  })
})

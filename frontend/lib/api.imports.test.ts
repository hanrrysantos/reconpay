import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, createMerchant, getImportBatches, importCsv, type ImportKind } from './api'

const merchantBase = 'http://localhost:8080/api/merchants/m-1'
const batch = { id: 'i-1', merchantId: 'm-1', fileName: 'extrato.csv', totalRows: 3, createdAt: '2026-10-16T13:05:00Z' }
const batchJson = JSON.stringify(batch)

afterEach(() => vi.unstubAllGlobals())

function stubFetch(body: string | null, init: ResponseInit = {}) {
  const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(body, init))
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

const call = (fetcher: ReturnType<typeof stubFetch>) => {
  const [url, init] = fetcher.mock.calls[0]
  const headers = new Headers(init?.headers)
  return { url, method: init?.method ?? 'GET', body: init?.body, contentType: headers.get('Content-Type'), auth: headers.get('Authorization') }
}

const paths: [ImportKind, string][] = [
  ['transactions', 'transactions'],
  ['settlements', 'external-settlements'],
  ['bankStatements', 'bank-statements'],
]

describe('importCsv', () => {
  it.each(paths)('envia o CSV de %s como multipart na parte file, sem layout (IMP-03)', async (kind, path) => {
    const fetcher = stubFetch(batchJson, { status: 201 })
    const file = new File(['lineReference,externalReference,amount,movementDate\n'], 'extrato.csv', { type: 'text/csv' })

    await expect(importCsv('jwt', 'm-1', kind, file)).resolves.toEqual(batch)

    const sent = call(fetcher)
    expect(sent.url).toBe(`${merchantBase}/${path}/import`)
    expect(sent.method).toBe('POST')
    expect(sent.auth).toBe('Bearer jwt')
    // The browser sets multipart/form-data with its boundary only when no Content-Type is forced.
    expect(sent.contentType).toBeNull()
    expect(sent.body).toBeInstanceOf(FormData)
    const form = sent.body as FormData
    expect([...form.keys()]).toEqual(['file'])
    expect(form.get('file')).toBeInstanceOf(File)
    expect((form.get('file') as File).name).toBe('extrato.csv')
  })

  it('mantém Content-Type JSON nos envios com corpo JSON', async () => {
    const fetcher = stubFetch('{"id":"m-9","name":"Alfa","document":"12345678000195","active":true,"createdAt":"2026-10-01T12:00:00Z"}', { status: 201 })
    await createMerchant('jwt', { name: 'Alfa', document: '12345678000195' })
    expect(call(fetcher).contentType).toBe('application/json')
  })
})

describe('getImportBatches', () => {
  it.each(paths)('lista os lotes de %s, 20 por página, do mais recente ao mais antigo (IMP-10)', async (kind, path) => {
    const fetcher = stubFetch(`{"content":[${batchJson}],"totalPages":3}`)

    const page = await getImportBatches('jwt', 'm-1', kind, 2)

    const sent = call(fetcher)
    expect(sent.url).toBe(`${merchantBase}/${path}/imports?page=2&size=20&sort=createdAt%2Cdesc`)
    expect(sent.method).toBe('GET')
    expect(sent.auth).toBe('Bearer jwt')
    expect(page).toEqual({ content: [batch], totalPages: 3 })
  })
})

describe('ApiError.details', () => {
  const failWith = async (body: string, status: number) => {
    stubFetch(body, { status })
    return importCsv('jwt', 'm-1', 'transactions', new File(['x'], 'a.csv')).catch((error: unknown) => error)
  }

  it('traz os erros por linha de um 400 (IMP-06)', async () => {
    const error = await failWith(JSON.stringify({
      status: 400, error: 'VALIDATION_ERROR', message: 'Erro na importação do CSV',
      details: { rowErrors: [{ row: 2, message: 'Referência externa é obrigatória' }, { row: 5, message: 'Valor inválido' }] },
    }), 400)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).message).toBe('Erro na importação do CSV')
    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).code).toBe('VALIDATION_ERROR')
    expect((error as ApiError).details).toEqual({
      rowErrors: [{ row: 2, message: 'Referência externa é obrigatória' }, { row: 5, message: 'Valor inválido' }],
    })
  })

  it('traz as referências em conflito de um 409 (IMP-07)', async () => {
    const error = await failWith(JSON.stringify({
      status: 409, error: 'CONFLICT', message: 'Referências externas já cadastradas para este comerciante: PED-1, PED-2',
      details: { conflictingReferences: ['PED-1', 'PED-2'] },
    }), 409)

    expect((error as ApiError).status).toBe(409)
    expect((error as ApiError).details).toEqual({ conflictingReferences: ['PED-1', 'PED-2'] })
  })

  it('fica sem details quando a resposta não traz details (IMP-08)', async () => {
    const error = await failWith(JSON.stringify({ status: 409, error: 'CONFLICT', message: 'Janela travada de 2026-10-01 a 2026-10-31' }), 409)

    expect((error as ApiError).message).toBe('Janela travada de 2026-10-01 a 2026-10-31')
    expect((error as ApiError).details).toBeUndefined()
  })

  it('fica sem details quando details não é um objeto', async () => {
    const error = await failWith(JSON.stringify({ status: 400, error: 'VALIDATION_ERROR', message: 'x', details: ['a'] }), 400)
    expect((error as ApiError).details).toBeUndefined()
  })

  it('fica sem details e com a mensagem padrão quando a resposta não é JSON (IMP-08)', async () => {
    const error = await failWith('<html>Bad Gateway</html>', 502)

    expect((error as ApiError).message).toBe('A API retornou erro 502.')
    expect((error as ApiError).details).toBeUndefined()
  })
})

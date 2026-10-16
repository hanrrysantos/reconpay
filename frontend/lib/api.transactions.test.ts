import { afterEach, describe, expect, it, vi } from 'vitest'
import { createTransaction, getTransactionPage, updateTransactionStatus } from './api'

const base = 'http://localhost:8080/api/merchants/m-1/transactions'
const october = { fromDate: '2026-10-01', toDate: '2026-10-31' }
const transaction = {
  id: 't-1', merchantId: 'm-1', externalReference: 'AVULSA-1', amount: '300.00', expectedNetAmount: '290.73',
  paymentMethod: 'CREDIT_CARD', installments: 3, status: 'APPROVED', transactionDate: '2026-10-03',
  createdAt: '2026-10-03T12:00:00Z', updatedAt: '2026-10-03T12:00:00Z',
}
const transactionJson = '{"id":"t-1","merchantId":"m-1","externalReference":"AVULSA-1","amount":300.00,"expectedNetAmount":290.73,'
  + '"paymentMethod":"CREDIT_CARD","installments":3,"status":"APPROVED","transactionDate":"2026-10-03",'
  + '"createdAt":"2026-10-03T12:00:00Z","updatedAt":"2026-10-03T12:00:00Z"}'

afterEach(() => vi.unstubAllGlobals())

function stubFetch(body: string | null, init: ResponseInit = {}) {
  const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(body, init))
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

const call = (fetcher: ReturnType<typeof stubFetch>) => {
  const [url, init] = fetcher.mock.calls[0]
  return { url, method: init?.method ?? 'GET', body: init?.body, auth: new Headers(init?.headers).get('Authorization') }
}

describe('getTransactionPage', () => {
  it('busca a janela, 20 por página, pela data da transação mais recente, sem status nem método (TXN-01 AC1)', async () => {
    const fetcher = stubFetch(`{"content":[${transactionJson}],"totalPages":2}`)

    const page = await getTransactionPage('jwt', 'm-1', { window: october }, 0)

    expect(call(fetcher)).toEqual({
      url: `${base}?fromDate=2026-10-01&toDate=2026-10-31&page=0&size=20&sort=transactionDate%2Cdesc`, method: 'GET', body: undefined, auth: 'Bearer jwt',
    })
    expect(page).toEqual({ content: [transaction], totalPages: 2 })
  })

  it('inclui status e paymentMethod quando informados, na página pedida (TXN-03)', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')

    await getTransactionPage('jwt', 'm-1', { window: october, status: 'CANCELLED', paymentMethod: 'PIX' }, 2)

    expect(call(fetcher).url).toBe(`${base}?fromDate=2026-10-01&toDate=2026-10-31&status=CANCELLED&paymentMethod=PIX&page=2&size=20&sort=transactionDate%2Cdesc`)
  })

  it('inclui só status quando só ele é informado', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getTransactionPage('jwt', 'm-1', { window: october, status: 'REFUNDED' }, 0)
    expect(call(fetcher).url).toBe(`${base}?fromDate=2026-10-01&toDate=2026-10-31&status=REFUNDED&page=0&size=20&sort=transactionDate%2Cdesc`)
  })

  it('inclui só paymentMethod quando só ele é informado', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getTransactionPage('jwt', 'm-1', { window: october, paymentMethod: 'BOLETO' }, 0)
    expect(call(fetcher).url).toBe(`${base}?fromDate=2026-10-01&toDate=2026-10-31&paymentMethod=BOLETO&page=0&size=20&sort=transactionDate%2Cdesc`)
  })

  it('mantém os valores monetários como texto, inclusive inteiros', async () => {
    stubFetch(`{"content":[${transactionJson.replace('"amount":300.00', '"amount":300').replace('"expectedNetAmount":290.73', '"expectedNetAmount":0')}],"totalPages":1}`)
    const page = await getTransactionPage('jwt', 'm-1', { window: october }, 0)
    expect(page.content[0].amount).toBe('300')
    expect(page.content[0].expectedNetAmount).toBe('0')
  })
})

describe('createTransaction', () => {
  const input = { externalReference: 'AVULSA-1', paymentMethod: 'CREDIT_CARD' as const, installments: 3, transactionDate: '2026-10-03' }

  it('faz POST com amount como número JSON igual ao texto e os demais campos (TXN-07 AC4)', async () => {
    const fetcher = stubFetch(transactionJson, { status: 201 })

    await expect(createTransaction('jwt', 'm-1', { ...input, amount: '1234.50' })).resolves.toEqual(transaction)

    expect(call(fetcher)).toEqual({
      url: base,
      method: 'POST',
      body: '{"externalReference":"AVULSA-1","amount":1234.50,"paymentMethod":"CREDIT_CARD","installments":3,"transactionDate":"2026-10-03"}',
      auth: 'Bearer jwt',
    })
  })

  it('preserva o zero à esquerda e as casas de um valor como 0.10', async () => {
    const fetcher = stubFetch(transactionJson, { status: 201 })
    await createTransaction('jwt', 'm-1', { ...input, amount: '0.10' })
    expect(call(fetcher).body).toBe('{"externalReference":"AVULSA-1","amount":0.10,"paymentMethod":"CREDIT_CARD","installments":3,"transactionDate":"2026-10-03"}')
  })
})

describe('updateTransactionStatus', () => {
  it('faz PATCH .../transactions/{id}/status com o novo status (TXN-09 AC4)', async () => {
    const fetcher = stubFetch(transactionJson.replace('"APPROVED"', '"REFUNDED"'))

    await expect(updateTransactionStatus('jwt', 'm-1', 't-1', 'REFUNDED')).resolves.toEqual({ ...transaction, status: 'REFUNDED' })

    expect(call(fetcher)).toEqual({ url: `${base}/t-1/status`, method: 'PATCH', body: '{"status":"REFUNDED"}', auth: 'Bearer jwt' })
  })
})

import { afterEach, describe, expect, it, vi } from 'vitest'
import { getRun, getRunItems, getRuns } from './api'

const base = 'http://localhost:8080/api/merchants/m-1/reconciliations'
const runJson = '{"id":"r-1","merchantId":"m-1","fromDate":"2026-10-01","toDate":"2026-10-31","status":"COMPLETED",'
  + '"totalItems":3,"matchedCount":2,"divergentCount":1,"createdAt":"2026-10-15T13:05:00Z","startedAt":null,'
  + '"finishedAt":null,"errorMessage":null,"supersededAt":null,"processedItems":3}'
const itemJson = '{"id":"i-1","reconciliationRunId":"r-1","externalReference":"TXN-1","result":"DIVERGENT",'
  + '"internalTransactionId":"t-1","externalSettlementId":null,"transactionAmount":150,"expectedNetAmount":145.00,'
  + '"settlementAmount":null,"settlementNetAmount":null,"netDifference":-145.00,"paymentMethod":"PIX","installments":1,'
  + '"transactionStatus":"APPROVED","settlementStatus":null,"transactionDate":"2026-10-03","settlementDate":null,'
  + '"discrepancies":[{"id":"d-1","status":"OPEN","type":"MISSING_SETTLEMENT","expectedValue":145.00,"actualValue":null}],'
  + '"createdAt":"2026-10-15T13:05:00Z"}'

afterEach(() => vi.unstubAllGlobals())

function stubFetch(body: string | null, init: ResponseInit = {}) {
  const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(body, init))
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

const call = (fetcher: ReturnType<typeof stubFetch>) => {
  const [url, init] = fetcher.mock.calls[0]
  return { url, method: init?.method ?? 'GET', auth: new Headers(init?.headers).get('Authorization') }
}

describe('getRun', () => {
  it('faz GET do run pelo id (REC-08 AC1)', async () => {
    const fetcher = stubFetch(runJson)

    const run = await getRun('jwt', 'm-1', 'r-1')

    expect(call(fetcher)).toEqual({ url: `${base}/r-1`, method: 'GET', auth: 'Bearer jwt' })
    expect(run).toMatchObject({ id: 'r-1', status: 'COMPLETED', totalItems: 3, matchedCount: 2, divergentCount: 1 })
  })

  it('rejeita com a mensagem e o status do 404 da API (REC-09 AC9)', async () => {
    stubFetch('{"status":404,"error":"NOT_FOUND","message":"Conciliação não encontrada com id: r-9"}', { status: 404 })

    await expect(getRun('jwt', 'm-1', 'r-9')).rejects.toMatchObject({
      status: 404, code: 'NOT_FOUND', message: 'Conciliação não encontrada com id: r-9',
    })
  })
})

describe('getRunItems', () => {
  it('busca a página pedida, 20 por página, pela maior diferença, sem filtros (REC-08 AC2)', async () => {
    const fetcher = stubFetch(`{"content":[${itemJson}],"totalPages":1}`)

    const page = await getRunItems('jwt', 'm-1', 'r-1', {}, 0)

    expect(call(fetcher)).toEqual({
      url: `${base}/r-1/items?page=0&size=20&sort=absDifference%2Cdesc`, method: 'GET', auth: 'Bearer jwt',
    })
    expect(page.totalPages).toBe(1)
    expect(page.content[0].externalReference).toBe('TXN-1')
  })

  it('inclui result e discrepancyType quando informados, nessa ordem (REC-09 AC4, AC5)', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')

    await getRunItems('jwt', 'm-1', 'r-1', { discrepancyType: 'FEE_DIVERGENCE', result: 'DIVERGENT' }, 2)

    expect(call(fetcher).url).toBe(`${base}/r-1/items?page=2&size=20&sort=absDifference%2Cdesc&result=DIVERGENT&discrepancyType=FEE_DIVERGENCE`)
  })

  it('inclui só result quando só ele é informado (REC-09 AC4)', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getRunItems('jwt', 'm-1', 'r-1', { result: 'MATCHED' }, 0)
    expect(call(fetcher).url).toBe(`${base}/r-1/items?page=0&size=20&sort=absDifference%2Cdesc&result=MATCHED`)
  })

  it('inclui só discrepancyType quando só ele é informado (REC-09 AC5)', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getRunItems('jwt', 'm-1', 'r-1', { discrepancyType: 'MISSING_SETTLEMENT' }, 0)
    expect(call(fetcher).url).toBe(`${base}/r-1/items?page=0&size=20&sort=absDifference%2Cdesc&discrepancyType=MISSING_SETTLEMENT`)
  })

  it.each(['OPEN', 'ACCEPTED', 'ADJUSTED', 'WRITTEN_OFF'] as const)('inclui discrepancyStatus=%s no fim, só quando informado (DIV-02)', async status => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getRunItems('jwt', 'm-1', 'r-1', { discrepancyStatus: status }, 0)
    expect(call(fetcher).url).toBe(`${base}/r-1/items?page=0&size=20&sort=absDifference%2Cdesc&discrepancyStatus=${status}`)
  })

  it('combina tipo e status nessa ordem, depois de result (DIV-02)', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getRunItems('jwt', 'm-1', 'r-1', { discrepancyStatus: 'ACCEPTED', discrepancyType: 'FEE_DIVERGENCE', result: 'DIVERGENT' }, 1)
    expect(call(fetcher).url)
      .toBe(`${base}/r-1/items?page=1&size=20&sort=absDifference%2Cdesc&result=DIVERGENT&discrepancyType=FEE_DIVERGENCE&discrepancyStatus=ACCEPTED`)
  })

  it('mantém os decimais dos itens como texto, inclusive inteiros (AD-010)', async () => {
    stubFetch(`{"content":[${itemJson}],"totalPages":1}`)

    const [item] = (await getRunItems('jwt', 'm-1', 'r-1', {}, 0)).content

    expect(item.transactionAmount).toBe('150')
    expect(item.expectedNetAmount).toBe('145.00')
    expect(item.netDifference).toBe('-145.00')
    expect(item.settlementNetAmount).toBeNull()
    expect(item.discrepancies[0].expectedValue).toBe('145.00')
  })
})

describe('getRuns para a lista da tela', () => {
  it('sem janela busca a página pedida, 20 por página, mais recente primeiro (REC-02 AC1)', async () => {
    const fetcher = stubFetch(`{"content":[${runJson}],"totalPages":1}`)

    await getRuns('jwt', 'm-1', { page: 3 })

    expect(call(fetcher).url).toBe(`${base}?page=3&size=20&sort=createdAt%2Cdesc`)
  })

  it('com janela envia fromDate e toDate exatos (REC-05 AC7)', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')

    await getRuns('jwt', 'm-1', { page: 0, window: { fromDate: '2026-09-01', toDate: '2026-09-30' } })

    expect(call(fetcher).url).toBe(`${base}?page=0&size=20&sort=createdAt%2Cdesc&fromDate=2026-09-01&toDate=2026-09-30`)
  })
})

import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError, exportRunCsv, getDiscrepancy, getLatestBankStatementImportTime, getLatestSettlementImport, getOpenDiscrepancies,
  getPeriod, getPeriodSummary, getRun, getRuns, lockPeriod, runReconciliation, unlockPeriod, updateDiscrepancy,
} from './api'

const base = 'http://localhost:8080/api/merchants/m-1'
const september = { fromDate: '2026-09-01', toDate: '2026-09-30' }

afterEach(() => vi.unstubAllGlobals())

function stubFetch(body: string, init: ResponseInit = {}) {
  const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(body, init))
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

const call = (fetcher: ReturnType<typeof stubFetch>) => {
  const [url, init] = fetcher.mock.calls[0]
  return { url, method: init?.method ?? 'GET', body: init?.body, auth: new Headers(init?.headers).get('Authorization') }
}

describe('runs', () => {
  it('lista os runs do merchant do mais recente para o mais antigo', async () => {
    const fetcher = stubFetch('{"content":[{"id":"r-1","status":"RUNNING","totalItems":40,"processedItems":12}],"totalPages":1}')
    const page = await getRuns('jwt', 'm-1', { size: 1 })
    expect(call(fetcher)).toEqual({ url: `${base}/reconciliations?page=0&size=1&sort=createdAt%2Cdesc`, method: 'GET', body: undefined, auth: 'Bearer jwt' })
    expect(page.content[0]).toEqual({ id: 'r-1', status: 'RUNNING', totalItems: 40, processedItems: 12 })
  })

  it('pode buscar o run mais antigo em ordem crescente', async () => {
    const fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getRuns('jwt', 'm-1', { size: 1, direction: 'asc' })
    expect(call(fetcher).url).toBe(`${base}/reconciliations?page=0&size=1&sort=createdAt%2Casc`)
  })

  it('busca um run pelo id', async () => {
    const fetcher = stubFetch('{"id":"r-1","status":"COMPLETED"}')
    await expect(getRun('jwt', 'm-1', 'r-1')).resolves.toEqual({ id: 'r-1', status: 'COMPLETED' })
    expect(call(fetcher)).toMatchObject({ url: `${base}/reconciliations/r-1`, method: 'GET', auth: 'Bearer jwt' })
  })

  it('dispara o run com a janela no corpo', async () => {
    const fetcher = stubFetch('{"id":"r-2","status":"PENDING"}', { status: 202 })
    await expect(runReconciliation('jwt', 'm-1', september)).resolves.toEqual({ id: 'r-2', status: 'PENDING' })
    expect(call(fetcher)).toEqual({ url: `${base}/reconciliations`, method: 'POST', body: '{"fromDate":"2026-09-01","toDate":"2026-09-30"}', auth: 'Bearer jwt' })
  })
})

describe('período', () => {
  it('lê o período da janela com decimais como texto, inclusive zero integral', async () => {
    const fetcher = stubFetch('{"runId":"r-1","totalItems":1284,"matchRate":0.9420,"openAmount":0,"locked":false}')
    const period = await getPeriod('jwt', 'm-1', september)
    expect(call(fetcher)).toMatchObject({ url: `${base}/periods?fromDate=2026-09-01&toDate=2026-09-30`, method: 'GET', auth: 'Bearer jwt' })
    expect(period).toEqual({ runId: 'r-1', totalItems: 1284, matchRate: '0.9420', openAmount: '0', locked: false })
  })

  it('trava e destrava a janela por POST', async () => {
    let fetcher = stubFetch('{"locked":true,"openAmount":0.00}')
    await expect(lockPeriod('jwt', 'm-1', september)).resolves.toEqual({ locked: true, openAmount: '0.00' })
    expect(call(fetcher)).toEqual({ url: `${base}/periods/lock`, method: 'POST', body: '{"fromDate":"2026-09-01","toDate":"2026-09-30"}', auth: 'Bearer jwt' })

    fetcher = stubFetch('{"locked":false}')
    await unlockPeriod('jwt', 'm-1', september)
    expect(call(fetcher)).toEqual({ url: `${base}/periods/unlock`, method: 'POST', body: '{"fromDate":"2026-09-01","toDate":"2026-09-30"}', auth: 'Bearer jwt' })
  })

  it('lê o resumo da janela sem arredondar valores nem contagens', async () => {
    const fetcher = stubFetch('{"runId":"r-1","grossAmount":12345678901234567.89,"feeAmount":1003.40,"bankReceivedAmount":null,'
      + '"openByType":{"MISSING_SETTLEMENT":3},"days":[{"date":"2026-09-01","expectedNetAmount":15060.00,"settledNetAmount":13582.00,'
      + '"bankReceivedAmount":null,"bankShortfallAmount":null,"itemCount":40,"matchedCount":38},{"date":"2026-09-02",'
      + '"expectedNetAmount":100.00,"settledNetAmount":100.00,"bankReceivedAmount":120.00,"bankShortfallAmount":-20.00,'
      + '"itemCount":1,"matchedCount":1}],"previousRunMatchRate":0.9240,"previousWindowGrossAmount":null,'
      + '"feePercent":3.00,"previousWindowGrossChangePercent":-6.3}')
    const summary = await getPeriodSummary('jwt', 'm-1', september)
    expect(call(fetcher)).toMatchObject({ url: `${base}/periods/summary?fromDate=2026-09-01&toDate=2026-09-30`, method: 'GET', auth: 'Bearer jwt' })
    expect(summary).toEqual({
      runId: 'r-1',
      grossAmount: '12345678901234567.89',
      feeAmount: '1003.40',
      bankReceivedAmount: null,
      openByType: { MISSING_SETTLEMENT: 3 },
      days: [
        { date: '2026-09-01', expectedNetAmount: '15060.00', settledNetAmount: '13582.00', bankReceivedAmount: null, bankShortfallAmount: null, itemCount: 40, matchedCount: 38 },
        { date: '2026-09-02', expectedNetAmount: '100.00', settledNetAmount: '100.00', bankReceivedAmount: '120.00', bankShortfallAmount: '-20.00', itemCount: 1, matchedCount: 1 },
      ],
      previousRunMatchRate: '0.9240',
      previousWindowGrossAmount: null,
      feePercent: '3.00',
      previousWindowGrossChangePercent: '-6.3',
    })
  })
})

describe('divergências', () => {
  it('pede as 4 abertas de maior diferença do run, com filtro opcional de tipo', async () => {
    let fetcher = stubFetch('{"content":[{"id":"i-1","expectedNetAmount":4870.00,"settlementNetAmount":null,"netDifference":-4870.00,"discrepancies":[]}],"totalPages":3,"totalElements":12}')
    const page = await getOpenDiscrepancies('jwt', 'm-1', 'r-1')
    expect(call(fetcher)).toMatchObject({ url: `${base}/reconciliations/r-1/items?discrepancyStatus=OPEN&sort=absDifference%2Cdesc&size=4`, method: 'GET', auth: 'Bearer jwt' })
    expect(page.content[0].expectedNetAmount).toBe('4870.00')
    expect(page.content[0].netDifference).toBe('-4870.00')
    expect(page.totalElements).toBe(12)

    fetcher = stubFetch('{"content":[],"totalPages":0}')
    await getOpenDiscrepancies('jwt', 'm-1', 'r-1', 'FEE_DIVERGENCE')
    expect(call(fetcher).url).toBe(`${base}/reconciliations/r-1/items?discrepancyStatus=OPEN&discrepancyType=FEE_DIVERGENCE&sort=absDifference%2Cdesc&size=4`)
  })

  it('lê o detalhe da divergência com ajustes como texto', async () => {
    const fetcher = stubFetch('{"id":"d-1","status":"OPEN","adjustments":[{"amount":-1000.00,"voided":true}],"transitions":[]}')
    const detail = await getDiscrepancy('jwt', 'm-1', 'r-1', 'd-1')
    expect(call(fetcher)).toMatchObject({ url: `${base}/reconciliations/r-1/discrepancies/d-1`, method: 'GET', auth: 'Bearer jwt' })
    expect(detail.adjustments).toEqual([{ amount: '-1000.00', voided: true }])
  })

  it('envia o ajuste com correctionAmount decimal exato e a nota', async () => {
    const fetcher = stubFetch('{"id":"d-1","status":"ADJUSTED"}')
    await updateDiscrepancy('jwt', 'm-1', 'r-1', 'd-1', { status: 'ADJUSTED', note: 'Tarifa estornada', correctionAmount: '-1000.00' })
    expect(call(fetcher)).toEqual({
      url: `${base}/reconciliations/r-1/discrepancies/d-1`,
      method: 'PATCH',
      body: '{"status":"ADJUSTED","note":"Tarifa estornada","correctionAmount":-1000.00}',
      auth: 'Bearer jwt',
    })
  })

  it('não arredonda correctionAmount acima de 13 dígitos', async () => {
    const fetcher = stubFetch('{}')
    await updateDiscrepancy('jwt', 'm-1', 'r-1', 'd-1', { status: 'ADJUSTED', correctionAmount: '12345678901234567.89' })
    expect(call(fetcher).body).toBe('{"status":"ADJUSTED","correctionAmount":12345678901234567.89}')
  })

  it('omite nota em branco e não envia correctionAmount em aceitar, baixar e reabrir', async () => {
    for (const status of ['ACCEPTED', 'WRITTEN_OFF', 'OPEN'] as const) {
      const fetcher = stubFetch('{}')
      await updateDiscrepancy('jwt', 'm-1', 'r-1', 'd-1', { status, note: '   ' })
      expect(call(fetcher)).toMatchObject({ method: 'PATCH', body: `{"status":"${status}"}` })
    }
  })
})

describe('importações e exportação', () => {
  it('lê o último import de liquidação ou null', async () => {
    let fetcher = stubFetch('{"content":[{"id":"imp-1","fileName":"set.csv","totalRows":10,"createdAt":"2026-10-05T09:42:00Z"}],"totalPages":4}')
    await expect(getLatestSettlementImport('jwt', 'm-1')).resolves.toEqual({ id: 'imp-1', fileName: 'set.csv', totalRows: 10, createdAt: '2026-10-05T09:42:00Z' })
    expect(call(fetcher)).toMatchObject({ url: `${base}/external-settlements/imports?size=1&sort=createdAt%2Cdesc`, auth: 'Bearer jwt' })

    fetcher = stubFetch('{"content":[],"totalPages":0}')
    await expect(getLatestSettlementImport('jwt', 'm-1')).resolves.toBeNull()
  })

  it('lê o horário do último import de extrato ou null', async () => {
    let fetcher = stubFetch('{"content":[{"id":"bi-1","merchantId":"m-1","fileName":"extrato.csv","totalRows":9,"createdAt":"2026-10-06T18:10:00Z"}],"totalPages":3}')
    await expect(getLatestBankStatementImportTime('jwt', 'm-1')).resolves.toBe('2026-10-06T18:10:00Z')
    expect(call(fetcher)).toMatchObject({ url: `${base}/bank-statements/imports?size=1&sort=createdAt%2Cdesc`, method: 'GET', auth: 'Bearer jwt' })

    fetcher = stubFetch('{"content":[],"totalPages":0}')
    await expect(getLatestBankStatementImportTime('jwt', 'm-1')).resolves.toBeNull()
  })

  it('baixa o CSV do run como blob com o token da sessão', async () => {
    const fetcher = stubFetch('externalReference;result\nPED-1;MATCHED\n', { headers: { 'Content-Type': 'text/csv' } })
    const blob = await exportRunCsv('jwt', 'm-1', 'r-1')
    expect(call(fetcher)).toMatchObject({ url: `${base}/reconciliations/r-1/export`, method: 'GET', auth: 'Bearer jwt' })
    expect(blob).toBeInstanceOf(Blob)
    expect(await blob.text()).toBe('externalReference;result\nPED-1;MATCHED\n')
  })
})

describe('erros', () => {
  it.each([
    [401, 'UNAUTHORIZED', 'Token expirado'],
    [403, 'FORBIDDEN', 'Acesso negado ao merchant'],
    [404, 'NOT_FOUND', 'Nenhum run concluído para a janela'],
    [409, 'CONFLICT', 'Esta divergência mudou'],
  ])('converte %i em ApiError com status, código e mensagem do StandardError', async (status, code, message) => {
    stubFetch(JSON.stringify({ status, error: code, message }), { status })
    const error = await getPeriod('jwt', 'm-1', september).catch(caught => caught)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status, code, message })
  })

  it('mantém o status quando o erro do PATCH ou do export vem sem JSON', async () => {
    stubFetch('', { status: 409 })
    await expect(updateDiscrepancy('jwt', 'm-1', 'r-1', 'd-1', { status: 'ACCEPTED' })).rejects.toMatchObject({ status: 409, code: undefined })
    stubFetch(JSON.stringify({ status: 404, error: 'NOT_FOUND', message: 'Run não encontrado' }), { status: 404 })
    await expect(exportRunCsv('jwt', 'm-1', 'r-1')).rejects.toMatchObject({ status: 404, code: 'NOT_FOUND', message: 'Run não encontrado' })
  })

  it('converte falha de rede em ApiError sem status', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    const error = await getPeriodSummary('jwt', 'm-1', september).catch(caught => caught)
    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBeUndefined()
    expect(error.message).toBe('Não foi possível conectar à API. Tente novamente.')
  })
})

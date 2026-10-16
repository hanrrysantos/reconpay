import { afterEach, describe, expect, it, vi } from 'vitest'
import { createFeeRule, deactivateFeeRule, getFeeRules, updateFeeRuleFees } from './api'

const base = 'http://localhost:8080/api/merchants/m-1/fee-rules'
const rule = {
  id: 'r-1', merchantId: 'm-1', merchantName: 'Loja Centro', paymentMethod: 'CREDIT_CARD', installments: 3,
  feePercentage: '2.9900', fixedFee: '0.30', active: true, createdAt: '2026-10-01T12:00:00Z',
}
const ruleJson = '{"id":"r-1","merchantId":"m-1","merchantName":"Loja Centro","paymentMethod":"CREDIT_CARD","installments":3,'
  + '"feePercentage":2.9900,"fixedFee":0.30,"active":true,"createdAt":"2026-10-01T12:00:00Z"}'

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

describe('fee rules', () => {
  it('lista uma página de até 100 regras por método e parcelas, com os decimais como texto (FLIST-02)', async () => {
    const fetcher = stubFetch(`{"content":[${ruleJson},${ruleJson.replace('"fixedFee":0.30', '"fixedFee":0').replace('"r-1"', '"r-2"')}],"totalPages":1}`)
    const rules = await getFeeRules('jwt', 'm-1')
    expect(call(fetcher)).toEqual({
      url: `${base}?page=0&size=100&sort=paymentMethod%2Casc&sort=installments%2Casc`, method: 'GET', body: undefined, auth: 'Bearer jwt',
    })
    expect(rules[0]).toEqual(rule)
    expect(rules[0].feePercentage).toBe('2.9900')
    expect(rules[1].fixedFee).toBe('0')
  })

  it('cadastra com os decimais como números JSON do texto digitado (FCAD-04)', async () => {
    const fetcher = stubFetch(ruleJson, { status: 201 })
    await expect(createFeeRule('jwt', 'm-1', { paymentMethod: 'CREDIT_CARD', installments: 3, feePercentage: '2.99', fixedFee: '0.30' }))
      .resolves.toEqual(rule)
    expect(call(fetcher)).toEqual({
      url: base, method: 'POST', body: '{"paymentMethod":"CREDIT_CARD","installments":3,"feePercentage":2.99,"fixedFee":0.30}', auth: 'Bearer jwt',
    })
  })

  it('altera mandando só percentual e taxa fixa (FEDIT-02)', async () => {
    const fetcher = stubFetch(ruleJson.replace('2.9900', '3.4900'))
    await expect(updateFeeRuleFees('jwt', 'm-1', 'r-1', { feePercentage: '3.49', fixedFee: '0.30' }))
      .resolves.toEqual({ ...rule, feePercentage: '3.4900' })
    expect(call(fetcher)).toEqual({ url: `${base}/r-1`, method: 'PUT', body: '{"feePercentage":3.49,"fixedFee":0.30}', auth: 'Bearer jwt' })
  })

  it('desativa com DELETE sem corpo (FDES-02)', async () => {
    const fetcher = stubFetch(null, { status: 204 })
    await expect(deactivateFeeRule('jwt', 'm-1', 'r-1')).resolves.toBeUndefined()
    expect(call(fetcher)).toEqual({ url: `${base}/r-1`, method: 'DELETE', body: undefined, auth: 'Bearer jwt' })
  })
})

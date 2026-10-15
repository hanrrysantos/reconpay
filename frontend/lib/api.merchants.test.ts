import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, createMerchant, deactivateMerchant, getMerchantPage, renameMerchant } from './api'

const base = 'http://localhost:8080/api/merchants'
const merchant = { id: 'm-1', name: 'Loja Centro', document: '12345678000195', active: true, createdAt: '2026-10-01T12:00:00Z' }

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

describe('merchants', () => {
  it('busca a página pedida, 20 por vez, em ordem de nome', async () => {
    const fetcher = stubFetch(JSON.stringify({ content: [merchant], totalPages: 3, totalElements: 41 }))
    await expect(getMerchantPage('jwt', 2)).resolves.toEqual({ content: [merchant], totalPages: 3, totalElements: 41 })
    expect(call(fetcher)).toEqual({ url: `${base}?page=2&size=20&sort=name%2Casc`, method: 'GET', body: undefined, auth: 'Bearer jwt' })
  })

  it('cadastra com nome e documento no corpo', async () => {
    const fetcher = stubFetch(JSON.stringify(merchant), { status: 201 })
    await expect(createMerchant('jwt', { name: 'Loja Centro', document: '12345678000195' })).resolves.toEqual(merchant)
    expect(call(fetcher)).toEqual({
      url: base, method: 'POST', body: JSON.stringify({ name: 'Loja Centro', document: '12345678000195' }), auth: 'Bearer jwt',
    })
  })

  it('renomeia mandando só o nome', async () => {
    const fetcher = stubFetch(JSON.stringify({ ...merchant, name: 'Loja Matriz' }))
    await expect(renameMerchant('jwt', 'm-1', 'Loja Matriz')).resolves.toEqual({ ...merchant, name: 'Loja Matriz' })
    expect(call(fetcher)).toEqual({ url: `${base}/m-1`, method: 'PUT', body: JSON.stringify({ name: 'Loja Matriz' }), auth: 'Bearer jwt' })
  })

  it('desativa com DELETE sem corpo', async () => {
    const fetcher = stubFetch(null, { status: 204 })
    await expect(deactivateMerchant('jwt', 'm-1')).resolves.toBeUndefined()
    expect(call(fetcher)).toEqual({ url: `${base}/m-1`, method: 'DELETE', body: undefined, auth: 'Bearer jwt' })
  })

  it('transforma o 409 em ApiError com a mensagem do StandardError', async () => {
    stubFetch(JSON.stringify({ status: 409, error: 'CONFLICT', message: 'Comerciante já cadastrado com documento: 12345678000195' }), { status: 409 })
    const error = await createMerchant('jwt', { name: 'Loja', document: '12345678000195' }).catch(caught => caught)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 409, code: 'CONFLICT', message: 'Comerciante já cadastrado com documento: 12345678000195' })
  })
})

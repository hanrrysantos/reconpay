import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  activateUser, ApiError, createUser, deactivateUser, getUserAccess, getUserPage, renameUser, replaceUserAccess, type User,
} from './api'

const base = 'http://localhost:8080/api/users'
const user: User = {
  id: 'u-1', name: 'Ana Operadora', email: 'ana@reconpay.local', role: 'OPERATOR', active: true,
  createdAt: '2026-10-01T12:00:00Z', demoExpiresAt: null,
}

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

describe('users', () => {
  it('busca a página com status, demo, 20 por vez, em ordem de nome', async () => {
    const fetcher = stubFetch(JSON.stringify({ content: [user], totalPages: 2, totalElements: 21 }))
    await expect(getUserPage('jwt', { status: 'ACTIVE', demo: false }, 1))
      .resolves.toEqual({ content: [user], totalPages: 2, totalElements: 21 })
    expect(call(fetcher)).toEqual({
      url: `${base}?status=ACTIVE&demo=false&page=1&size=20&sort=name%2Casc`, method: 'GET', body: undefined, auth: 'Bearer jwt',
    })
  })

  it('leva os outros filtros para a query', async () => {
    const fetcher = stubFetch(JSON.stringify({ content: [], totalPages: 0 }))
    await getUserPage('jwt', { status: 'INACTIVE', demo: true }, 0)
    expect(call(fetcher).url).toBe(`${base}?status=INACTIVE&demo=true&page=0&size=20&sort=name%2Casc`)
  })

  it('cria com nome, e-mail, senha e papel no corpo', async () => {
    const fetcher = stubFetch(JSON.stringify(user), { status: 201 })
    const input = { name: 'Ana Operadora', email: 'ana@reconpay.local', password: 'Operadora1', role: 'ADMIN' as const }
    await expect(createUser('jwt', input)).resolves.toEqual(user)
    expect(call(fetcher)).toEqual({ url: base, method: 'POST', body: JSON.stringify(input), auth: 'Bearer jwt' })
  })

  it('renomeia mandando só o nome', async () => {
    const fetcher = stubFetch(JSON.stringify({ ...user, name: 'Ana Souza' }))
    await expect(renameUser('jwt', 'u-1', 'Ana Souza')).resolves.toEqual({ ...user, name: 'Ana Souza' })
    expect(call(fetcher)).toEqual({ url: `${base}/u-1`, method: 'PUT', body: JSON.stringify({ name: 'Ana Souza' }), auth: 'Bearer jwt' })
  })

  it('desativa com DELETE sem corpo', async () => {
    const fetcher = stubFetch(null, { status: 204 })
    await expect(deactivateUser('jwt', 'u-1')).resolves.toBeUndefined()
    expect(call(fetcher)).toEqual({ url: `${base}/u-1`, method: 'DELETE', body: undefined, auth: 'Bearer jwt' })
  })

  it('reativa com PATCH em /activation', async () => {
    const fetcher = stubFetch(JSON.stringify(user))
    await expect(activateUser('jwt', 'u-1')).resolves.toEqual(user)
    expect(call(fetcher)).toEqual({ url: `${base}/u-1/activation`, method: 'PATCH', body: undefined, auth: 'Bearer jwt' })
  })

  it('lê os acessos e devolve os merchantIds', async () => {
    const fetcher = stubFetch(JSON.stringify({ userId: 'u-1', merchantIds: ['m-1', 'm-2'] }))
    await expect(getUserAccess('jwt', 'u-1')).resolves.toEqual(['m-1', 'm-2'])
    expect(call(fetcher)).toEqual({ url: `${base}/u-1/merchants`, method: 'GET', body: undefined, auth: 'Bearer jwt' })
  })

  it('substitui os acessos com exatamente a lista enviada, inclusive vazia', async () => {
    const fetcher = stubFetch(JSON.stringify({ userId: 'u-1', merchantIds: [] }))
    await expect(replaceUserAccess('jwt', 'u-1', [])).resolves.toEqual([])
    expect(call(fetcher)).toEqual({
      url: `${base}/u-1/merchants`, method: 'PUT', body: JSON.stringify({ merchantIds: [] }), auth: 'Bearer jwt',
    })
  })

  it('transforma o 409 em ApiError com a mensagem do StandardError', async () => {
    stubFetch(JSON.stringify({ status: 409, error: 'CONFLICT', message: 'Email já cadastrado: ana@reconpay.local' }), { status: 409 })
    const error = await createUser('jwt', { name: 'Ana', email: 'ana@reconpay.local', password: 'Operadora1', role: 'OPERATOR' })
      .catch(caught => caught)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 409, code: 'CONFLICT', message: 'Email já cadastrado: ana@reconpay.local' })
  })
})

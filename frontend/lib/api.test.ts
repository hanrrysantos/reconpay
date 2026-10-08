import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, authenticate, getMerchants, getTransactions, register } from './api'

afterEach(() => vi.unstubAllGlobals())

describe('API do ReconPay', () => {
  it('só autentica depois de carregar o perfil com o token recebido', async () => {
    const paths: string[] = []
    vi.stubGlobal('fetch', vi.fn(async (input: string, init?: RequestInit) => {
      paths.push(input)
      if (input.endsWith('/api/auth/login')) {
        expect(init?.method).toBe('POST')
        expect(init?.body).toBe(JSON.stringify({ email: 'ana@example.com', password: 'Senha123' }))
        return Response.json({ token: 'jwt', type: 'Bearer', expiresIn: 86400 })
      }
      expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer jwt')
      return Response.json({ id: 'user-1', name: 'Ana', email: 'ana@example.com', role: 'OPERATOR', active: true })
    }))

    await expect(authenticate('ana@example.com', 'Senha123')).resolves.toEqual({
      token: 'jwt',
      user: { id: 'user-1', name: 'Ana', email: 'ana@example.com', role: 'OPERATOR', active: true },
    })
    expect(paths).toEqual(['http://localhost:8080/api/auth/login', 'http://localhost:8080/api/me'])
  })

  it('rejeita o login quando o perfil não pode ser carregado', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(Response.json({ token: 'jwt', type: 'Bearer', expiresIn: 86400 }))
      .mockResolvedValueOnce(Response.json({ status: 401, error: 'UNAUTHORIZED', message: 'Token expirado' }, { status: 401 })))

    await expect(authenticate('ana@example.com', 'Senha123')).rejects.toMatchObject({ status: 401, message: 'Token expirado' })
  })

  it('lista todos os merchants acessíveis, mesmo quando a API pagina a resposta', async () => {
    const pages = [
      { content: [{ id: 'a', name: 'Loja A', document: '1' }], totalPages: 2, number: 0 },
      { content: [{ id: 'b', name: 'Loja B', document: '2' }], totalPages: 2, number: 1 },
    ]
    let nextPage = 0
    const fetcher = vi.fn(async (_input: string) => Response.json(pages[nextPage++]))
    vi.stubGlobal('fetch', fetcher)

    await expect(getMerchants('jwt')).resolves.toEqual([
      { id: 'a', name: 'Loja A', document: '1' },
      { id: 'b', name: 'Loja B', document: '2' },
    ])
    expect(fetcher.mock.calls.map(([url]) => url)).toEqual([
      'http://localhost:8080/api/me/merchants?page=0&size=100&sort=name%2Casc',
      'http://localhost:8080/api/me/merchants?page=1&size=100&sort=name%2Casc',
    ])
  })

  it('preserva centavos e números grandes das transações sem arredondamento', async () => {
    const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(
      '{"content":[{"id":"tx-1","merchantId":"merchant-1","externalReference":"Pedido 1","amount":9007199254740993.27,"expectedNetAmount":8999999999999999.12,"paymentMethod":"PIX","installments":1,"status":"APPROVED","transactionDate":"2026-10-08","createdAt":"2026-10-08T10:00:00Z","updatedAt":"2026-10-08T10:00:00Z"}],"totalPages":1,"number":0}',
      { headers: { 'Content-Type': 'application/json' } },
    ))
    vi.stubGlobal('fetch', fetcher)

    const transactions = await getTransactions('jwt', 'merchant-1')
    expect(transactions[0].amount).toBe('9007199254740993.27')
    expect(transactions[0].expectedNetAmount).toBe('8999999999999999.12')
    expect(fetcher.mock.calls[0][0]).toBe('http://localhost:8080/api/merchants/merchant-1/transactions?size=5&sort=transactionDate%2Cdesc')
    expect(new Headers(fetcher.mock.calls[0][1]?.headers).get('Authorization')).toBe('Bearer jwt')
  })

  it('mantém como texto um valor monetário integral enviado pela API', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response(
      '{"content":[{"id":"tx-2","merchantId":"merchant-1","externalReference":"Pedido 2","amount":100,"expectedNetAmount":98,"paymentMethod":"PIX","installments":1,"status":"APPROVED","transactionDate":"2026-10-08","createdAt":"2026-10-08T10:00:00Z","updatedAt":"2026-10-08T10:00:00Z"}],"totalPages":1}',
    )))

    const transactions = await getTransactions('jwt', 'merchant-1')
    expect(transactions[0].amount).toBe('100')
    expect(transactions[0].expectedNetAmount).toBe('98')
  })

  it('usa a mensagem do StandardError e mantém o código HTTP', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ status: 409, error: 'CONFLICT', message: 'Email já cadastrado' }, { status: 409 })))

    await expect(register({ name: 'Ana', email: 'ana@example.com', password: 'Senha123' }))
      .rejects.toEqual(new ApiError('Email já cadastrado', 409))
  })

  it('informa quando a API está indisponível', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))

    await expect(getMerchants('jwt')).rejects.toThrow('Não foi possível conectar à API')
  })
})

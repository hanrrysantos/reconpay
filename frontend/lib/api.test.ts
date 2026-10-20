import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, authenticate, createDemo, getMerchants, getTransactions, register, resendVerification, verifyEmail } from './api'

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

  it('abre a demo com POST sem corpo e carrega o perfil com o token recebido (DEMO-07 AC2; DEMO-09)', async () => {
    const fetcher = vi.fn(async (input: string, init?: RequestInit) => input.endsWith('/api/auth/demo')
      ? Response.json({ token: 'demo-jwt', type: 'Bearer', expiresIn: 86400 }, { status: 201 })
      : Response.json({ id: 'demo-1', name: 'Visitante', email: 'demo-abc@demo.reconpay.invalid', role: 'OPERATOR', active: true, demoExpiresAt: '2099-10-21T12:34:56Z' }))
    vi.stubGlobal('fetch', fetcher)

    await expect(createDemo()).resolves.toEqual({
      token: 'demo-jwt',
      user: { id: 'demo-1', name: 'Visitante', email: 'demo-abc@demo.reconpay.invalid', role: 'OPERATOR', active: true, demoExpiresAt: '2099-10-21T12:34:56Z' },
    })
    expect(fetcher.mock.calls.map(([url]) => url)).toEqual(['http://localhost:8080/api/auth/demo', 'http://localhost:8080/api/me'])
    const [, demoInit] = fetcher.mock.calls[0]
    expect(demoInit?.method).toBe('POST')
    expect(demoInit?.body).toBeUndefined()
    expect(new Headers(demoInit?.headers).get('Authorization')).toBeNull()
    expect(new Headers(fetcher.mock.calls[1][1]?.headers).get('Authorization')).toBe('Bearer demo-jwt')
  })

  it('recusa a demo com o status e a mensagem do 429 sem carregar o perfil (DEMO-07 AC5)', async () => {
    const fetcher = vi.fn(async () => Response.json({
      timestamp: '2026-10-20T12:00:00Z', status: 429, error: 'TOO_MANY_REQUESTS',
      message: 'Limite de demonstrações atingido. Tente novamente em até 1 hora.', path: '/api/auth/demo',
    }, { status: 429 }))
    vi.stubGlobal('fetch', fetcher)

    const error = await createDemo().catch((caught: unknown) => caught)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 429, code: 'TOO_MANY_REQUESTS', message: 'Limite de demonstrações atingido. Tente novamente em até 1 hora.' })
    expect(fetcher).toHaveBeenCalledTimes(1)
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

  it('confirma o e-mail com o token pelo POST da API', async () => {
    const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetcher)

    await expect(verifyEmail(' q7Xk2mV9 ')).resolves.toBeUndefined()
    expect(fetcher.mock.calls[0][0]).toBe('http://localhost:8080/api/auth/verify-email')
    expect(fetcher.mock.calls[0][1]?.method).toBe('POST')
    expect(JSON.parse(String(fetcher.mock.calls[0][1]?.body))).toEqual({ token: ' q7Xk2mV9 ' })
  })

  it('recusa a confirmação com a mensagem e o código da API', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ status: 400, error: 'VALIDATION_ERROR', message: 'Token de verificação inválido ou expirado' }, { status: 400 })))

    await expect(verifyEmail('velho')).rejects.toEqual(new ApiError('Token de verificação inválido ou expirado', 400))
  })

  it('pede um link novo com o e-mail sem espaços nas pontas', async () => {
    const fetcher = vi.fn(async (_input: string, _init?: RequestInit) => new Response(null, { status: 202 }))
    vi.stubGlobal('fetch', fetcher)

    await expect(resendVerification('  ana@lojacentro.com.br  ')).resolves.toBeUndefined()
    expect(fetcher.mock.calls[0][0]).toBe('http://localhost:8080/api/auth/resend-verification')
    expect(fetcher.mock.calls[0][1]?.method).toBe('POST')
    expect(JSON.parse(String(fetcher.mock.calls[0][1]?.body))).toEqual({ email: 'ana@lojacentro.com.br' })
  })
})

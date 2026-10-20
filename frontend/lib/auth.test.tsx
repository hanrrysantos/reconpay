import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './auth'

const KEY = 'reconpay.demoToken'
const demoUser = { id: 'demo-1', name: 'Visitante', email: 'demo-abc@demo.reconpay.invalid', role: 'OPERATOR', active: true, demoExpiresAt: '2099-10-21T12:34:56Z' }
const regularUser = { id: 'u-1', name: 'Ana', email: 'ana@example.com', role: 'OPERATOR', active: true, demoExpiresAt: null }

function Probe() {
  const { token, user, restoring, expired, signIn, signOut, startDemo } = useAuth()
  return (
    <>
      <p data-testid="token">{token ?? 'sem token'}</p>
      <p data-testid="user">{user?.name ?? 'sem usuário'}</p>
      <p data-testid="restoring">{restoring ? 'restaurando' : 'pronto'}</p>
      <p data-testid="expired">{expired ? 'expirada' : 'válida'}</p>
      <button onClick={() => signIn('ana@example.com', 'Senha123')}>entrar</button>
      <button onClick={() => startDemo()}>demo</button>
      <button onClick={signOut}>sair</button>
    </>
  )
}

const text = (id: string) => screen.getByTestId(id).textContent

function stubApi(me: (token: string | null) => Response = token => Response.json(token === 'demo-jwt' ? demoUser : regularUser)) {
  const fetcher = vi.fn(async (input: string, init?: RequestInit) => {
    if (input.endsWith('/api/auth/demo')) return Response.json({ token: 'demo-jwt', type: 'Bearer', expiresIn: 86400 }, { status: 201 })
    if (input.endsWith('/api/auth/login')) return Response.json({ token: 'jwt', type: 'Bearer', expiresIn: 86400 })
    return me(new Headers(init?.headers).get('Authorization')?.replace('Bearer ', '') ?? null)
  })
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

beforeEach(() => {
  sessionStorage.clear()
  localStorage.clear()
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('AuthProvider: sessão da demo', () => {
  it('startDemo abre a sessão e grava o token da demo em sessionStorage (DEMO-08 AC1)', async () => {
    stubApi()
    render(<AuthProvider><Probe /></AuthProvider>)
    await waitFor(() => expect(text('restoring')).toBe('pronto'))

    await userEvent.click(screen.getByRole('button', { name: 'demo' }))

    await waitFor(() => expect(text('token')).toBe('demo-jwt'))
    expect(text('user')).toBe('Visitante')
    expect(sessionStorage.getItem(KEY)).toBe('demo-jwt')
  })

  it('o login comum não grava o token em sessionStorage nem em localStorage (DEMO-08 AC3)', async () => {
    stubApi()
    render(<AuthProvider><Probe /></AuthProvider>)
    await waitFor(() => expect(text('restoring')).toBe('pronto'))

    await userEvent.click(screen.getByRole('button', { name: 'entrar' }))

    await waitFor(() => expect(text('token')).toBe('jwt'))
    expect(sessionStorage.length).toBe(0)
    expect(localStorage.length).toBe(0)
  })

  it('ao montar com token salvo, restaura token e usuário pelo /api/me (DEMO-08 AC2)', async () => {
    sessionStorage.setItem(KEY, 'demo-jwt')
    const fetcher = stubApi()
    render(<AuthProvider><Probe /></AuthProvider>)

    expect(text('restoring')).toBe('restaurando')
    expect(text('token')).toBe('sem token')
    await waitFor(() => expect(text('token')).toBe('demo-jwt'))
    expect(text('user')).toBe('Visitante')
    expect(text('restoring')).toBe('pronto')
    expect(text('expired')).toBe('válida')
    expect(fetcher.mock.calls.map(([url]) => url)).toEqual(expect.arrayContaining(['http://localhost:8080/api/me']))
    expect(sessionStorage.getItem(KEY)).toBe('demo-jwt')
  })

  it('sem token salvo, termina a restauração sem sessão e sem chamar a API', async () => {
    const fetcher = stubApi()
    render(<AuthProvider><Probe /></AuthProvider>)

    await waitFor(() => expect(text('restoring')).toBe('pronto'))
    expect(text('token')).toBe('sem token')
    expect(text('expired')).toBe('válida')
    expect(fetcher).not.toHaveBeenCalled()
  })

  it('restauração recusada com 401 apaga o token e marca a demo como expirada (DEMO-08 AC5)', async () => {
    sessionStorage.setItem(KEY, 'demo-jwt')
    stubApi(() => Response.json({ status: 401, error: 'UNAUTHORIZED', message: 'Token expirado' }, { status: 401 }))
    render(<AuthProvider><Probe /></AuthProvider>)

    await waitFor(() => expect(text('restoring')).toBe('pronto'))
    expect(text('expired')).toBe('expirada')
    expect(text('token')).toBe('sem token')
    expect(sessionStorage.getItem(KEY)).toBeNull()
  })

  it('signOut apaga o token da demo e encerra a sessão (DEMO-08 AC4)', async () => {
    sessionStorage.setItem(KEY, 'demo-jwt')
    stubApi()
    render(<AuthProvider><Probe /></AuthProvider>)
    await waitFor(() => expect(text('token')).toBe('demo-jwt'))

    await userEvent.click(screen.getByRole('button', { name: 'sair' }))

    expect(text('token')).toBe('sem token')
    expect(sessionStorage.getItem(KEY)).toBeNull()
  })

  it('sessionStorage que lança não quebra o provider nem a abertura da demo', async () => {
    for (const method of ['getItem', 'setItem', 'removeItem'] as const) {
      vi.spyOn(Storage.prototype, method).mockImplementation(() => { throw new DOMException('bloqueado', 'SecurityError') })
    }
    stubApi()
    render(<AuthProvider><Probe /></AuthProvider>)

    await waitFor(() => expect(text('restoring')).toBe('pronto'))
    await userEvent.click(screen.getByRole('button', { name: 'demo' }))
    await waitFor(() => expect(text('token')).toBe('demo-jwt'))
    await userEvent.click(screen.getByRole('button', { name: 'sair' }))
    expect(text('token')).toBe('sem token')
  })
})

import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useQuery } from '@tanstack/react-query'
import { afterEach, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { Providers } from './providers'

afterEach(() => vi.unstubAllGlobals())

function Probe({ failWith }: { failWith: ApiError }) {
  const { token, signIn } = useAuth()
  useQuery({ queryKey: ['probe', token], queryFn: () => Promise.reject(failWith), enabled: Boolean(token) })
  return (
    <>
      <p>{token ? `sessão ${token}` : 'sem sessão'}</p>
      <button onClick={() => signIn('ana@example.com', 'Senha123')}>entrar</button>
    </>
  )
}

function stubLogin() {
  vi.stubGlobal('fetch', vi.fn(async (input: string) => input.endsWith('/api/auth/login')
    ? Response.json({ token: 'jwt' })
    : Response.json({ id: 'u-1', name: 'Ana', email: 'ana@example.com', role: 'OPERATOR', active: true })))
}

it('encerra a sessão quando uma query responde 401', async () => {
  stubLogin()
  render(<Providers><Probe failWith={new ApiError('Token expirado', 401, 'UNAUTHORIZED')} /></Providers>)

  await userEvent.click(screen.getByRole('button', { name: 'entrar' }))

  await waitFor(() => expect(screen.getByText('sem sessão')).toBeInTheDocument())
})

it('mantém a sessão quando a query responde 403', async () => {
  stubLogin()
  render(<Providers><Probe failWith={new ApiError('Sem acesso', 403, 'FORBIDDEN')} /></Providers>)

  await userEvent.click(screen.getByRole('button', { name: 'entrar' }))

  await waitFor(() => expect(screen.getByText('sessão jwt')).toBeInTheDocument())
  await new Promise(resolve => setTimeout(resolve, 50))
  expect(screen.getByText('sessão jwt')).toBeInTheDocument()
})

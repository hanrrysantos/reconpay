import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '@/lib/auth'
import LoginPage from './page'

const navigation = vi.hoisted(() => ({ replace: vi.fn() }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: navigation.replace }) }))

const demoUser = { id: 'demo-1', name: 'Visitante', email: 'demo-abc@demo.reconpay.invalid', role: 'OPERATOR', active: true, demoExpiresAt: '2099-10-21T12:34:56Z' }
const ana = { id: 'u-1', name: 'Ana Souza', email: 'ana@lojacentro.com.br', role: 'OPERATOR', active: true, demoExpiresAt: null }

function stubApi() {
  vi.stubGlobal('fetch', vi.fn(async (input: string, init?: RequestInit) => {
    if (input.endsWith('/api/auth/login')) return Response.json({ token: 'ana-jwt', type: 'Bearer', expiresIn: 3600 })
    const bearer = new Headers(init?.headers).get('Authorization')
    return Response.json(bearer === 'Bearer demo-jwt' ? demoUser : ana)
  }))
}

beforeEach(() => {
  navigation.replace.mockReset()
  sessionStorage.clear()
  stubApi()
})
afterEach(() => vi.unstubAllGlobals())

describe('LoginPage com a sessão real', () => {
  it('com a demo aberta na aba mostra o aviso; "Sair e entrar com outra conta" apaga o token e mostra o formulário (SESS-02 AC1, AC2)', async () => {
    sessionStorage.setItem('reconpay.demoToken', 'demo-jwt')
    render(<AuthProvider><LoginPage /></AuthProvider>)

    expect(await screen.findByText('Você já está em uma sessão')).toBeInTheDocument()
    expect(screen.getByRole('main')).toHaveTextContent('Conectado como demo-abc@demo.reconpay.invalid.')

    await userEvent.click(screen.getByRole('button', { name: 'Sair e entrar com outra conta' }))

    expect(sessionStorage.getItem('reconpay.demoToken')).toBeNull()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Entrar')
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument()
    expect(navigation.replace).not.toHaveBeenCalled()
  })

  it('um login enviado com sucesso vai para /painel sem mostrar o aviso (SESS-02 AC3)', async () => {
    render(<AuthProvider><LoginPage /></AuthProvider>)
    const user = userEvent.setup()

    await user.type(await screen.findByLabelText('E-mail'), 'ana@lojacentro.com.br')
    await user.type(screen.getByLabelText('Senha'), 'Senha@2026')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(navigation.replace).toHaveBeenCalledWith('/painel'))
    expect(screen.queryByText('Você já está em uma sessão')).not.toBeInTheDocument()
  })
})

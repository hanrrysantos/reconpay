import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '@/lib/auth'
import { DemoButton } from './DemoButton'

const navigation = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: navigation.push }) }))

const demoUser = { id: 'demo-1', name: 'Visitante', email: 'demo-abc@demo.reconpay.invalid', role: 'OPERATOR', active: true, demoExpiresAt: '2099-10-21T12:34:56Z' }
const GENERIC = 'Não foi possível abrir a demonstração. Tente novamente.'

function stubDemo(demo: () => Promise<Response>) {
  const fetcher = vi.fn(async (input: string, _init?: RequestInit) => input.endsWith('/api/auth/demo') ? demo() : Response.json(demoUser))
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

const demoCalls = (fetcher: ReturnType<typeof stubDemo>) => fetcher.mock.calls.filter(([url]) => url.endsWith('/api/auth/demo'))

function renderButton() {
  render(<AuthProvider><DemoButton className="cta" /></AuthProvider>)
  return screen.getByRole('button', { name: 'Ver demonstração' })
}

beforeEach(() => {
  navigation.push.mockReset()
  sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('DemoButton', () => {
  it('cliques repetidos chamam POST /api/auth/demo uma vez; durante a chamada fica desabilitado com "Preparando demonstração..." (DEMO-07 AC2, AC3)', async () => {
    let respond: (response: Response) => void = () => {}
    const fetcher = stubDemo(() => new Promise(resolve => { respond = resolve }))
    const button = renderButton()

    fireEvent.click(button)
    fireEvent.click(button)
    fireEvent.click(button)

    expect(button).toBeDisabled()
    expect(button).toHaveTextContent('Preparando demonstração...')
    expect(demoCalls(fetcher)).toHaveLength(1)
    expect(demoCalls(fetcher)[0][1]?.method).toBe('POST')

    respond(Response.json({ token: 'demo-jwt', type: 'Bearer', expiresIn: 86400 }, { status: 201 }))
    await waitFor(() => expect(navigation.push).toHaveBeenCalledWith('/painel'))
    expect(demoCalls(fetcher)).toHaveLength(1)
  })

  it('com 201 abre /painel autenticado com o token da demo (DEMO-07 AC4)', async () => {
    stubDemo(async () => Response.json({ token: 'demo-jwt', type: 'Bearer', expiresIn: 86400 }, { status: 201 }))
    const button = renderButton()

    await userEvent.click(button)

    await waitFor(() => expect(navigation.push).toHaveBeenCalledWith('/painel'))
    expect(navigation.push).toHaveBeenCalledTimes(1)
    expect(sessionStorage.getItem('reconpay.demoToken')).toBe('demo-jwt')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it.each([
    [429, 'TOO_MANY_REQUESTS', 'Limite de demonstrações atingido. Tente novamente em até 1 hora.'],
    [503, 'SERVICE_UNAVAILABLE', 'Demonstrações esgotadas no momento. Tente novamente mais tarde.'],
  ])('com %i mostra a message da API em alerta e reabilita o botão (DEMO-07 AC5)', async (status, error, message) => {
    stubDemo(async () => Response.json({ timestamp: '2026-10-20T12:00:00Z', status, error, message, path: '/api/auth/demo' }, { status }))
    const button = renderButton()

    await userEvent.click(button)

    expect(await screen.findByRole('alert')).toHaveTextContent(message)
    expect(button).toBeEnabled()
    expect(button).toHaveTextContent('Ver demonstração')
    expect(navigation.push).not.toHaveBeenCalled()
  })

  it.each([
    ['falha de rede', () => Promise.reject(new TypeError('Failed to fetch'))],
    ['500 sem message', async () => new Response('', { status: 500 })],
  ])('com %s mostra a mensagem padrão e reabilita o botão (DEMO-07 AC6)', async (_case, demo) => {
    stubDemo(demo)
    const button = renderButton()

    await userEvent.click(button)

    expect(await screen.findByRole('alert')).toHaveTextContent(GENERIC)
    expect(button).toBeEnabled()
    expect(navigation.push).not.toHaveBeenCalled()
  })

  it('uma nova tentativa depois do erro chama a API de novo e limpa o alerta (DEMO-07 AC5)', async () => {
    const responses = [
      Response.json({ status: 429, error: 'TOO_MANY_REQUESTS', message: 'Limite de demonstrações atingido. Tente novamente em até 1 hora.' }, { status: 429 }),
      Response.json({ token: 'demo-jwt', type: 'Bearer', expiresIn: 86400 }, { status: 201 }),
    ]
    const fetcher = stubDemo(async () => responses.shift()!)
    const button = renderButton()

    await userEvent.click(button)
    await screen.findByRole('alert')
    await userEvent.click(button)

    await waitFor(() => expect(navigation.push).toHaveBeenCalledWith('/painel'))
    expect(demoCalls(fetcher)).toHaveLength(2)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

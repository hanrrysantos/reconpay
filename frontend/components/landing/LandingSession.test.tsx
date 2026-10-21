import { render, screen, within } from '@testing-library/react'
import { useEffect } from 'react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from '@/lib/auth'
import { LandingPage } from './LandingPage'

const navigation = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: navigation.push }) }))

const demoUser = { id: 'demo-1', name: 'Visitante', email: 'demo-abc@demo.reconpay.invalid', role: 'OPERATOR', active: true, demoExpiresAt: '2099-10-21T12:34:56Z' }
const ana = { id: 'u-1', name: 'Ana Souza', email: 'ana@lojacentro.com.br', role: 'OPERATOR', active: true, demoExpiresAt: null }

// Any other demo request answers 429, as in production once the IP quota is spent.
function stubApi() {
  const fetcher = vi.fn(async (input: string, init?: RequestInit) => {
    if (input.endsWith('/api/auth/login')) return Response.json({ token: 'ana-jwt', type: 'Bearer', expiresIn: 3600 })
    if (input.endsWith('/api/me')) return Response.json(new Headers(init?.headers).get('Authorization') === 'Bearer demo-jwt' ? demoUser : ana)
    return Response.json({ message: 'Limite de demonstrações por IP atingido.' }, { status: 429 })
  })
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}

/** A regular login lives only in memory, so it reaches the landing by signing in within the same provider. */
function SignedInLanding() {
  const { signIn } = useAuth()
  useEffect(() => { void signIn('ana@lojacentro.com.br', 'Senha@2026') }, [])
  return <LandingPage />
}

// The production case is the demo: opened in this tab, then the visitor came back to the landing without "Sair".
const sessions = [
  { kind: 'demonstração', open: () => { sessionStorage.setItem('reconpay.demoToken', 'demo-jwt'); render(<AuthProvider><LandingPage /></AuthProvider>) } },
  { kind: 'conta comum', open: () => { render(<AuthProvider><SignedInLanding /></AuthProvider>) } },
]

const demoCalls = (fetcher: ReturnType<typeof stubApi>) => fetcher.mock.calls.filter(([url]) => url.endsWith('/api/auth/demo'))
const hero = () => screen.getByRole('heading', { level: 1 }).closest('section')!
const finalCta = () => screen.getByRole('heading', { level: 2, name: 'Feche o próximo mês sem abrir uma planilha.' }).closest('section')!

async function renderWithOpenSession(open: () => void) {
  const fetcher = stubApi()
  open()
  await within(hero()).findByRole('button', { name: 'Voltar ao painel' })
  return fetcher
}

beforeEach(() => {
  navigation.push.mockReset()
  sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe.each(sessions)('LandingPage com sessão aberta de $kind', ({ open }) => {
  it('"Ver demonstração" vira "Voltar ao painel" na abertura e no convite final e leva a /painel sem criar conta (SESS-01 AC1)', async () => {
    const fetcher = await renderWithOpenSession(open)

    expect(within(hero()).queryByRole('button', { name: 'Ver demonstração' })).not.toBeInTheDocument()
    expect(within(finalCta()).queryByRole('button', { name: 'Ver demonstração' })).not.toBeInTheDocument()

    await userEvent.click(within(hero()).getByRole('button', { name: 'Voltar ao painel' }))
    await userEvent.click(within(finalCta()).getByRole('button', { name: 'Voltar ao painel' }))

    expect(navigation.push.mock.calls).toEqual([['/painel'], ['/painel']])
    expect(demoCalls(fetcher)).toHaveLength(0)
  })

  it('"Entrar" do menu e do rodapé continua "Entrar" para /login, onde o aviso da sessão aparece (SESS-01 AC2)', async () => {
    await renderWithOpenSession(open)

    const nav = screen.getByRole('navigation', { name: 'Principal' })
    expect(within(nav).getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
    expect(within(nav).queryByRole('link', { name: 'Voltar ao painel' })).not.toBeInTheDocument()
    const footer = screen.getByRole('contentinfo')
    expect(within(footer).getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
    expect(within(footer).queryByRole('link', { name: 'Voltar ao painel' })).not.toBeInTheDocument()
  })

  it.each([['abertura', hero], ['convite final', finalCta]])(
    '"Entrar na minha conta" da %s vira "Sair e entrar com outra conta": encerra a sessão e vai para /login (SESS-01 AC3)',
    async (_, region) => {
      await renderWithOpenSession(open)
      expect(within(region()).queryByRole('link', { name: 'Entrar na minha conta' })).not.toBeInTheDocument()

      await userEvent.click(within(region()).getByRole('button', { name: 'Sair e entrar com outra conta' }))

      expect(navigation.push).toHaveBeenCalledWith('/login')
      expect(sessionStorage.getItem('reconpay.demoToken')).toBeNull()
      expect(await within(hero()).findByRole('button', { name: 'Ver demonstração' })).toBeInTheDocument()
      expect(within(hero()).getByRole('link', { name: 'Entrar na minha conta' })).toHaveAttribute('href', '/login')
    })
})

describe('LandingPage sem sessão', () => {
  it('com o token da demo expirado a landing fica no estado sem sessão (edge)', async () => {
    sessionStorage.setItem('reconpay.demoToken', 'expired-jwt')
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ message: 'Token inválido' }, { status: 401 })))
    render(<AuthProvider><LandingPage /></AuthProvider>)

    await vi.waitFor(() => expect(sessionStorage.getItem('reconpay.demoToken')).toBeNull())
    expect(within(hero()).getByRole('button', { name: 'Ver demonstração' })).toBeInTheDocument()
    expect(within(hero()).getByRole('link', { name: 'Entrar na minha conta' })).toHaveAttribute('href', '/login')
    expect(within(screen.getByRole('navigation', { name: 'Principal' })).getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
  })
})

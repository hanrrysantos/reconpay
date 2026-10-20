import { render as renderDom, screen } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import type { ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '@/lib/queries'
import { AppShell } from './AppShell'

const replace = vi.fn()
const auth = vi.hoisted(() => ({ token: null as string | null, restoring: false, expired: false, demoExpiresAt: null as string | null }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }), usePathname: () => '/conciliacoes' }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: auth.token, user: auth.token ? { name: 'Ana Souza', demoExpiresAt: auth.demoExpiresAt } : null, restoring: auth.restoring, expired: auth.expired }) }))

const render = (ui: ReactElement) => renderDom(<QueryClientProvider client={createQueryClient(vi.fn())}>{ui}</QueryClientProvider>)

beforeEach(() => {
  replace.mockReset()
  auth.restoring = false
  auth.expired = false
  auth.demoExpiresAt = null
  vi.stubGlobal('fetch', vi.fn(async () => Response.json({ content: [{ id: 'm-1', name: 'Alfa Pagamentos', document: '12345678000190' }], totalPages: 1 })))
})
afterEach(() => vi.unstubAllGlobals())

describe('AppShell', () => {
  it('sem token redireciona para /login e não renderiza a rota', () => {
    auth.token = null
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    expect(replace).toHaveBeenCalledWith('/login')
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()
  })

  it('durante a restauração da sessão não redireciona e mostra "Abrindo sessão..." (DEMO-08 AC2)', () => {
    auth.token = null
    auth.restoring = true
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    expect(replace).not.toHaveBeenCalled()
    expect(screen.getByRole('status')).toHaveTextContent('Abrindo sessão...')
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()
  })

  it('demo expirada na restauração leva à landing, não ao login (DEMO-08 AC5)', () => {
    auth.token = null
    auth.expired = true
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    expect(replace).toHaveBeenCalledWith('/')
    expect(replace).not.toHaveBeenCalledWith('/login')
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument()
  })

  it('com token renderiza a navegação e a rota sem redirecionar', () => {
    auth.token = 'jwt'
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    expect(replace).not.toHaveBeenCalled()
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeInTheDocument()
    expect(screen.getByText('conteúdo protegido')).toBeInTheDocument()
  })

  it('com token mostra o seletor com a primeira empresa do acesso', async () => {
    auth.token = 'jwt'
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    expect(await screen.findByRole('button', { name: /Alfa Pagamentos/ })).toHaveAttribute('aria-haspopup', 'listbox')
  })

  it('na sessão de demo mostra a faixa no topo, antes da navegação (DEMO-09 AC3)', () => {
    auth.token = 'demo-jwt'
    auth.demoExpiresAt = '2099-10-21T12:34:56Z'
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    const banner = screen.getByText(/^Conta de demonstração · expira em \d{2}\/\d{2} às \d{2}:\d{2}$/)
    expect(banner.compareDocumentPosition(screen.getByRole('navigation', { name: 'Principal' })) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('na conta comum não mostra a faixa de demo (DEMO-09 AC4)', () => {
    auth.token = 'jwt'
    render(<AppShell><p>conteúdo protegido</p></AppShell>)

    expect(screen.getByText('conteúdo protegido')).toBeInTheDocument()
    expect(screen.queryByText(/Conta de demonstração/)).not.toBeInTheDocument()
  })
})

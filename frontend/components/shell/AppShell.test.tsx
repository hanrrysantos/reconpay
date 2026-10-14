import { render as renderDom, screen } from '@testing-library/react'
import { QueryClientProvider } from '@tanstack/react-query'
import type { ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createQueryClient } from '@/lib/queries'
import { AppShell } from './AppShell'

const replace = vi.fn()
const auth = vi.hoisted(() => ({ token: null as string | null }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }), usePathname: () => '/conciliacoes' }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: auth.token, user: auth.token ? { name: 'Ana Souza' } : null }) }))

const render = (ui: ReactElement) => renderDom(<QueryClientProvider client={createQueryClient(vi.fn())}>{ui}</QueryClientProvider>)

beforeEach(() => {
  replace.mockReset()
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
})

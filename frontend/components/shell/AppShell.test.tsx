import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppShell } from './AppShell'

const replace = vi.fn()
const auth = vi.hoisted(() => ({ token: null as string | null }))
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }), usePathname: () => '/conciliacoes' }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ token: auth.token, user: auth.token ? { name: 'Ana Souza' } : null }) }))

beforeEach(() => { replace.mockReset() })

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
})

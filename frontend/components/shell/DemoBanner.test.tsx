import { render, screen } from '@testing-library/react'
import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { DemoBanner } from './DemoBanner'

// Pinned before the imports run, so the local time differs from UTC (São Paulo is UTC-3, no daylight saving).
const originalTz = vi.hoisted(() => {
  const tz = process.env.TZ
  process.env.TZ = 'America/Sao_Paulo'
  return tz
})
afterAll(() => {
  if (originalTz === undefined) delete process.env.TZ
  else process.env.TZ = originalTz
})

const auth = vi.hoisted(() => ({ user: null as { name: string; demoExpiresAt: string | null } | null }))
vi.mock('@/lib/auth', () => ({ useAuth: () => ({ user: auth.user }) }))

beforeEach(() => { auth.user = null })

describe('DemoBanner', () => {
  it.each([
    ['2099-10-21T12:34:56Z', '09:34'],
    ['2099-10-21T02:05:00Z', '23:05'],
  ])('na demo mostra "Conta de demonstração · expira às HH:mm" no horário local (%s → %s) (DEMO-09 AC3)', (iso, local) => {
    auth.user = { name: 'Visitante', demoExpiresAt: iso }
    render(<DemoBanner />)

    expect(screen.getByText(/^Conta de demonstração/).textContent).toBe(`Conta de demonstração · expira às ${local}`)
  })

  it('em conta comum não renderiza nada (DEMO-09 AC4)', () => {
    auth.user = { name: 'Ana', demoExpiresAt: null }
    const { container } = render(<DemoBanner />)

    expect(container).toBeEmptyDOMElement()
  })
})

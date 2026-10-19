import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import VerifyEmailPage from './page'

const navigation = vi.hoisted(() => ({ params: new URLSearchParams(), replace: vi.fn(), push: vi.fn() }))
vi.mock('next/navigation', () => ({
  useSearchParams: () => navigation.params,
  useRouter: () => ({ replace: navigation.replace, push: navigation.push }),
}))
const api = vi.hoisted(() => ({ verifyEmail: vi.fn(), resendVerification: vi.fn() }))
vi.mock('@/lib/api', async importOriginal => ({
  ...(await importOriginal<typeof import('@/lib/api')>()),
  verifyEmail: api.verifyEmail,
  resendVerification: api.resendVerification,
}))

beforeEach(() => {
  navigation.replace.mockReset()
  navigation.push.mockReset()
  api.verifyEmail.mockReset()
  api.verifyEmail.mockResolvedValue(undefined)
})

describe('/verificar-email', () => {
  it('abre sem sessão e confirma o token da URL (VER-04, VER-01 AC1)', async () => {
    navigation.params = new URLSearchParams('token=q7Xk2mV9pL4r')
    render(<VerifyEmailPage />)

    expect(await screen.findByRole('heading', { name: 'E-mail confirmado' })).toBeInTheDocument()
    expect(api.verifyEmail).toHaveBeenCalledWith('q7Xk2mV9pL4r')
    expect(navigation.replace).not.toHaveBeenCalled()
    expect(navigation.push).not.toHaveBeenCalled()
  })

  it('mostra o link inválido quando a URL não tem token (VER-02 AC3)', () => {
    navigation.params = new URLSearchParams()
    render(<VerifyEmailPage />)

    expect(screen.getByRole('heading', { name: 'Este link não vale mais' })).toBeInTheDocument()
    expect(api.verifyEmail).not.toHaveBeenCalled()
  })
})

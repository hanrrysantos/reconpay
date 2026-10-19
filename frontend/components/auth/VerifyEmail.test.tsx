import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api'
import { VerifyEmail } from './VerifyEmail'

const api = vi.hoisted(() => ({ verifyEmail: vi.fn(), resendVerification: vi.fn() }))
vi.mock('@/lib/api', async importOriginal => ({
  ...(await importOriginal<typeof import('@/lib/api')>()),
  verifyEmail: api.verifyEmail,
  resendVerification: api.resendVerification,
}))

beforeEach(() => {
  api.verifyEmail.mockReset()
  api.verifyEmail.mockResolvedValue(undefined)
  api.resendVerification.mockReset()
  api.resendVerification.mockResolvedValue(undefined)
})

const rejectedToken = () => new ApiError('Token de verificação inválido ou expirado', 400, 'VALIDATION_ERROR')
const resendButton = () => screen.getByRole('button', { name: /^(Enviar novo link|Enviando\.\.\.)$/ })

async function renderInvalid() {
  api.verifyEmail.mockRejectedValue(rejectedToken())
  render(<VerifyEmail token="velho" />)
  await screen.findByRole('heading', { name: 'Este link não vale mais' })
  return userEvent.setup()
}

describe('VerifyEmail', () => {
  it('mostra "Confirmando..." e chama a confirmação com o token (VER-01 AC1)', async () => {
    let finish: () => void = () => {}
    api.verifyEmail.mockReturnValue(new Promise<void>(resolve => { finish = resolve }))
    render(<VerifyEmail token="q7Xk2mV9" />)

    expect(screen.getByRole('status')).toHaveTextContent('Confirmando...')
    expect(api.verifyEmail).toHaveBeenCalledTimes(1)
    expect(api.verifyEmail).toHaveBeenCalledWith('q7Xk2mV9')
    finish()
    await screen.findByRole('heading', { name: 'E-mail confirmado' })
  })

  it('chama a API uma vez só em modo estrito (edge case)', async () => {
    render(<StrictMode><VerifyEmail token="q7Xk2mV9" /></StrictMode>)

    await screen.findByRole('heading', { name: 'E-mail confirmado' })
    expect(api.verifyEmail).toHaveBeenCalledTimes(1)
  })

  it('envia o token com espaços nas pontas como veio (edge case)', async () => {
    render(<VerifyEmail token=" q7Xk2mV9 " />)

    await screen.findByRole('heading', { name: 'E-mail confirmado' })
    expect(api.verifyEmail).toHaveBeenCalledWith(' q7Xk2mV9 ')
  })

  it('confirma a conta e leva ao login (VER-01 AC2)', async () => {
    render(<VerifyEmail token="q7Xk2mV9" />)

    expect(await screen.findByRole('heading', { level: 1, name: 'E-mail confirmado' })).toBeInTheDocument()
    expect(screen.getByText('Sua conta está ativa. Entre com o e-mail e a senha que você cadastrou.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Ir para o login' })).toHaveAttribute('href', '/login')
    expect(screen.queryByRole('button', { name: 'Enviar novo link' })).not.toBeInTheDocument()
  })

  it('mostra a marca para fundo claro, toda em preto e sem a frase (BRD-02 AC2)', async () => {
    const { container } = render(<VerifyEmail token="q7Xk2mV9" />)
    await screen.findByRole('heading', { name: 'E-mail confirmado' })

    expect(container.querySelector('img')).toHaveAttribute('src', '/brand/reconpay-mark-on-light.svg')
    expect(screen.getByText('Pay').parentElement).toHaveTextContent(/^ReconPay$/)
    expect(screen.getByText('Pay').parentElement).toHaveClass('text-foreground')
    expect(screen.getByText('Pay')).not.toHaveClass('text-primary')
    expect(screen.queryByText('Conciliação financeira com clareza')).not.toBeInTheDocument()
  })

  it('mostra o link inválido com o reenvio quando a API recusa o token (VER-02 AC3)', async () => {
    await renderInvalid()

    expect(screen.getByRole('heading', { level: 1, name: 'Este link não vale mais' })).toBeInTheDocument()
    expect(screen.getByText('Ele expirou ou já foi usado. Informe seu e-mail e enviamos um link novo.')).toBeInTheDocument()
    expect(screen.getByLabelText('E-mail')).toBeRequired()
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('type', 'email')
    expect(resendButton()).toHaveTextContent('Enviar novo link')
    expect(screen.getByText('Já confirmou?')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
    expect(screen.queryByRole('link', { name: 'Ir para o login' })).not.toBeInTheDocument()
  })

  it('mostra o link inválido sem chamar a API quando não há token (VER-02 AC3)', () => {
    render(<VerifyEmail token={null} />)

    expect(screen.getByRole('heading', { level: 1, name: 'Este link não vale mais' })).toBeInTheDocument()
    expect(resendButton()).toBeInTheDocument()
    expect(api.verifyEmail).not.toHaveBeenCalled()
  })

  it('mostra o erro de rede com "Tentar de novo", que repete a confirmação (VER-02 AC4)', async () => {
    api.verifyEmail.mockRejectedValueOnce(new ApiError('Não foi possível conectar à API. Tente novamente.'))
    render(<VerifyEmail token="q7Xk2mV9" />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar à API. Tente novamente.')
    expect(screen.queryByRole('heading', { name: 'Este link não vale mais' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'E-mail confirmado' })).not.toBeInTheDocument()

    await userEvent.setup().click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('heading', { name: 'E-mail confirmado' })).toBeInTheDocument()
    expect(api.verifyEmail).toHaveBeenCalledTimes(2)
    expect(api.verifyEmail).toHaveBeenLastCalledWith('q7Xk2mV9')
  })

  it('mostra a mensagem de um erro 500 com "Tentar de novo" (VER-02 AC4)', async () => {
    api.verifyEmail.mockRejectedValue(new ApiError('Erro interno', 500))
    render(<VerifyEmail token="q7Xk2mV9" />)

    expect(await screen.findByRole('alert')).toHaveTextContent('Erro interno')
    expect(screen.getByRole('button', { name: 'Tentar de novo' })).toBeInTheDocument()
  })

  it('pede o link novo e mostra a confirmação com o e-mail sem espaços (VER-03 AC1, edge case)', async () => {
    const user = await renderInvalid()

    await user.type(screen.getByLabelText('E-mail'), '  ana@lojacentro.com.br  ')
    await user.click(resendButton())

    expect(api.resendVerification).toHaveBeenCalledWith('ana@lojacentro.com.br')
    const heading = await screen.findByRole('heading', { level: 1, name: 'Confira seu e-mail' })
    const card = heading.closest('section')!
    expect(card).toHaveTextContent('Se ana@lojacentro.com.br tiver um cadastro aguardando confirmação, o link novo chega em alguns minutos. Não chegou? Confira a caixa de spam.')
    expect(within(card).queryByLabelText('E-mail')).not.toBeInTheDocument()
    expect(within(card).getByRole('link', { name: 'Entrar' })).toHaveAttribute('href', '/login')
  })

  it('desabilita "Enviar novo link" com "Enviando..." e não envia de novo (VER-03 AC2)', async () => {
    const user = await renderInvalid()
    let finish: () => void = () => {}
    api.resendVerification.mockReturnValue(new Promise<void>(resolve => { finish = resolve }))
    await user.type(screen.getByLabelText('E-mail'), 'ana@lojacentro.com.br')

    await user.click(resendButton())
    expect(resendButton()).toBeDisabled()
    expect(resendButton()).toHaveTextContent('Enviando...')
    await user.click(resendButton())
    resendButton().closest('form')!.requestSubmit()

    expect(api.resendVerification).toHaveBeenCalledTimes(1)
    finish()
    await screen.findByRole('heading', { name: 'Confira seu e-mail' })
  })

  it('mostra a falha do reenvio e mantém o e-mail; o aviso sai no novo envio (VER-03 AC3)', async () => {
    const user = await renderInvalid()
    api.resendVerification.mockRejectedValueOnce(new ApiError('Email inválido', 400, 'VALIDATION_ERROR'))
    await user.type(screen.getByLabelText('E-mail'), 'ana@lojacentro.com.br')

    await user.click(resendButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Email inválido')
    expect(screen.getByLabelText('E-mail')).toHaveValue('ana@lojacentro.com.br')
    expect(resendButton()).toBeEnabled()

    let finish: () => void = () => {}
    api.resendVerification.mockReturnValueOnce(new Promise<void>(resolve => { finish = resolve }))
    await user.clear(screen.getByLabelText('E-mail'))
    await user.type(screen.getByLabelText('E-mail'), 'ana.lima@lojacentro.com.br')
    await user.click(resendButton())

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(api.resendVerification).toHaveBeenLastCalledWith('ana.lima@lojacentro.com.br')
    finish()
    expect((await screen.findByRole('heading', { name: 'Confira seu e-mail' })).closest('section')).toHaveTextContent('Se ana.lima@lojacentro.com.br tiver um cadastro aguardando confirmação')
  })

  it('usa a mensagem padrão quando o reenvio falha com algo que não é Error (VER-03 AC3)', async () => {
    const user = await renderInvalid()
    api.resendVerification.mockRejectedValueOnce('falhou')
    await user.type(screen.getByLabelText('E-mail'), 'ana@lojacentro.com.br')

    await user.click(resendButton())

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível enviar o link. Tente novamente.')
  })
})
